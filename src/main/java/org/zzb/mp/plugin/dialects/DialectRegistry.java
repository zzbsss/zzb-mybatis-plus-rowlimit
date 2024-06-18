package org.zzb.mp.plugin.dialects;

import com.baomidou.mybatisplus.annotation.DbType;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class DialectRegistry {

    private final Map<DbType, IDialect> dialect_enum_map = new EnumMap<>(DbType.class);

    public DialectRegistry() {
        dialect_enum_map.put(DbType.MYSQL, new MySqlDialect());
        dialect_enum_map.put(DbType.MARIADB, new MariaDBDialect());
        dialect_enum_map.put(DbType.ORACLE, new OracleDialect());
        dialect_enum_map.put(DbType.ORACLE_12C, new Oracle12cDialect());
        dialect_enum_map.put(DbType.DB2, new DB2Dialect());
        dialect_enum_map.put(DbType.H2, new H2Dialect());
        dialect_enum_map.put(DbType.HSQL, new HSQLDialect());
        dialect_enum_map.put(DbType.SQLITE, new SQLiteDialect());
        dialect_enum_map.put(DbType.POSTGRE_SQL, new PostgreDialect());
        dialect_enum_map.put(DbType.SQL_SERVER2005, new SQLServer2005Dialect());
        dialect_enum_map.put(DbType.SQL_SERVER, new SQLServerDialect());
        dialect_enum_map.put(DbType.DM, new DmDialect());
        dialect_enum_map.put(DbType.XU_GU, new XuGuDialect());
        dialect_enum_map.put(DbType.KINGBASE_ES, new KingbaseDialect());
        dialect_enum_map.put(DbType.PHOENIX, new PhoenixDialect());
        dialect_enum_map.put(DbType.OTHER, new UnknownDialect());
    }

    public IDialect getDialect(DbType dbType) {
        return dialect_enum_map.get(dbType);
    }

    public Collection<IDialect> getDialects() {
        return Collections.unmodifiableCollection(dialect_enum_map.values());
    }
}
