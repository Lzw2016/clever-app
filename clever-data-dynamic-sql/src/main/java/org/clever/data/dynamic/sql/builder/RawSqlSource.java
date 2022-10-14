package org.clever.data.dynamic.sql.builder;


import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.dynamic.sql.node.DynamicContext;
import org.clever.data.dynamic.sql.node.SqlNode;

import java.util.HashMap;

public class RawSqlSource implements SqlSource {
    private final StaticSqlSource sqlSource;

    public RawSqlSource(SqlNode rootSqlNode) {
        this(getSql(rootSqlNode));
    }

    public RawSqlSource(String sql) {
        this.sqlSource = new StaticSqlSource(sql, new DynamicContext(new HashMap<>()));
    }

    private static String getSql(SqlNode rootSqlNode) {
        DynamicContext context = new DynamicContext(null);
        rootSqlNode.apply(context);
        return context.getSql();
    }

    @Override
    public BoundSql getBoundSql(DbType dbType, Object parameterObject) {
        return sqlSource.getBoundSql(dbType, parameterObject);
    }
}
