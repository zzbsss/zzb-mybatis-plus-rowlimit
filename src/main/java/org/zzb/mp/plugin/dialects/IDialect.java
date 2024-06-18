package org.zzb.mp.plugin.dialects;

public interface IDialect {

    /**
     * 构建查询全表sql
     *
     * @return 分页模型
     */
    String buildQueryAllTablesSql(String schema);
}
