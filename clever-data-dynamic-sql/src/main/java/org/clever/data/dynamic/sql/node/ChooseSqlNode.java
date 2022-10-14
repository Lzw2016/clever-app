package org.clever.data.dynamic.sql.node;

import java.util.List;

public class ChooseSqlNode implements SqlNode {
    private final SqlNode defaultSqlNode;
    private final List<SqlNode> ifSqlNodes;

    public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
        this.ifSqlNodes = ifSqlNodes;
        this.defaultSqlNode = defaultSqlNode;
    }

    @Override
    public boolean apply(DynamicContext context) {
        for (SqlNode sqlNode : ifSqlNodes) {
            if (sqlNode.apply(context)) {
                return true;
            }
        }
        if (defaultSqlNode != null) {
            defaultSqlNode.apply(context);
            return true;
        }
        return false;
    }
}
