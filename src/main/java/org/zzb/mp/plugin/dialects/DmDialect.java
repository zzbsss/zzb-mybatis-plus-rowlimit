package org.zzb.mp.plugin.dialects;

public class DmDialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT TABLE_NAME FROM USER_TABLES";
    }
}
