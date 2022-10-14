package org.clever.data.dynamic.sql.node;

import org.clever.data.dynamic.sql.ognl.OgnlCache;

public class VarDeclSqlNode implements SqlNode {
    private final String name;
    private final String expression;

    public VarDeclSqlNode(String var, String exp) {
        name = var;
        expression = exp;
    }

    @Override
    public boolean apply(DynamicContext context) {
        context.addParameterExpression(expression);
        context.addParameterVar(name);
        final Object value = OgnlCache.getValue(expression, context.getBindings());
        context.bind(name, value);
        return true;
    }
}
