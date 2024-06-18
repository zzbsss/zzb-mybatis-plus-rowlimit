package org.zzb.mp.plugin.dialects;

public class SQLiteDialect implements IDialect{


    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT name  FROM sqlite_master WHERE type='table'";
    }
}
