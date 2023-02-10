package org.clever.data.redis.core;

import org.clever.data.redis.connection.RedisZSetCommands.*;
import org.clever.data.redis.core.ZSetOperations.TypedTuple;
import org.clever.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 绑定到某个键的ZSet（或SortedSet）操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:45 <br/>
 */
public interface BoundZSetOperations<K, V> extends BoundKeyOperations<K> {
    /**
     * 将 {@code value} 添加到绑定键处的排序集，或者更新其 {@code score} （如果它已经存在）
     *
     * @param value value
     * @param score score
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    Boolean add(V value, double score);

    /**
     * 将 {@code value} 添加到绑定键处的已排序集合（如果该集合不存在）
     *
     * @param value value
     * @param score score
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD NX</a>
     */
    Boolean addIfAbsent(V value, double score);

    /**
     * 将 {@code tuples} 添加到绑定键处的排序集，或者更新其 {@code score} （如果它已经存在）
     *
     * @param tuples 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    Long add(Set<TypedTuple<V>> tuples);

    /**
     * 将 {@code tuples} 添加到绑定键处的已排序集合（如果该集合不存在）
     *
     * @param tuples 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD NX</a>
     */
    Long addIfAbsent(Set<TypedTuple<V>> tuples);

    /**
     * 从排序集中删除 {@code values} 。返回已删除元素的数量
     *
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrem">Redis 文档: ZREM</a>
     */
    Long remove(Object... values);

    /**
     * 按 {@code increment} 递增排序集中的 {@code value} 元素的分数
     *
     * @param value value.
     * @param delta 要添加的增量。可以是负数
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zincrby">Redis 文档: ZINCRBY</a>
     */
    Double incrementScore(V value, double delta);

    /**
     * 从绑定键处的集合中获取随机元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    V randomMember();

    /**
     * 从绑定键的集合中获取 {@code count} 个不同的随机元素
     *
     * @param count 要返回的成员数
     * @return 如果 {@code key} 不存在，则为空 {@link Set}
     * @throws IllegalArgumentException 如果计数为负
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    Set<V> distinctRandomMembers(long count);

    /**
     * 从绑定键处的集合中获取 {@code count} 个随机元素
     *
     * @param count 要返回的成员数
     * @return 如果 {@code key} 不存在或在管道/事务中使用时 {@literal null} ，则为空  {@link List}
     * @throws IllegalArgumentException 如果计数为负
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    List<V> randomMembers(long count);

    /**
     * Get random element with its score from set at the bound key.
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    TypedTuple<V> randomMemberWithScore();

    /**
     * 获取 {@code count} 个不同的随机元素，并在绑定键处设置其得分
     *
     * @param count 要返回的成员数
     * @return 如果 {@code key} 不存在，则为空 {@link Set}
     * @throws IllegalArgumentException 如果计数为负
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    Set<TypedTuple<V>> distinctRandomMembersWithScore(long count);

    /**
     * Get {@code count} random elements with their score from set at the bound key.
     *
     * @param count number of members to return.
     * @return 如果 {@code key} 不存在或在管道/事务中使用时 {@literal null} ，则为空  {@link List}
     * @throws IllegalArgumentException 如果计数为负
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    List<TypedTuple<V>> randomMembersWithScore(long count);

    /**
     * 确定排序集中具有 {@code value} 的元素的索引
     *
     * @param o value
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrank">Redis 文档: ZRANK</a>
     */
    Long rank(Object o);

    /**
     * 当得分从高到低时，确定排序集中具有 {@code value} 的元素的索引
     *
     * @param o value
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrank">Redis 文档: ZREVRANK</a>
     */
    Long reverseRank(Object o);

    /**
     * 从排序集中获取 {@code start} 和 {@code end} 之间的元素。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrange">Redis 文档: ZRANGE</a>
     */
    Set<V> range(long start, long end);

    /**
     * 从排序集获取 {@code start} 和 {@code end} 之间的 {@link Tuple} 集
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrange">Redis 文档: ZRANGE</a>
     */
    Set<TypedTuple<V>> rangeWithScores(long start, long end);

    /**
     * 从排序集中获取分数介于 {@code min} 和 {@code max} 之间的元素。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<V> rangeByScore(double min, double max);

    /**
     * 获取 {@link Tuple} 的集合，其中分数在排序集合中的 {@code min} 和 {@code max} 之间。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<TypedTuple<V>> rangeByScoreWithScores(double min, double max);

    /**
     * 从从高到低排序的集合中获取范围从 {@code start} 和 {@code end} 的元素。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrange">Redis 文档: ZREVRANGE</a>
     */
    Set<V> reverseRange(long start, long end);

    /**
     * 从从高到低排序的集合中获取范围从 {@code start} 和 {@code end} 的 {@link Tuple} 集合。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrange">Redis 文档: ZREVRANGE</a>
     */
    Set<TypedTuple<V>> reverseRangeWithScores(long start, long end);

    /**
     * 从从高到低排序的集合中获取得分介于 {@code min} 和 {@code max} 之间的元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<V> reverseRangeByScore(double min, double max);

    /**
     * 获取 {@link Tuple} 的集合，其中得分介于 {@code min} 和 {@code max} 之间，从高到低排序。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<TypedTuple<V>> reverseRangeByScoreWithScores(double min, double max);

    /**
     * 计数排序集中得分介于 {@code min} 和 {@code max} 之间的元素数。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zcount">Redis 文档: ZCOUNT</a>
     */
    Long count(double min, double max);

    /**
     * 应用字典排序，计算排序集内值介于 {@code Range#min} 和 {@code Range#max} 之间的元素数
     *
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zlexcount">Redis 文档: ZLEXCOUNT</a>
     */
    Long lexCount(Range range);

    /**
     * 移除并返回绑定键处排序集中得分最低的值
     *
     * @return 当排序集为空或在管道/事务中使用时，{@literal null}
     * @see <a href="https://redis.io/commands/zpopmin">Redis 文档: ZPOPMIN</a>
     */
    TypedTuple<V> popMin();

    /**
     * 移除并返回 {@code count} 值，这些值的得分在绑定键处的排序集中最低。
     *
     * @param count 要弹出的元素数
     * @return 当排序集为空或在管道/事务中使用时，{@literal null}
     * @see <a href="https://redis.io/commands/zpopmin">Redis 文档: ZPOPMIN</a>
     */
    Set<TypedTuple<V>> popMin(long count);

    /**
     * 移除并返回绑定键处排序集中得分最低的值。
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param unit 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/bzpopmin">Redis 文档: BZPOPMIN</a>
     */
    TypedTuple<V> popMin(long timeout, TimeUnit unit);

    /**
     * 移除并返回绑定键处排序集中得分最低的值。
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param timeout 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @throws IllegalArgumentException 如果超时为 {@literal null} 或负值
     * @see <a href="https://redis.io/commands/bzpopmin">Redis 文档: BZPOPMIN</a>
     */
    default TypedTuple<V> popMin(Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return popMin(TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 移除并返回绑定键处排序集中得分最高的值
     *
     * @return 当排序集为空或在管道/事务中使用时，{@literal null}
     * @see <a href="https://redis.io/commands/zpopmax">Redis 文档: ZPOPMAX</a>
     */
    TypedTuple<V> popMax();

    /**
     * 移除并返回 {@code count} 值，这些值的得分在绑定键处的排序集中最高。
     *
     * @param count 要弹出的元素数
     * @return 当排序集为空或在管道/事务中使用时，{@literal null}
     * @see <a href="https://redis.io/commands/zpopmax">Redis 文档: ZPOPMAX</a>
     */
    Set<TypedTuple<V>> popMax(long count);

    /**
     * 移除并返回绑定键处排序集中得分最高的值。
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param unit 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/bzpopmax">Redis 文档: BZPOPMAX</a>
     */
    TypedTuple<V> popMax(long timeout, TimeUnit unit);

    /**
     * 移除并返回绑定键处排序集中得分最高的值。
     * <b>阻止连接</b> 直到元素可用或达到 {@code timeout}
     *
     * @param timeout 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @throws IllegalArgumentException 如果超时为 {@literal null} 或负值
     * @see <a href="https://redis.io/commands/bzpopmax">Redis 文档: BZPOPMAX</a>
     */
    default TypedTuple<V> popMax(Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return popMax(TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 返回使用给定绑定键存储的排序集的元素数
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see #zCard()
     * @see <a href="https://redis.io/commands/zcard">Redis 文档: ZCARD</a>
     */
    Long size();

    /**
     * 使用绑定键获取排序集的大小
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zcard">Redis 文档: ZCARD</a>
     */
    Long zCard();

    /**
     * 从带有键绑定键的排序集中获取带有 {@code value} 的元素的分数
     *
     * @param o the value.
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zscore">Redis 文档: ZSCORE</a>
     */
    Double score(Object o);

    /**
     * 从带有键绑定键的排序集中获取带有 {@code values} 的元素的分数。
     *
     * @param o the values.
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zmscore">Redis 文档: ZMSCORE</a>
     */
    List<Double> score(Object... o);

    /**
     * 使用绑定键从排序集中删除 {@code start} 和 {@code end} 之间范围内的元素。
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zremrangebyrank">Redis 文档: ZREMRANGEBYRANK</a>
     */
    Long removeRange(long start, long end);

    /**
     * 使用绑定键从排序集中删除 {@link Range} 中的元素
     *
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zremrangebylex">Redis 文档: ZREMRANGEBYLEX</a>
     */
    Long removeRangeByLex(Range range);

    /**
     * 使用绑定键从排序集中删除分数介于 {@code min} 和 {@code max} 之间的元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zremrangebyscore">Redis 文档: ZREMRANGEBYSCORE</a>
     */
    Long removeRangeByScore(double min, double max);

    /**
     * 将绑定键和 {@code otherKeys} 处的排序集合并，并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long unionAndStore(Collection<K> otherKeys, K destKey, Aggregate aggregate, Weights weights);

    /**
     * 差异排序 {@code sets}
     *
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    default Set<V> difference(K otherKey) {
        return difference(Collections.singleton(otherKey));
    }

    /**
     * 差异排序 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    Set<V> difference(Collection<K> otherKeys);

    /**
     * 差异排序 {@code sets}
     *
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    default Set<TypedTuple<V>> differenceWithScores(K otherKey) {
        return differenceWithScores(Collections.singleton(otherKey));
    }

    /**
     * 差异排序 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    Set<TypedTuple<V>> differenceWithScores(Collection<K> otherKeys);

    /**
     * 区分排序的 {@code sets} 并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiffstore">Redis 文档: ZDIFFSTORE</a>
     */
    default Long differenceAndStore(K otherKey, K destKey) {
        return differenceAndStore(Collections.singleton(otherKey), destKey);
    }

    /**
     * 区分排序的 {@code sets} 并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zdiffstore">Redis 文档: ZDIFFSTORE</a>
     */
    Long differenceAndStore(Collection<K> otherKeys, K destKey);

    /**
     * 相交排序的 {@code sets}
     *
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    default Set<V> intersect(K otherKey) {
        return intersect(Collections.singleton(otherKey));
    }

    /**
     * 相交排序的 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<V> intersect(Collection<K> otherKeys);

    /**
     * 相交排序的 {@code sets}
     *
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    default Set<TypedTuple<V>> intersectWithScores(K otherKey) {
        return intersectWithScores(Collections.singleton(otherKey));
    }

    /**
     * 相交排序的 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<TypedTuple<V>> intersectWithScores(Collection<K> otherKeys);

    /**
     * 相交排序的 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<TypedTuple<V>> intersectWithScores(Collection<K> otherKeys, Aggregate aggregate, Weights weights);

    /**
     * 在绑定键和 {@code otherKey} 处相交排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long intersectAndStore(K otherKey, K destKey);

    /**
     * 在绑定键和 {@code otherKeys} 处相交排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long intersectAndStore(Collection<K> otherKeys, K destKey);

    /**
     * 在绑定键和 {@code otherKeys} 处相交排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long intersectAndStore(Collection<K> otherKeys, K destKey, Aggregate aggregate);

    /**
     * 在绑定键和 {@code otherKeys} 处相交排序集，并将结果存储在目标 {@code destKey} 中。
     *
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long intersectAndStore(Collection<K> otherKeys, K destKey, Aggregate aggregate, Weights weights);

    /**
     * 联合排序 {@code sets}
     *
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    default Set<V> union(K otherKey) {
        return union(Collections.singleton(otherKey));
    }

    /**
     * 联合排序 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<V> union(Collection<K> otherKeys);

    /**
     * 联合排序 {@code sets}
     *
     * @param otherKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    default Set<TypedTuple<V>> unionWithScores(K otherKey) {
        return unionWithScores(Collections.singleton(otherKey));
    }

    /**
     * 联合排序 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<TypedTuple<V>> unionWithScores(Collection<K> otherKeys);

    /**
     * 在绑定键和 {@code otherKeys} 处联合排序集
     *
     * @param otherKeys 不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    default Set<TypedTuple<V>> unionWithScores(Collection<K> otherKeys, Aggregate aggregate) {
        return unionWithScores(otherKeys, aggregate, Weights.fromSetCount(1 + otherKeys.size()));
    }

    /**
     * 联合排序 {@code sets}
     *
     * @param otherKeys 不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @param weights   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<TypedTuple<V>> unionWithScores(Collection<K> otherKeys, Aggregate aggregate, Weights weights);

    /**
     * 在绑定键和 {@code otherKeys} 处合并排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKey 不得为 {@literal null}
     * @param destKey  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long unionAndStore(K otherKey, K destKey);

    /**
     * 在绑定键和 {@code otherKeys} 处合并排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long unionAndStore(Collection<K> otherKeys, K destKey);

    /**
     * 在绑定键和 {@code otherKeys} 处合并排序集，并将结果存储在目标 {@code destKey} 中
     *
     * @param otherKeys 不得为 {@literal null}
     * @param destKey   不得为 {@literal null}
     * @param aggregate 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long unionAndStore(Collection<K> otherKeys, K destKey, Aggregate aggregate);

    /**
     * 在绑定键处循环访问 zset 中的元素。 <br />
     * <strong>重要:</strong> 完成后调用 {@link Cursor#close()} 以避免资源泄漏。
     */
    Cursor<TypedTuple<V>> scan(ScanOptions options);

    /**
     * 获取具有字典排序的所有元素，其值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间
     *
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebylex">Redis 文档: ZRANGEBYLEX</a>
     */
    default Set<V> rangeByLex(Range range) {
        return rangeByLex(range, Limit.unlimited());
    }

    /**
     * 获取所有元素 {@literal n} elements，其中 {@literal n = } {@link Limit#getCount()}，
     * 从 {@link Limit#getOffset()} 开始，词典排序的值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间。
     *
     * @param range 不得为 {@literal null}
     * @param limit 可以是 {@literal null}。
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrangebylex">Redis 文档: ZRANGEBYLEX</a>
     */
    Set<V> rangeByLex(Range range, Limit limit);

    /**
     * 获取具有反向字典顺序的所有元素，其值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间
     *
     * @param range 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebylex">Redis 文档: ZREVRANGEBYLEX</a>
     */
    default Set<V> reverseRangeByLex(Range range) {
        return reverseRangeByLex(range, Limit.unlimited());
    }

    /**
     * 获取所有元素 {@literal n} elements，其中 {@literal n = } {@link Limit#getCount()}，
     * 从 {@link Limit#getOffset()} 开始，反向词典排序的值介于 {@link Range#getMin()} 和 {@link Range#getMax()} 之间。
     *
     * @param range 不得为 {@literal null}
     * @param limit 可以是 {@literal null}。
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/zrevrangebylex">Redis 文档: ZREVRANGEBYLEX</a>
     */
    Set<V> reverseRangeByLex(Range range, Limit limit);

    /**
     * @return 从不为 {@literal null}
     */
    RedisOperations<K, V> getOperations();
}
