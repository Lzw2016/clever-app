package org.clever.data.redis.core;

import org.clever.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.clever.data.redis.connection.RedisListCommands.Direction;

/**
 * Redis List具体操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:44 <br/>
 */
public interface ListOperations<K, V> {
    /**
     * 从位于 {@code key} 的List中获取 {@code begin} 和 {@code end} 之间的元素
     *
     * @param key   不得为 {@literal null}
     * @param start 不得为 {@literal null}
     * @param end   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lrange">Redis 文档: LRANGE</a>
     */
    List<V> range(K key, long start, long end);

    /**
     * 将 {@code key} 处的列表修剪为 {@code start} 和 {@code end} 之间的元素
     *
     * @param key   不得为 {@literal null}
     * @param start 不得为 {@literal null}
     * @param end   不得为 {@literal null}
     * @see <a href="https://redis.io/commands/ltrim">Redis 文档: LTRIM</a>
     */
    void trim(K key, long start, long end);

    /**
     * 获取存储在 {@code key} 的列表的大小
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/llen">Redis 文档: LLEN</a>
     */
    Long size(K key);

    /**
     * 将 {@code value} 添加到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: LPUSH</a>
     */
    Long leftPush(K key, V value);

    /**
     * 将 {@code values} 添加到 {@code key}
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: LPUSH</a>
     */
    @SuppressWarnings("unchecked")
    Long leftPushAll(K key, V... values);

    /**
     * 将 {@code values} 添加到 {@code key}
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: LPUSH</a>
     */
    Long leftPushAll(K key, Collection<V> values);

    /**
     * 仅当列表存在时才将 {@code values} 添加到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpushx">Redis 文档: LPUSHX</a>
     */
    Long leftPushIfPresent(K key, V value);

    /**
     * 在 {@code pivot} 之前将 {@code value} 插入到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param pivot 不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/linsert">Redis 文档: LINSERT</a>
     */
    Long leftPush(K key, V pivot, V value);

    /**
     * 将 {@code value} 附加到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpush">Redis 文档: RPUSH</a>
     */
    Long rightPush(K key, V value);

    /**
     * 将 {@code values} 附加到 {@code key}
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpush">Redis 文档: RPUSH</a>
     */
    @SuppressWarnings("unchecked")
    Long rightPushAll(K key, V... values);

    /**
     * 将 {@code values} 附加到 {@code key}
     *
     * @param key    不得为 {@literal null}
     * @param values 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpush">Redis 文档: RPUSH</a>
     */
    Long rightPushAll(K key, Collection<V> values);

    /**
     * 仅当列表存在时，才将 {@code values} 附加到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpushx">Redis 文档: RPUSHX</a>
     */
    Long rightPushIfPresent(K key, V value);

    /**
     * 在 {@code pivot} 之后将 {@code value} 插入到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param pivot 不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/linsert">Redis 文档: LINSERT</a>
     */
    Long rightPush(K key, V pivot, V value);

    /**
     * 表示 {@code LMOVE} 命令的 {@code where from} 部分的值对象
     *
     * @see <a href="https://redis.io/commands/lmove">Redis 文档: LMOVE</a>
     */
    class MoveFrom<K> {
        final K key;
        final Direction direction;

        MoveFrom(K key, Direction direction) {
            this.key = key;
            this.direction = direction;
        }

        public static <K> MoveFrom<K> fromHead(K key) {
            return new MoveFrom<>(key, Direction.first());
        }

        public static <K> MoveFrom<K> fromTail(K key) {
            return new MoveFrom<>(key, Direction.last());
        }
    }

    /**
     * 表示 {@code LMOVE} 命令的 {@code where to} 部分的值对象
     *
     * @see <a href="https://redis.io/commands/lmove">Redis 文档: LMOVE</a>
     */
    class MoveTo<K> {
        final K key;
        final Direction direction;

        MoveTo(K key, Direction direction) {
            this.key = key;
            this.direction = direction;
        }

        public static <K> MoveTo<K> toHead(K key) {
            return new MoveTo<>(key, Direction.first());
        }

        public static <K> MoveTo<K> toTail(K key) {
            return new MoveTo<>(key, Direction.last());
        }
    }

    /**
     * 原子地返回并删除存储在 {@code sourceKey} 列表的第一个元素（head/tail 取决于 {@code from} 参数），
     * 并将该元素推送到 first/last 元素（head/tail 取决于 {@code to} 参数）存储在 {@code destinationKey} 的列表
     *
     * @param from 不得为 {@literal null}
     * @param to   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lmove">Redis 文档: LMOVE</a>
     */
    default V move(MoveFrom<K> from, MoveTo<K> to) {
        Assert.notNull(from, "Move from must not be null");
        Assert.notNull(to, "Move to must not be null");
        return move(from.key, from.direction, to.key, to.direction);
    }

    /**
     * 原子地返回并删除存储在 {@code sourceKey} 列表的第一个元素（head/tail 取决于 {@code from} 参数），
     * 并将该元素推送到 first/last 元素（head/tail 取决于 {@code to} 参数）存储在 {@code destinationKey} 的列表。
     *
     * @param sourceKey      不得为 {@literal null}
     * @param from           不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param to             不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lmove">Redis 文档: LMOVE</a>
     */
    V move(K sourceKey, Direction from, K destinationKey, Direction to);

    /**
     * 原子地返回并删除存储在 {@code sourceKey} 列表的第一个元素（head/tail 取决于 {@code from} 参数），
     * 并将该元素推送到 first/last 元素（head/tail 取决于 {@code to} 参数）存储在 {@code destinationKey} 的列表。
     * <p>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}
     *
     * @param from    不得为 {@literal null}
     * @param to      不得为 {@literal null}
     * @param timeout 不得为 {@literal null} 或负数
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/blmove">Redis 文档: BLMOVE</a>
     */
    default V move(MoveFrom<K> from, MoveTo<K> to, Duration timeout) {
        Assert.notNull(from, "Move from must not be null");
        Assert.notNull(to, "Move to must not be null");
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return move(
                from.key,
                from.direction,
                to.key,
                to.direction,
                TimeoutUtils.toMillis(timeout.toMillis(), TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 原子地返回并删除存储在 {@code sourceKey} 列表的第一个元素（head/tail 取决于 {@code from} 参数），
     * 并将该元素推送到 first/last 元素（head/tail 取决于 {@code to} 参数）存储在 {@code destinationKey} 的列表。
     * <p>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}
     *
     * @param sourceKey      不得为 {@literal null}
     * @param from           不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param to             不得为 {@literal null}
     * @param timeout        不得为 {@literal null} 或负数
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/blmove">Redis 文档: BLMOVE</a>
     */
    default V move(K sourceKey, Direction from, K destinationKey, Direction to, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return move(
                sourceKey,
                from,
                destinationKey,
                to,
                TimeoutUtils.toMillis(timeout.toMillis(), TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 原子地返回并删除存储在 {@code sourceKey} List的第一个元素（headtail 取决于 {@code from} 参数），
     * 并将该元素推送到 firstlast 元素（headtail 取决于 {@code to} 参数）存储在 {@code destinationKey} 的List。
     * <p>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}
     *
     * @param sourceKey      不得为 {@literal null}
     * @param from           不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param to             不得为 {@literal null}
     * @param timeout        不得为 {@literal null}
     * @param unit           不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/blmove">Redis 文档: BLMOVE</a>
     */
    V move(K sourceKey, Direction from, K destinationKey, Direction to, long timeout, TimeUnit unit);

    /**
     * 在 {@code index} 设置 {@code value} List元素。
     *
     * @param key   不得为 {@literal null}
     * @param index 不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/lset">Redis 文档: LSET</a>
     */
    void set(K key, long index, V value);

    /**
     * 从存储在 {@code key} 的List中删除前 {@code count} 次出现的 {@code value}。
     *
     * @param key   不得为 {@literal null}
     * @param count 不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lrem">Redis 文档: LREM</a>
     */
    Long remove(K key, long count, Object value);

    /**
     * 在 {@code key} 的 {@code index} 表单List中获取元素。
     *
     * @param key   不得为 {@literal null}
     * @param index 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lindex">Redis 文档: LINDEX</a>
     */
    V index(K key, long index);

    /**
     * 返回指定值在List中第一次出现的索引，位于 {@code key}。 <br />
     * 需要 Redis 6.0.6 或更新版本。
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} when used in pipeline / transaction or when not contained in list.
     * @see <a href="https://redis.io/commands/lpos">Redis 文档: LPOS</a>
     */
    Long indexOf(K key, V value);

    /**
     * 返回指定值在List中最后一次出现的索引，位于 {@code key}。<br />
     * 需要 Redis 6.0.6 或更新版本。
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} when used in pipeline / transaction or when not contained in list.
     * @see <a href="https://redis.io/commands/lpos">Redis 文档: LPOS</a>
     */
    Long lastIndexOf(K key, V value);

    /**
     * 删除并返回存储在 {@code key} 的List中的第一个元素
     *
     * @param key 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/lpop">Redis 文档: LPOP</a>
     */
    V leftPop(K key);

    /**
     * 删除并返回存储在 {@code key} List中的第一个 {@code} 元素
     *
     * @param key   不得为 {@literal null}
     * @param count 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/lpop">Redis 文档: LPOP</a>
     */
    List<V> leftPop(K key, long count);

    /**
     * 从存储在 {@code key} 的List中删除并返回第一个元素。 <br>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @param unit    不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/blpop">Redis 文档: BLPOP</a>
     */
    V leftPop(K key, long timeout, TimeUnit unit);

    /**
     * 从存储在 {@code key} 的List中删除并返回第一个元素。 <br>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @throws IllegalArgumentException if the timeout is {@literal null} or negative.
     * @see <a href="https://redis.io/commands/blpop">Redis 文档: BLPOP</a>
     */
    default V leftPop(K key, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return leftPop(key, TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 删除并返回存储在 {@code key} List中的最后一个元素
     *
     * @param key 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/rpop">Redis 文档: RPOP</a>
     */
    V rightPop(K key);

    /**
     * 删除并返回存储在 {@code key} List中的最后一个 {@code} 元素
     *
     * @param key   不得为 {@literal null}
     * @param count 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/rpop">Redis 文档: RPOP</a>
     */
    List<V> rightPop(K key, long count);

    /**
     * 从存储在 {@code key} 的List中删除并返回最后一个元素。 <br>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}。
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @param unit    不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/brpop">Redis 文档: BRPOP</a>
     */
    V rightPop(K key, long timeout, TimeUnit unit);

    /**
     * 从存储在 {@code key} 的List中删除并返回最后一个元素。 <br>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/brpop">Redis 文档: BRPOP</a>
     */
    default V rightPop(K key, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return rightPop(key, TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 从 {@code sourceKey} 的List中删除最后一个元素，将其附加到 {@code destinationKey} 并返回其值
     *
     * @param sourceKey      不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/rpoplpush">Redis 文档: RPOPLPUSH</a>
     */
    V rightPopAndLeftPush(K sourceKey, K destinationKey);

    /**
     * 从 {@code srcKey} 处的List中删除最后一个元素，将其附加到 {@code dstKey} 并返回其值。<br>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}。
     *
     * @param sourceKey      不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param timeout        不得为 {@literal null}
     * @param unit           不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/brpoplpush">Redis 文档: BRPOPLPUSH</a>
     */
    V rightPopAndLeftPush(K sourceKey, K destinationKey, long timeout, TimeUnit unit);

    /**
     * 从 {@code srcKey} 的List中删除最后一个元素，将其附加到 {@code dstKey} 并返回其值。<br>
     * <b>阻止连接<b>直到元素可用或达到{@code timeout}
     *
     * @param sourceKey      不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param timeout        不得为 {@literal null}
     * @return 可以是 {@literal null}。
     * @throws IllegalArgumentException 如果超时为 {@literal null} 或负数
     * @see <a href="https://redis.io/commands/brpoplpush">Redis 文档: BRPOPLPUSH</a>
     */
    default V rightPopAndLeftPush(K sourceKey, K destinationKey, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return rightPopAndLeftPush(sourceKey, destinationKey, TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    RedisOperations<K, V> getOperations();
}
