package org.zzb.mp.plugin.dialects;

public class DB2Dialect implements IDialect {

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return "SELECT TABNAME FROM SYSIBM.SYSTABLES WHERE TYPE = 'T' AND TABSCHEMA = " + schema;
    }
}
