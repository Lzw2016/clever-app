package org.clever.data.redis.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis设置具体操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:56 <br/>
 */
public interface SetOperations<K, V> {
    /**
     * 添加给定的 {@code values} 以设置为 {@code key}
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sadd">Redis 文档: SADD</a>
     */
    @SuppressWarnings("unchecked")
    Long add(K key, V... values);

    /**
     * 从位于 {@code key} 的集合中移除给定的 {@code values} 并返回移除元素的数量
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/srem">Redis 文档: SREM</a>
     */
    Long remove(K key, Object... values);

    /**
     * 从 {@code key} 的集合中删除并返回一个随机成员
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/spop">Redis 文档: SPOP</a>
     */
    V pop(K key);

    /**
     * 从 {@code key} 的集合中删除并返回 {@code count} 个随机成员
     *
     * @param key   不得为 {@literal null}
     * @param count 从集合中弹出的随机成员数
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/spop">Redis 文档: SPOP</a>
     */
    List<V> pop(K key, long count);

    /**
     * 将 {@code value} 从 {@code key} 移动到 {@code destKey}
     *
     * @param key     不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/smove">Redis 文档: SMOVE</a>
     */
    Boolean move(K key, V value, K destKey);

    /**
     * 在 {@code key} 获取集合的大小
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/scard">Redis 文档: SCARD</a>
     */
    Long size(K key);

    /**
     * 检查设置在 {@code key} 是否包含 {@code value}
     *
     * @param key 不得为 {@literal null}
     * @param o   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sismember">Redis 文档: SISMEMBER</a>
     */
    Boolean isMember(K key, Object o);

    /**
     * 检查 set at {@code key} 是否包含一个或多个 {@code values}
     *
     * @param key     不得为 {@literal null}
     * @param objects 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/smismember">Redis 文档: SMISMEMBER</a>
     * @since 2.6
     */
    Map<Object, Boolean> isMember(K key, Object... objects);

    /**
     * 返回在 {@code key} 和 {@code otherKey} 处与所有给定集相交的成员
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinter">Redis 文档: SINTER</a>
     */
    Set<V> intersect(K key, K otherKey);

    /**
     * 返回在 {@code key} 和 {@code otherKeys} 处与所有给定集相交的成员
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinter">Redis 文档: SINTER</a>
     */
    Set<V> intersect(K key, Collection<K> otherKeys);

    /**
     * 返回在 {@code keys} 处与所有给定集相交的成员
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinter">Redis 文档: SINTER</a>
     */
    Set<V> intersect(Collection<K> keys);

    /**
     * 在 {@code key} 和 {@code otherKey} 处将所有给定的集合相交，并将结果存储在 {@code destKey} 中
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinterstore">Redis 文档: SINTERSTORE</a>
     */
    Long intersectAndStore(K key, K otherKey, K destKey);

    /**
     * 在 {@code key} 和 {@code otherKeys} 处将所有给定的集合相交，并将结果存储在 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinterstore">Redis 文档: SINTERSTORE</a>
     */
    Long intersectAndStore(K key, Collection<K> otherKeys, K destKey);

    /**
     * 在 {@code keys} 处与所有给定集相交并将结果存储在 {@code destKey} 中
     *
     * @param keys    不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sinterstore">Redis 文档: SINTERSTORE</a>
     */
    Long intersectAndStore(Collection<K> keys, K destKey);

    /**
     * 在给定的 {@code keys} 和 {@code otherKey} 处合并所有集合
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunion">Redis 文档: SUNION</a>
     */
    Set<V> union(K key, K otherKey);

    /**
     * 在给定的 {@code keys} 和 {@code otherKeys} 处合并所有集合
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunion">Redis 文档: SUNION</a>
     */
    Set<V> union(K key, Collection<K> otherKeys);

    /**
     * 在给定的 {@code keys} 联合所有集合
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunion">Redis 文档: SUNION</a>
     */
    Set<V> union(Collection<K> keys);

    /**
     * 在给定的 {@code key} 和 {@code otherKey} 处合并所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunionstore">Redis 文档: SUNIONSTORE</a>
     */
    Long unionAndStore(K key, K otherKey, K destKey);

    /**
     * 在给定的 {@code key} 和 {@code otherKeys} 处合并所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunionstore">Redis 文档: SUNIONSTORE</a>
     */
    Long unionAndStore(K key, Collection<K> otherKeys, K destKey);

    /**
     * 在给定的 {@code keys} 处合并所有集合并将结果存储在 {@code destKey} 中
     *
     * @param keys    不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sunionstore">Redis 文档: SUNIONSTORE</a>
     */
    Long unionAndStore(Collection<K> keys, K destKey);

    /**
     * 区分给定 {@code key} 和 {@code otherKey} 的所有集合
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiff">Redis 文档: SDIFF</a>
     */
    Set<V> difference(K key, K otherKey);

    /**
     * 区分给定 {@code key} 和 {@code otherKeys} 的所有集合
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiff">Redis 文档: SDIFF</a>
     */
    Set<V> difference(K key, Collection<K> otherKeys);

    /**
     * 区分给定 {@code keys} 的所有集合
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiff">Redis 文档: SDIFF</a>
     */
    Set<V> difference(Collection<K> keys);

    /**
     * 区分给定 {@code key} 和 {@code otherKey} 的所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiffstore">Redis 文档: SDIFFSTORE</a>
     */
    Long differenceAndStore(K key, K otherKey, K destKey);

    /**
     * 区分给定 {@code key} 和 {@code otherKeys} 的所有集合，并将结果存储在 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiffstore">Redis 文档: SDIFFSTORE</a>
     */
    Long differenceAndStore(K key, Collection<K> otherKeys, K destKey);

    /**
     * 区分给定 {@code keys} 的所有集合并将结果存储在 {@code destKey} 中
     *
     * @param keys    不得为 {@literal null}
     * @param destKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sdiffstore">Redis 文档: SDIFFSTORE</a>
     */
    Long differenceAndStore(Collection<K> keys, K destKey);

    /**
     * 在 {@code key} 处获取集合的所有元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/smembers">Redis 文档: SMEMBERS</a>
     */
    Set<V> members(K key);

    /**
     * 从 {@code key} 的集合中获取随机元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    V randomMember(K key);

    /**
     * 从 {@code key} 的集合中获取 {@code count} 个不同的随机元素
     *
     * @param key   不得为 {@literal null}
     * @param count 返回的成员数量
     * @return 如果 {@code key} 不存在，则为空 {@link Set}
     * @throws IllegalArgumentException 如果计数为负
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    Set<V> distinctRandomMembers(K key, long count);

    /**
     * 从 {@code key} 的集合中获取 {@code count} 个随机元素
     *
     * @param key   不得为 {@literal null}
     * @param count 要返回的成员数
     * @return 清空 {@link List} 如果 {@code key} 不存在或 {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果计数为负
     * @see <a href="https://redis.io/commands/srandmember">Redis 文档: SRANDMEMBER</a>
     */
    List<V> randomMembers(K key, long count);

    /**
     * 在 {@code key} 处迭代集合中的元素。 <br />
     * <strong>重要：</strong> 完成后调用 {@link Cursor#close()} 以避免资源泄漏
     */
    Cursor<V> scan(K key, ScanOptions options);

    RedisOperations<K, V> getOperations();
}
