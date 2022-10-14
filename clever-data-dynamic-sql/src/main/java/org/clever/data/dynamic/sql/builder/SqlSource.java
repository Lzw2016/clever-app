package org.clever.data.dynamic.sql.builder;

import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.dialect.DbType;

public interface SqlSource {
    BoundSql getBoundSql(DbType dbType, Object parameterObject);
}
