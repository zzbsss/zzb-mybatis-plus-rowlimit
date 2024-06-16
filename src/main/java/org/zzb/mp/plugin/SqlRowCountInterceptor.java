package org.zzb.mp.plugin;

import com.baomidou.mybatisplus.core.MybatisDefaultParameterHandler;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.SqlInfo;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.handlers.AbstractSqlParserHandler;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
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
import org.zzb.mp.config.MaxRowCountConfig;
import org.zzb.mp.support.TableSizeCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlRowCountInterceptor extends AbstractSqlParserHandler implements Interceptor {

    private TableSizeCache tableSizeCache;

    private MaxRowCountConfig maxRowCountConfig;

    /**
     * COUNT SQL 解析
     */
    protected ISqlParser countSqlParser;

    private static final Pattern TABLE_NAME_PATTERN =  Pattern.compile("(?i)\\bfrom\\s+([\\w\\.]*)|(?i)\\binner\\s+join\\s+([\\w\\.]*)|(?i)\\bselect\\s+([\\w\\.]*)\\b");;

    @Override
    public Object intercept(Invocation invocation) throws Exception {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        // this.sqlParser(metaObject);
        MappedStatement mappedStatement = (MappedStatement)metaObject.getValue("delegate.mappedStatement");
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        // 先判断是不是SELECT操作
        if (SqlCommandType.SELECT != mappedStatement.getSqlCommandType()
                || StatementType.CALLABLE == mappedStatement.getStatementType()) {
            return invocation.proceed();
        }
        Connection connection = (Connection)invocation.getArgs()[0];
        // 分析涉及的表
        Matcher matcher = TABLE_NAME_PATTERN.matcher(boundSql.getSql());
        while (matcher.find()) {
            String tableName = matcher.group(1);
            // 判断表是否在监控范围内
            if (Objects.isNull(tableSizeCache.get(connection.getCatalog() + "." + tableName))) {
                continue;
            }
            long sqlCount = analyzeSql(mappedStatement, boundSql, connection);
            if (sqlCount > maxRowCountConfig.getMaxRowCount()) {
                throw ExceptionUtils.mpe("Table " + tableName + " exceeds the maximum row count limit.");
            }
        }
        // 继续执行SQL
        return invocation.proceed();
    }

    /**
     * 优化sql count 语句
     * @param mappedStatement
     * @param boundSql
     * @param connection
     * @return
     */
    private long analyzeSql(MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {
        SqlInfo sqlInfo = SqlParserUtils.getOptimizeCountSql(true, countSqlParser, boundSql.getSql());
        return this.queryTotal(sqlInfo.getSql(), mappedStatement, boundSql, connection);
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
