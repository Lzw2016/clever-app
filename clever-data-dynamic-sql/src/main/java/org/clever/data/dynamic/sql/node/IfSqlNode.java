package org.clever.data.dynamic.sql.node;

import org.clever.data.dynamic.sql.ognl.ExpressionEvaluator;

public class IfSqlNode implements SqlNode {
    private final ExpressionEvaluator evaluator;
    private final String test;
    private final SqlNode contents;

    public IfSqlNode(SqlNode contents, String test) {
        this.test = test;
        this.contents = contents;
        this.evaluator = new ExpressionEvaluator();
    }

    @Override
    public boolean apply(DynamicContext context) {
        context.addParameterExpression(test);
        if (evaluator.evaluateBoolean(test, context.getBindings())) {
            contents.apply(context);
            return true;
        }
        return false;
    }
}
