package org.clever.data.redis.hash;

import java.util.Map;

/**
 * Java类型和Redis哈希映射之间的核心映射契约。支持嵌套对象取决于实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:57 <br/>
 */
public interface HashMapper<T, K, V> {
    /**
     * 将 {@code object} 转换为可用于Redis哈希的映射
     */
    Map<K, V> toHash(T object);

    /**
     * 将 {@code hash} (map)转换为对象
     */
    T fromHash(Map<K, V> hash);
}
