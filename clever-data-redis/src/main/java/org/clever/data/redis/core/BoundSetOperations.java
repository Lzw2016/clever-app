package org.clever.data.redis.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设置与某个 key 绑定的操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:57 <br/>
 */
public interface BoundSetOperations<K, V> extends BoundKeyOperations<K> {
    /**
     * 添加给定的 {@code values} 以在绑定键处设置
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sadd">Redis 文档: SADD</a>
     */
    @SuppressWarnings("unchecked")
    Long add(V... values);

    /**
     * 从绑定键处的集合中删除给定的 {@code values}，并返回已删除元素的数量
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/srem">Redis 文档: SREM</a>
     */
    Long remove(Object... values);

    /**
     * 从绑定键处的集合中删除并返回随机成员
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/spop">Redis 文档: SPOP</a>
     */
    V pop();

    /**
     * 将 {@code value} 从绑定键移动到 {@code destKey}
     *
     * @param destKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/smove">Redis 文档: SMOVE</a>
     */
    Boolean move(K destKey, V value);

    /**
     * 获取绑定键处设置的大小
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/scard">Redis 文档: SCARD</a>
     */
    Long size();

    /**
     * 检查在绑定键处设置的是否包含 {@code value}
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sismember">Redis 文档: SISMEMBER</a>
     */
    Boolean isMember(Object o);

    /**
     * 检查在绑定键处设置的是否包含一个或多个 {@code values}
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/smismember">Redis 文档: SMISMEMBER</a>
     */
    Map<Object, Boolean> isMember(Object... objects);

    /**
     * 返回在绑定键和 {@code key} 处与所有给定集合相交的成员
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinter">Redis 文档: SINTER</a>
     */
    Set<V> intersect(K key);

    /**
     * 返回在绑定键和 {@code keys} 处与所有给定集合相交的成员
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinter">Redis 文档: SINTER</a>
     */
    Set<V> intersect(Collection<K> keys);

    /**
     * 在绑定键和 {@code key} 处与所有给定的集合相交，并将结果存储在 {@code destKey} 中
     *
     * @param key     不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/sinterstore">Redis 文档: SINTERSTORE</a>
     */
    void intersectAndStore(K key, K destKey);

    /**
     * 在绑定键和 {@code keys} 处与所有给定的集合相交，并将结果存储在 {@code destKey} 中
     *
     * @param keys    不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/sinterstore">Redis 文档: SINTERSTORE</a>
     */
    void intersectAndStore(Collection<K> keys, K destKey);

    /**
     * 在给定的 {@code key} 和 {@code key} 处合并所有集合
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunion">Redis 文档: SUNION</a>
     */
    Set<V> union(K key);

    /**
     * 在给定的 {@code keys} 和 {@code keys} 处合并所有集合
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunion">Redis 文档: SUNION</a>
     */
    Set<V> union(Collection<K> keys);

    /**
     * 在给定的绑定键和 {@code key} 处合并所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param key     不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/sunionstore">Redis 文档: SUNIONSTORE</a>
     */
    void unionAndStore(K key, K destKey);

    /**
     * 在给定的绑定键和 {@code keys} 处合并所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param keys    不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/sunionstore">Redis 文档: SUNIONSTORE</a>
     */
    void unionAndStore(Collection<K> keys, K destKey);

    /**
     * 区分给定绑定键和 {@code key} 的所有集合
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiff">Redis 文档: SDIFF</a>
     */
    Set<V> diff(K key);

    /**
     * 区分给定绑定键和 {@code keys} 的所有集合
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiff">Redis 文档: SDIFF</a>
     */
    Set<V> diff(Collection<K> keys);

    /**
     * 区分给定绑定键和 {@code keys} 的所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param keys    不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/sdiffstore">Redis 文档: SDIFFSTORE</a>
     */
    void diffAndStore(K keys, K destKey);

    /**
     * 区分给定绑定键和 {@code keys} 的所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param keys    不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/sdiffstore">Redis 文档: SDIFFSTORE</a>
     */
    void diffAndStore(Collection<K> keys, K destKey);

    /**
     * 获取绑定键处集合的所有元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/smembers">Redis 文档: SMEMBERS</a>
     */
    Set<V> members();

    /**
     * 从绑定键的集合中获取随机元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    V randomMember();

    /**
     * 从绑定键的集合中获取 {@code count} 个不同的随机元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    Set<V> distinctRandomMembers(long count);

    /**
     * 从绑定键的集合中获取 {@code count} 个随机元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    List<V> randomMembers(long count);

    /**
     * @return {@literal null} 在管道/事务中使用时。
     */
    Cursor<V> scan(ScanOptions options);

    /**
     * @return 从不为 {@literal null}
     */
    RedisOperations<K, V> getOperations();
}
