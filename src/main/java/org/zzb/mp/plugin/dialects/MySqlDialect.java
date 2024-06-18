package org.zzb.mp.plugin.dialects;

public class MySqlDialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SHOW TABLES";
    }
}
