package org.clever.data.dynamic.sql.node;

import java.util.Arrays;
import java.util.List;

public class WhereSqlNode extends TrimSqlNode {
    private static final List<String> Prefix_List = Arrays.asList("AND ", "OR ", "AND\n", "OR\n", "AND\r", "OR\r", "AND\t", "OR\t");

    public WhereSqlNode(SqlNode contents) {
        super(contents, "WHERE", Prefix_List, null, null);
    }
}
