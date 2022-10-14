package org.clever.data.dynamic.sql.builder;


import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.dynamic.sql.node.DynamicContext;
import org.clever.data.dynamic.sql.node.SqlNode;

public class DynamicSqlSource implements SqlSource {
    private final SqlNode rootSqlNode;

    public DynamicSqlSource(SqlNode rootSqlNode) {
        this.rootSqlNode = rootSqlNode;
    }

    @Override
    public BoundSql getBoundSql(DbType dbType, Object parameterObject) {
        DynamicContext context = new DynamicContext(parameterObject);
        rootSqlNode.apply(context);
        StaticSqlSource sqlSource = new StaticSqlSource(context.getSql(), context);
        BoundSql boundSql = sqlSource.getBoundSql(dbType, parameterObject);
        context.getBindings().forEach(boundSql::setAdditionalParameter);
        context.getParameterExpressionSet().forEach(srt -> boundSql.getParameterExpressionSet().add(srt));
        return boundSql;
    }
}
