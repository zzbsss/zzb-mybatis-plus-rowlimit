package org.zzb.mp.config;


import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zzb.mp.optimize.JsqlParserCountOptimize;
import org.zzb.mp.plugin.SqlRowCountInterceptor;
import org.zzb.mp.support.TableSizeCache;
import org.zzb.mp.support.TableSizeScanner;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "zzb.mp",name = "enabled",havingValue = "true")
public class SqlRowCountAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "zzb.mp")
    public MaxRowCountConfig maxRowCountConfig() {
       return new MaxRowCountConfig();
    }

    @Bean
    public TableSizeCache tableSizeCache() {
        return new TableSizeCache();
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public TableSizeScanner tableSizeScanner(Map<String, DataSource> dataSourceMap, SqlRowCountAutoConfiguration sqlRowCountAutoConfiguration) {
        // 兼容 多数据源
        if (dataSourceMap.get("dataSource") instanceof DynamicRoutingDataSource) {
            dataSourceMap = ((DynamicRoutingDataSource) dataSourceMap.get("dataSource")).getCurrentDataSources();
        }
        return new TableSizeScanner(tableSizeCache(), dataSourceMap, sqlRowCountAutoConfiguration);
    }

    @Bean
    public SqlRowCountInterceptor sqlRowCountInterceptor(MaxRowCountConfig maxRowCountConfig) {
        return new SqlRowCountInterceptor(tableSizeCache(),maxRowCountConfig, new JsqlParserCountOptimize());
    }
}
