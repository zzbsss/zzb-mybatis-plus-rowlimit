package org.zzb.mp.plugin;

import com.baomidou.mybatisplus.core.MybatisDefaultParameterHandler;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.SqlInfo;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.ParameterUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.handlers.AbstractSqlParserHandler;
import java.util.List;
import java.util.Optional;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zzb.mp.config.MaxRowCountConfig;
import org.zzb.mp.support.TableSizeCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.zzb.mp.util.SqlParserUtils;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlRowCountInterceptor extends AbstractSqlParserHandler implements Interceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private TableSizeCache tableSizeCache;

    private MaxRowCountConfig maxRowCountConfig;

    /**
     * COUNT SQL 解析
     */
    protected ISqlParser countSqlParser;

    @Override
    public Object intercept(Invocation invocation) throws Exception {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        // this.sqlParser(metaObject);
        MappedStatement mappedStatement = (MappedStatement)metaObject.getValue("delegate.mappedStatement");
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        Connection connection = (Connection)invocation.getArgs()[0];
        // 先判断是不是SELECT操作
        if (SqlCommandType.SELECT != mappedStatement.getSqlCommandType()
                || StatementType.CALLABLE == mappedStatement.getStatementType()) {
            return invocation.proceed();
        }
        Optional<IPage> pageOptional = ParameterUtils.findPage(boundSql.getParameterObject());
        // 是分页不做处理
        if (pageOptional.isPresent()) {
            return invocation.proceed();
        }
        // 是流式读取不做处理
        if(mappedStatement.getFetchSize() > 0) {
            return invocation.proceed();
        }
        // 分析涉及的表
        Statement statement;
        try {
            statement =  CCJSqlParserUtil.parse(boundSql.getSql());
        } catch (JSQLParserException e) {
            throw ExceptionUtils.mpe("分析sql失败", e);
        }
        // 解析sql中的表
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(statement);
        for (String tableName : tableList) {
            // 判断表是否在监控范围内
            if (Objects.isNull(tableSizeCache.get(connection.getCatalog() + "." + tableName))) {
                continue;
            }
            // 执行优化后的count sql
            SqlInfo countSql = SqlParserUtils.getOptimizeCountSql(true, countSqlParser, boundSql.getSql());
            long sqlCount = this.queryTotal(countSql.getSql(), mappedStatement, boundSql, connection);
            if (sqlCount > maxRowCountConfig.getMaxRowCount()) {
                log.error("error table: {}, max row: {}, original sql: {}", tableName, maxRowCountConfig.getMaxRowCount(), countSql.getSql());
                throw ExceptionUtils.mpe(maxRowCountConfig.getErrorMessage());
            }
        }
        // 继续执行SQL
        return invocation.proceed();
    }


    /**
     * 查询总记录条数
     *
     */
    protected long queryTotal(String sql, MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            long total = 0;
            MybatisDefaultParameterHandler mybatisDefaultParameterHandler
                    = new MybatisDefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
            mybatisDefaultParameterHandler.setParameters(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    total = resultSet.getLong(1);
                }
            }
            return total;
        } catch (Exception e) {
            throw ExceptionUtils.mpe("Error: Method queryTotal execution error of sql : \n %s \n", e, sql);
        }
    }

    public SqlRowCountInterceptor(TableSizeCache tableSizeCache, MaxRowCountConfig maxRowCountConfig, ISqlParser countSqlParser) {
        this.tableSizeCache = tableSizeCache;
        this.maxRowCountConfig = maxRowCountConfig;
        this.countSqlParser = countSqlParser;
    }
}
