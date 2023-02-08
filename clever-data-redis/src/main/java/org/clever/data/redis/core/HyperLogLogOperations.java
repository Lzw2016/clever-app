package org.clever.data.redis.core;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:44 <br/>
 */
public interface HyperLogLogOperations<K, V> {
    /**
     * 将给定的 {@literal values} 添加到 {@literal key}
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return 至少一个值中的 1 个已添加到键中;否则为 0。{@literal null} 在管道/事务中使用时
     */
    @SuppressWarnings("unchecked")
    Long add(K key, V... values);

    /**
     * 获取 {@literal key} 中的当前元素数
     *
     * @param keys 不得为 {@literal null} 或 {@literal empty}
     * @return {@literal null} 在管道/事务中使用时。
     */
    @SuppressWarnings("unchecked")
    Long size(K... keys);

    /**
     * 将给定 {@literal sourceKeys} 的所有值合并到 {@literal destination} 键中
     *
     * @param destination 要将源密钥移动到的 HyperLogLog 的键
     * @param sourceKeys  不得为 {@literal null} 或 {@literal empty}
     * @return {@literal null} 在管道/事务中使用时。
     */
    @SuppressWarnings("unchecked")
    Long union(K destination, K... sourceKeys);

    /**
     * 删除给定的 {@literal key}
     *
     * @param key 不得为 {@literal null}
     */
    void delete(K key);
}
