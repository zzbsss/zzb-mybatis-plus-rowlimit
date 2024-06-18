package org.zzb.mp.plugin.dialects;

public class KingbaseDialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT tablename FROM pg_tables WHERE schemaname = " + schema;
    }
}
