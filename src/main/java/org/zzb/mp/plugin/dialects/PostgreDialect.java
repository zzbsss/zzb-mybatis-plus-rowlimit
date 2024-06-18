package org.zzb.mp.plugin.dialects;

public class PostgreDialect implements IDialect {

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT tablename FROM pg_tables ";
    }
}
