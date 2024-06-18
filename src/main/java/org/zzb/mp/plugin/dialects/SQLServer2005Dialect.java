package org.zzb.mp.plugin.dialects;

public class SQLServer2005Dialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT name FROM sys.tables";
    }
}
