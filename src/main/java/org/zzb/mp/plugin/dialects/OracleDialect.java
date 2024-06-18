package org.zzb.mp.plugin.dialects;

public class OracleDialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT TABLE_NAME FROM USER_TABLES";
    }
}
