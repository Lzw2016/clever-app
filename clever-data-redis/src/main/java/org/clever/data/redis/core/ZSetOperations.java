package org.clever.data.redis.core;

import org.clever.data.redis.connection.RedisZSetCommands.*;
import org.clever.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis ZSetsorted 集合具体操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:44 <br/>
 */
public interface ZSetOperations<K, V> {
    /**
     * 类型化的 ZSet 元组
     */
    interface TypedTuple<V> extends Comparable<TypedTuple<V>> {
        V getValue();

        Double getScore();

        /**
         * Create a new {@link TypedTuple}.
         *
         * @param value 不得为 {@literal null}
         * @param score 可以是 {@literal null}。
         * @return {@link TypedTuple} 的新实例
         */
        static <V> TypedTuple<V> of(V value, Double score) {
            return new DefaultTypedTuple<>(value, score);
        }
    }

    /**
     * 将 {@code value} 添加到 {@code key} 的排序集，或者更新它的 {@code score}（如果它已经存在）
     *
     * @param key   不得为 {@literal null}
     * @param value value
     * @param score score
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    Boolean add(K key, V value, double score);

    /**
     * 将 {@code value} 添加到 {@code key} 处的排序集（如果它尚不存在）
     *
     * @param key   不得为 {@literal null}
     * @param value value
     * @param score score
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD NX</a>
     */
    Boolean addIfAbsent(K key, V value, double score);

    /**
     * 将 {@code tuples} 添加到 {@code key} 处的排序集，或者更新其 {@code score}（如果已存在）
     *
     * @param key    不得为 {@literal null}
     * @param tuples 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    Long add(K key, Set<TypedTuple<V>> tuples);

    /**
     * 将 {@code tuples} 添加到 {@code key} 处的排序集（如果它尚不存在）
     *
     * @param key    不得为 {@literal null}
     * @param tuples 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD NX</a>
     */
    Long addIfAbsent(K key, Set<TypedTuple<V>> tuples);

    /**
     * 从排序集中删除 {@code values}。返回已删除元素的数量
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrem">Redis 文档: ZREM</a>
     */
    Long remove(K key, Object... values);

    /**
     * 在按 {@code increment}排序的设置中递增具有 {@code value} 的元素的分数。
     *
     * @param key   不得为 {@literal null}
     * @param value value
     * @param delta 要添加的增量。可以是负数
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zincrby">Redis 文档: ZINCRBY</a>
     */
    Double incrementScore(K key, V value, double delta);

    /**
     * 从 {@code key} 处获取随机元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    V randomMember(K key);

    /**
     * 从 {@code key} 处获取 {@code count} 个不同的随机元素
     *
     * @param key   不得为 {@literal null}
     * @param count 要返回的成员数
     * @return 如果 {@code key} 不存在，则为空 {@link Set}
     * @throws IllegalArgumentException 如果计数为负数
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    Set<V> distinctRandomMembers(K key, long count);

    /**
     * 从 {@code key} 处获取 {@code count} 个随机元素。
     *
     * @param key   不得为 {@literal null}
     * @param count 要返回的成员数
     * @return 如果 {@code key} 不存在，则为空 {@link List} 或 {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果计数为负数
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    List<V> randomMembers(K key, long count);

    /**
     * 获取随机元素，其分数从 {@code key} 设置
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    TypedTuple<V> randomMemberWithScore(K key);

    /**
     * 获取 {@code count} 个不同的随机元素，其分数从 {@code key} 设置
     *
     * @param key   不得为 {@literal null}
     * @param count 要返回的成员数
     * @return 如果 {@code key} 不存在，则为空 {@link Set}
     * @throws IllegalArgumentException 如果计数为负数
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    Set<TypedTuple<V>> distinctRandomMembersWithScore(K key, long count);

    /**
     * 获取 {@code count} 个随机元素，其分数从 {@code key} 设置
     *
     * @param key   不得为 {@literal null}
     * @param count 要返回的成员数
     * @return 如果 {@code key} 不存在，则为空 {@link List} 或 {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException if count is negative.
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    List<TypedTuple<V>> randomMembersWithScore(K key, long count);

    /**
     * 确定排序集中具有 {@code value} 的元素的索引
     *
     * @param key 不得为 {@literal null}
     * @param o   value
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrank">Redis 文档: ZRANK</a>
     */
    Long rank(K key, Object o);

    /**
     * 确定排序集中具有 {@code value} 的元素在得分从高到低时的索引
     *
     * @param key 不得为 {@literal null}
     * @param o   value
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrank">Redis 文档: ZREVRANK</a>
     */
    Long reverseRank(K key, Object o);

    /**
     * 从排序集中获取 {@code start} 和 {@code end} 之间的元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrange">Redis 文档: ZRANGE</a>
     */
    Set<V> range(K key, long start, long end);

    /**
     * 从排序集中获取 {@code start} 和 {@code end} 之间的 {@link Tuple} 集。
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrange">Redis 文档: ZRANGE</a>
     */
    Set<TypedTuple<V>> rangeWithScores(K key, long start, long end);

    /**
     * 从排序集中获取分数介于 {@code min} 和 {@code max} 之间的元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<V> rangeByScore(K key, double min, double max);

    /**
     * 从排序集中获取分数介于 {@code min} 和 {@code max} 之间的 {@link Tuple} 集
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<TypedTuple<V>> rangeByScoreWithScores(K key, double min, double max);

    /**
     * 从排序集中获取从 {@code start} 到 {@code end} 范围内的元素，其中分数介于 {@code min} 和 {@code max} 之间
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<V> rangeByScore(K key, double min, double max, long offset, long count);

    /**
     * 获取从 {@code start} 到 {@code end} 范围内的 {@link Tuple} 集，其中分数介于排序集中的 {@code min} 和 {@code max} 之间
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<TypedTuple<V>> rangeByScoreWithScores(K key, double min, double max, long offset, long count);

    /**
     * 从从高到低排序的排序集中获取从 {@code start} 到 {@code end} 范围内的元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrange">Redis 文档: ZREVRANGE</a>
     */
    Set<V> reverseRange(K key, long start, long end);

    /**
     * 从从高到低排序的排序集获取从 {@code start} 到 {@code end} 范围内的 {@link Tuple} 集
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrange">Redis 文档: ZREVRANGE</a>
     */
    Set<TypedTuple<V>> reverseRangeWithScores(K key, long start, long end);

    /**
     * 从从高到低排序的排序集中获取分数介于 {@code min} 和 {@code max} 之间的元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<V> reverseRangeByScore(K key, double min, double max);

    /**
     * 从从高到低排序的排序集中获取 {@link Tuple} 的集合，其中分数介于 {@code min} 和 {@code max} 之间
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<TypedTuple<V>> reverseRangeByScoreWithScores(K key, double min, double max);

    /**
     * 从排序集获取从 {@code Tuple} 到 {@code 结束} 范围内的元素，其中分数介于 {@code 分钟} 和 {@code max} 之间，排序集高 -> 低
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<V> reverseRangeByScore(K key, double min, double max, long offset, long count);

    /**
     * 从 {@code 开始} 到 {@code 结束} 的范围内获取 {@link Tuple} 的集合，其中分数介于 {@code 分钟} 和 {@code 最大} 之间，从排序集高 -> 低排序
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<TypedTuple<V>> reverseRangeByScoreWithScores(K key, double min, double max, long offset, long count);

    /**
     * 计算排序集中分数在 {@code min} 和 {@code max} 之间的元素数
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zcount">Redis 文档: ZCOUNT</a>
     */
    Long count(K key, double min, double max);

    /**
     * 计算排序集中的元素数，其值介于 {@code Range#min} 和 {@code Range#max} 之间，应用字典顺序
     *
     * @param key   不得为 {@literal null}
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zlexcount">Redis 文档: ZLEXCOUNT</a>
     */
    Long lexCount(K key, Range range);

    /**
     * 删除并返回其分数从 {@code key} 处排序集中得分最低的值
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当排序集为空或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zpopmin">Redis 文档: ZPOPMIN</a>
     */
    TypedTuple<V> popMin(K key);

    /**
     * 删除并返回 {@code count} 值，其分数从排序设置为 {@code key} 中得分最低
     *
     * @param key   不得为 {@literal null}
     * @param count 要弹出的元素数
     * @return {@literal null} 当排序集为空或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zpopmin">Redis 文档: ZPOPMIN</a>
     */
    Set<TypedTuple<V>> popMin(K key, long count);

    /**
     * 删除并返回其分数从 {@code key} 处排序集中得分最低的值。<br />
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param key  不得为 {@literal null}
     * @param unit 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/bzpopmin">Redis 文档: BZPOPMIN</a>
     */
    TypedTuple<V> popMin(K key, long timeout, TimeUnit unit);

    /**
     * 删除并返回其分数从 {@code key} 处排序集中得分最低的值。 <br />
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @throws IllegalArgumentException 如果超时为 {@literal空} 或负数
     * @see <a href="https://redis.io/commands/bzpopmin">Redis 文档: BZPOPMIN</a>
     */
    default TypedTuple<V> popMin(K key, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return popMin(key, TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 删除并返回其分数从 {@code key} 处排序集中得分最高的值
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当排序集为空或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zpopmax">Redis 文档: ZPOPMAX</a>
     */
    TypedTuple<V> popMax(K key);

    /**
     * 删除并返回 {@code count} 值，其分数从 {@code key} 处的排序集中获得最高分数
     *
     * @param key   不得为 {@literal null}
     * @param count 要弹出的元素数
     * @return {@literal null} 当排序集为空或在管道事务中使用时
     * @see <a href="https://redis.io/commands/zpopmax">Redis 文档: ZPOPMAX</a>
     */
    Set<TypedTuple<V>> popMax(K key, long count);

    /**
     * 删除并返回其分数从 {@code key} 处排序集中得分最高的值。 <br />
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param key  不得为 {@literal null}
     * @param unit 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/bzpopmax">Redis 文档: BZPOPMAX</a>
     */
    TypedTuple<V> popMax(K key, long timeout, TimeUnit unit);

    /**
     * 删除并返回其分数从 {@code key} 处排序集中得分最高的值。 <br />
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return 可以是 {@literal null}
     * @throws IllegalArgumentException 如果超时为 {@literal null} 或负数
     * @see <a href="https://redis.io/commands/bzpopmax">Redis 文档: BZPOPMAX</a>
     */
    default TypedTuple<V> popMax(K key, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return popMax(key, TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 返回使用给定 {@code key} 存储的排序集的元素数
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see #zCard(Object)
     * @see <a href="https://redis.io/commands/zcard">Redis 文档: ZCARD</a>
     */
    Long size(K key);

    /**
     * 使用 {@code key} 获取排序集的大小
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zcard">Redis 文档: ZCARD</a>
     */
    Long zCard(K key);

    /**
     * 从键 {@code key} 的排序集中获取具有 {@code value} 的元素的分数
     *
     * @param key 不得为 {@literal null}
     * @param o   value
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zscore">Redis 文档: ZSCORE</a>
     */
    Double score(K key, Object o);

    /**
     * 从键 {@code key} 的排序集中获取具有 {@code values} 的元素的分数
     *
     * @param key 不得为 {@literal null}
     * @param o   values
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zmscore">Redis 文档: ZMSCORE</a>
     */
    List<Double> score(K key, Object... o);

    /**
     * 从带有 {@code 键} 的排序集中删除 {@code 开始} 和 {@code 结束} 范围内的元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zremrangebyrank">Redis 文档: ZREMRANGEBYRANK</a>
     */
    Long removeRange(K key, long start, long end);

    /**
     * 使用 {@literal key} 从排序集中删除 {@link Range} 中的元素。
     *
     * @param key   不得为 {@literal null}
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zremrangebylex">Redis 文档: ZREMRANGEBYLEX</a>
     */
    Long removeRangeByLex(K key, Range range);

    /**
     * 从带有 {@code key} 的排序集中删除分数在 {@code min} 和 {@code max} 之间的元素
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zremrangebyscore">Redis 文档: ZREMRANGEBYSCORE</a>
     */
    Long removeRangeByScore(K key, double min, double max);

    /**
     * 差异排序 {@code sets}
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    default Set<V> difference(K key, K otherKey) {
        return difference(key, Collections.singleton(otherKey));
    }

    /**
     * 差异排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    Set<V> difference(K key, Collection<K> otherKeys);

    /**
     * 差异排序 {@code sets}
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    default Set<TypedTuple<V>> differenceWithScores(K key, K otherKey) {
        return differenceWithScores(key, Collections.singleton(otherKey));
    }

    /**
     * 差异排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    Set<TypedTuple<V>> differenceWithScores(K key, Collection<K> otherKeys);

    /**
     * 区分排序的 {@code sets} 并将结果存储在目标 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiffstore">Redis 文档: ZDIFFSTORE</a>
     */
    Long differenceAndStore(K key, Collection<K> otherKeys, K destKey);

    /**
     * 相交排序 {@code sets}
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    default Set<V> intersect(K key, K otherKey) {
        return intersect(key, Collections.singleton(otherKey));
    }

    /**
     * 相交排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<V> intersect(K key, Collection<K> otherKeys);

    /**
     * 相交排序 {@code sets}
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    default Set<TypedTuple<V>> intersectWithScores(K key, K otherKey) {
        return intersectWithScores(key, Collections.singleton(otherKey));
    }

    /**
     * 相交排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<TypedTuple<V>> intersectWithScores(K key, Collection<K> otherKeys);

    /**
     * 在 {@code key} 和 {@code otherKeys} 处相交排序集
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    default Set<TypedTuple<V>> intersectWithScores(K key, Collection<K> otherKeys, Aggregate aggregate) {
        return intersectWithScores(key, otherKeys, aggregate, Weights.fromSetCount(1 + otherKeys.size()));
    }

    /**
     * 相交排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<TypedTuple<V>> intersectWithScores(K key, Collection<K> otherKeys, Aggregate aggregate, Weights weights);

    /**
     * 在 {@code key} 和 {@code otherKey} 处相交排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long intersectAndStore(K key, K otherKey, K destKey);

    /**
     * 在 {@code key} 和 {@code otherKeys} 处相交排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long intersectAndStore(K key, Collection<K> otherKeys, K destKey);

    /**
     * 在 {@code key} 和 {@code otherKeys} 处相交排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    default Long intersectAndStore(K key, Collection<K> otherKeys, K destKey, Aggregate aggregate) {
        return intersectAndStore(key, otherKeys, destKey, aggregate, Weights.fromSetCount(1 + otherKeys.size()));
    }

    /**
     * 在 {@code key} 和 {@code otherKeys} 处相交排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long intersectAndStore(K key, Collection<K> otherKeys, K destKey, Aggregate aggregate, Weights weights);

    /**
     * 联合排序 {@code sets}
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    default Set<V> union(K key, K otherKey) {
        return union(key, Collections.singleton(otherKey));
    }

    /**
     * 联合排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<V> union(K key, Collection<K> otherKeys);

    /**
     * 联合排序 {@code sets}
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    default Set<TypedTuple<V>> unionWithScores(K key, K otherKey) {
        return unionWithScores(key, Collections.singleton(otherKey));
    }

    /**
     * 联合排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<TypedTuple<V>> unionWithScores(K key, Collection<K> otherKeys);

    /**
     * 在 {@code sets} 和 {@code otherKeys} 处并集排序集
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    default Set<TypedTuple<V>> unionWithScores(K key, Collection<K> otherKeys, Aggregate aggregate) {
        return unionWithScores(key, otherKeys, aggregate, Weights.fromSetCount(1 + otherKeys.size()));
    }

    /**
     * 联合排序 {@code sets}
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<TypedTuple<V>> unionWithScores(K key, Collection<K> otherKeys, Aggregate aggregate, Weights weights);

    /**
     * 将排序集合并在 {@code key} 和 {@code otherKeys} 并将结果存储在目标 {@code destKey} 中
     *
     * @param key      不得为 {@literal null}
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long unionAndStore(K key, K otherKey, K destKey);

    /**
     * 将排序集合并在 {@code key} 和 {@code otherKeys} 并将结果存储在目标 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long unionAndStore(K key, Collection<K> otherKeys, K destKey);

    /**
     * 将排序集合并在 {@code key} 和 {@code otherKeys} 并将结果存储在目标 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    default Long unionAndStore(K key, Collection<K> otherKeys, K destKey, Aggregate aggregate) {
        return unionAndStore(key, otherKeys, destKey, aggregate, Weights.fromSetCount(1 + otherKeys.size()));
    }

    /**
     * 将排序集合并在 {@code key} 和 {@code otherKeys} 并将结果存储在目标 {@code destKey} 中
     *
     * @param key       不得为 {@literal null}
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long unionAndStore(K key, Collection<K> otherKeys, K destKey, Aggregate aggregate, Weights weights);

    /**
     * 在 {@code key} 处遍历 zset 中的元素。 <br />
     * <strong>重要:</strong> 完成后调用 {@link Cursor#close()} 以避免资源泄漏
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zscan">Redis 文档: ZSCAN</a>
     */
    Cursor<TypedTuple<V>> scan(K key, ScanOptions options);

    /**
     * 从 {@literal ZSET} 的 {@code key} 获取所有具有字典顺序的元素，其值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间。
     *
     * @param key   不得为 {@literal null}
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebylex">Redis 文档: ZRANGEBYLEX</a>
     */
    default Set<V> rangeByLex(K key, Range range) {
        return rangeByLex(key, range, Limit.unlimited());
    }

    /**
     * 获取所有元素 {@literal n} elements，其中 {@literal n = } {@link Limit#getCount()}，
     * 从 {@link Limit#getOffset()} 开始，从 {@literal ZSET} 在 {@code key} 进行字典编纂排序，值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间。
     *
     * @param key   不得为 {@literal null}
     * @param range 不得为 {@literal null}
     * @param limit 可以是 {@literal null}。
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebylex">Redis 文档: ZRANGEBYLEX</a>
     */
    Set<V> rangeByLex(K key, Range range, Limit limit);

    /**
     * 从 {@code key} 处的 {@literal ZSET} 获取所有具有反向字典顺序的元素，其值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间。
     *
     * @param key   不得为 {@literal null}
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebylex">Redis 文档: ZREVRANGEBYLEX</a>
     */
    default Set<V> reverseRangeByLex(K key, Range range) {
        return reverseRangeByLex(key, range, Limit.unlimited());
    }

    /**
     * 获取所有元素 {@literal n} elements，其中 {@literal n = } {@link Limit#getCount()}，
     * 从 {@link Limit#getOffset()} 开始，反向词典编纂顺序从 {@literal ZSET} 在 {@code 键} 值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间。
     *
     * @param key   不得为 {@literal null}
     * @param range 不得为 {@literal null}
     * @param limit 可以是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebylex">Redis 文档: ZREVRANGEBYLEX</a>
     */
    Set<V> reverseRangeByLex(K key, Range range, Limit limit);

    /**
     * @return 从不为 {@literal null}
     */
    RedisOperations<K, V> getOperations();
}
