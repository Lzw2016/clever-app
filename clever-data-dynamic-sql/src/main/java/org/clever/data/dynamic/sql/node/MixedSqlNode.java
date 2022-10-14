package org.clever.data.dynamic.sql.node;

import java.util.List;

public class MixedSqlNode implements SqlNode {
    private final List<SqlNode> contents;

    public MixedSqlNode(List<SqlNode> contents) {
        this.contents = contents;
    }

    @Override
    public boolean apply(DynamicContext context) {
        contents.forEach(node -> node.apply(context));
        return true;
    }
}
