package org.zzb.mp.plugin.dialects;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.ClassUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.DialectModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.ibatis.session.RowBounds;

/**
 * 分页方言工厂类
 *
 * @author hubin
 * @since 2016-01-23
 */
public class DialectFactory {

    private static final DialectRegistry DIALECT_REGISTRY = new DialectRegistry();

    /**
     * 自定义方言缓存
     */
    private static final Map<String, IDialect> DIALECT_CACHE = new ConcurrentHashMap<>();


    /**
     * 获取实现方言
     *
     * @param dialectClazz 方言全类名
     * @return 方言实现对象
     * @since 3.3.1
     */
    public static IDialect getDialect(String dialectClazz) {
        return DIALECT_CACHE.computeIfAbsent(dialectClazz, DialectFactory::classToDialect);
    }

    public static IDialect getDialect(DbType dbType) {
        return DIALECT_REGISTRY.getDialect(dbType);
    }

    private static IDialect newInstance(Class<? extends IDialect> dialectClazz) {
        IDialect dialect = ClassUtils.newInstance(dialectClazz);
        Assert.notNull(dialect, "The value of the dialect property in mybatis configuration.xml is not defined.");
        return dialect;
    }

    @SuppressWarnings("unchecked")
    private static IDialect classToDialect(String dialectClazz){
        IDialect dialect = null;
        try {
            Class<?> clazz = Class.forName(dialectClazz);
            if (IDialect.class.isAssignableFrom(clazz)) {
                dialect = newInstance((Class<? extends IDialect>) clazz);
            }
        } catch (ClassNotFoundException e) {
            throw ExceptionUtils.mpe("Class : %s is not found", dialectClazz);
        }
        return dialect;
    }
}
