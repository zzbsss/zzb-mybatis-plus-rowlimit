package org.zzb.mp.support;

import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;
import org.zzb.mp.config.MaxRowCountConfig;
import org.zzb.mp.config.SqlRowCountAutoConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class TableSizeScanner implements ApplicationRunner {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private TableSizeCache tableSizeCache;

    private SqlRowCountAutoConfiguration sqlRowCountAutoConfiguration;

    // 兼容多数据源
    private Map<String,DataSource> dataSourceMap;

    private ConcurrentHashMap<String,JdbcTemplate> jdbcTemplateConcurrentHashMap  = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        this.scanAndCacheTableSizes();
    }

    public TableSizeScanner(TableSizeCache tableSizeCache, Map<String, DataSource> dataSourceMap, SqlRowCountAutoConfiguration sqlRowCountAutoConfiguration) {
        this.tableSizeCache = tableSizeCache;
        this.dataSourceMap = dataSourceMap;
        this.sqlRowCountAutoConfiguration = sqlRowCountAutoConfiguration;
    }

    // 每小时执行一次
    @Scheduled(cron = "${zzb.mp.exec-corn}")
    public synchronized void  scanAndCacheTableSizes() {
        TableSizeCache currentTableSizeCache = new TableSizeCache();
        // 是否开启
        if (!sqlRowCountAutoConfiguration.maxRowCountConfig().isEnabled()) {
            return;
        }
        StopWatch cacheTbleSizeStopWatch = new StopWatch("cache table");
        cacheTbleSizeStopWatch.start("cache table size");
        MaxRowCountConfig maxRowCountConfig = sqlRowCountAutoConfiguration.maxRowCountConfig();
        // 是否多数据源 并且开启只检查指定数据源
        Map<String,DataSource> checkDataSourceMap = dataSourceMap;
        if (dataSourceMap.size() > 1 && maxRowCountConfig.getCheckDataSources().size() > 0) {
            checkDataSourceMap = new HashMap<>();
            List<String> checkDataSources = maxRowCountConfig.getCheckDataSources();
            for (String checkDataSource : checkDataSources) {
                if (!dataSourceMap.containsKey(checkDataSource)) {
                    log.warn("can not find datasource {}, please check your config", checkDataSource);
                    continue;
                }
                checkDataSourceMap.put(checkDataSource, dataSourceMap.get(checkDataSource));
            }
        }
        try {
            // 配置了表名
            if (sqlRowCountAutoConfiguration.maxRowCountConfig().getCheckTables().size() > 0) {
                for (String checkTable : sqlRowCountAutoConfiguration.maxRowCountConfig().getCheckTables()) {
                    // 考虑多数据源情况 (多数据源 数据源名.表名)
                    if (checkDataSourceMap.size() > 1) {
                        String[] tableFlag = checkTable.split("\\.");
                        if (tableFlag.length != 2) {
                            throw ExceptionUtils.mpe("please check tables config, the format is {datasource.table}");
                        }
                        long rowCount = getRowCount(getJdbcTemplate(tableFlag[0], dataSourceMap.get(tableFlag[0])), tableFlag[1]);
                        log.debug("table {} cache size {}", checkTable, rowCount);
                        // 仅超过指定大小的才检查
                        if (rowCount > sqlRowCountAutoConfiguration.maxRowCountConfig().getCheckTableSize()) {
                            currentTableSizeCache.put(checkTable, rowCount);
                        }
                        continue;
                    }
                    DataSource dataSource = (DataSource)checkDataSourceMap.values().toArray()[0];
                    long rowCount = getRowCount(getJdbcTemplate(checkTable, dataSource), checkTable);
                    Connection connection = dataSource.getConnection();
                    String catalog = connection.getCatalog();
                    // 仅当表大小大于 指定数量时才进行sql监控
                    if (rowCount >= sqlRowCountAutoConfiguration.maxRowCountConfig().getCheckTableSize()) {
                        log.debug("table {} cache size {}", catalog + "." + checkTable, rowCount);
                        currentTableSizeCache.put(catalog + "." + checkTable, rowCount);
                    }
                    // 归还连接
                    JdbcUtils.closeConnection(connection);
                }
            }
        } catch (Exception e) {
            throw ExceptionUtils.mpe("get check table filed", e);
        }
        if (sqlRowCountAutoConfiguration.maxRowCountConfig().getCheckTables().size() == 0) {
            // 遍历数据源
            checkDataSourceMap.forEach((k,dataSource) -> {
                JdbcTemplate jdbcTemplate = getJdbcTemplate(k, dataSource);
                List<String> tableNames = jdbcTemplate.queryForList("SHOW TABLES", String.class);
                // 将连接数据库中的所有表缓存起来
                for (String tableName : tableNames) {
                    Connection connection = null;
                    String catalogName;
                    try {
                         connection  = dataSource.getConnection();
                         catalogName = connection.getCatalog();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }finally {
                        JdbcUtils.closeConnection(connection);
                    }
                    long rowCount = getRowCount(jdbcTemplate, tableName);
                    if (rowCount >= sqlRowCountAutoConfiguration.maxRowCountConfig().getCheckTableSize()) {
                        log.debug("table {} cache size {}", catalogName + "." + tableName, rowCount);
                        currentTableSizeCache.put(catalogName + "." + tableName, rowCount);
                    }
                }
            });
        }
        // 更新本次快照
        tableSizeCache = currentTableSizeCache;
        cacheTbleSizeStopWatch.stop();
        log.debug("cache table success \n {}", cacheTbleSizeStopWatch.prettyPrint() );
    }

    private JdbcTemplate getJdbcTemplate(String k, DataSource dataSource) {
        JdbcTemplate jdbcTemplate = jdbcTemplateConcurrentHashMap.get(k);
        if (Objects.isNull(jdbcTemplate)) {
            jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplateConcurrentHashMap.put(k, jdbcTemplate);
        }
        return jdbcTemplate;
    }

    private long getRowCount(JdbcTemplate jdbcTemplate, String tableName) {
        // 根据数据库类型，执行相应的SQL查询获取行数
        String sql = "SELECT COUNT(*) FROM " + tableName;
        return jdbcTemplate.queryForObject(sql, Long.class);
    }


}
