package org.clever.data.redis.connection;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:30 <br/>
 */
public interface NamedNode {
    /**
     * @return 节点名称。可以是 {@literal null}
     */
    String getName();
}
