package org.zzb.mp.plugin.dialects;

public class MariaDBDialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SHOW TABLES";
    }
}
