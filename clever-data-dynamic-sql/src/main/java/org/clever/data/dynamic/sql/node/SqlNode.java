package org.clever.data.dynamic.sql.node;

/**
 * 动态SQL节点
 */
public interface SqlNode {
    boolean apply(DynamicContext context);
}
