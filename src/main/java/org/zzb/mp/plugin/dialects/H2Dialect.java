package org.zzb.mp.plugin.dialects;

public class H2Dialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEM = " + schema;
    }
}
