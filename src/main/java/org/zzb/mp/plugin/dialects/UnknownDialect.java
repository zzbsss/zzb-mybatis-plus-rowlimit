package org.zzb.mp.plugin.dialects;

public class UnknownDialect implements IDialect{

    @Override
    public String buildQueryAllTablesSql(String schema) {
        return null;
    }
}
