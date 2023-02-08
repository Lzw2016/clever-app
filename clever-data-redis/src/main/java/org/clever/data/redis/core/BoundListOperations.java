package org.clever.data.redis.core;

import org.clever.data.redis.connection.RedisListCommands.Direction;
import org.clever.util.Assert;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 列出绑定到某个key的操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:45 <br/>
 */
public interface BoundListOperations<K, V> extends BoundKeyOperations<K> {
    /**
     * 从绑定键的列表中获取 {@code begin} 和 {@code end} 之间的元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lrange">Redis 文档: LRANGE</a>
     */
    List<V> range(long start, long end);

    /**
     * 在 {@code start} 和 {@code end} 之间的元素的绑定键处修剪列表
     *
     * @see <a href="https://redis.io/commands/ltrim">Redis 文档: LTRIM</a>
     */
    void trim(long start, long end);

    /**
     * 获取存储在绑定键中的列表的大小
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/llen">Redis 文档: LLEN</a>
     */
    Long size();

    /**
     * 将 {@code value} 添加到绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: LPUSH</a>
     */
    Long leftPush(V value);

    /**
     * 将 {@code values} 添加到绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: LPUSH</a>
     */
    @SuppressWarnings({"unchecked"})
    Long leftPushAll(V... values);

    /**
     * 仅当列表存在时才将 {@code values} 添加到绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpushx">Redis 文档: LPUSHX</a>
     */
    Long leftPushIfPresent(V value);

    /**
     * 在 {@code value} 之前将 {@code values} 添加到绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: LPUSH</a>
     */
    Long leftPush(V pivot, V value);

    /**
     * 将 {@code value} 附加到绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpush">Redis 文档: RPUSH</a>
     */
    Long rightPush(V value);

    /**
     * 将 {@code values} 附加到绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpush">Redis 文档: RPUSH</a>
     */
    @SuppressWarnings({"unchecked"})
    Long rightPushAll(V... values);

    /**
     * 仅当列表存在时才将 {@code values} 附加到绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpushx">Redis 文档: RPUSHX</a>
     */
    Long rightPushIfPresent(V value);

    /**
     * 将 {@code values} 附加到 {@code value} 之前的绑定键
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpush">Redis 文档: RPUSH</a>
     */
    Long rightPush(V pivot, V value);

    /**
     * 以原子方式返回并删除存储在绑定键中的列表的第一个最后一个元素（head/tail 取决于 {@code from} 参数），
     * 并将该元素压入存储在 {@code destinationKey} 的列表。
     *
     * @param from           不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param to             不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lmove">Redis 文档: LMOVE</a>
     */
    V move(Direction from, K destinationKey, Direction to);

    /**
     * 以原子方式返回并删除存储在绑定键中的列表的第一个最后一个元素（head/tail 取决于 {@code from} 参数），并将该元素压入存储在 {@code destinationKey} 的列表。
     * <p>
     * <b>阻止连接</b>直到元素可用或达到{@code timeout}
     *
     * @param from           不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param to             不得为 {@literal null}
     * @param timeout        不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/blmove">Redis 文档: BLMOVE</a>
     */
    V move(Direction from, K destinationKey, Direction to, Duration timeout);

    /**
     * 以原子方式返回并删除存储在绑定键中的列表的第一个最后一个元素（head/tail 取决于 {@code from} 参数），并将该元素压入存储在 {@code destinationKey} 的列表。
     * <p>
     * <b>阻止连接</b>直到元素可用或达到{@code timeout}
     *
     * @param from           不得为 {@literal null}
     * @param destinationKey 不得为 {@literal null}
     * @param to             不得为 {@literal null}
     * @param timeout        不得为 {@literal null}
     * @param unit           不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/blmove">Redis 文档: BLMOVE</a>
     */
    V move(Direction from, K destinationKey, Direction to, long timeout, TimeUnit unit);

    /**
     * 在 {@code index} 设置 {@code value} 列表元素
     *
     * @see <a href="https://redis.io/commands/lset">Redis 文档: LSET</a>
     */
    void set(long index, V value);

    /**
     * 从存储在绑定键的列表中删除 {@code value} 的前 {@code count} 次出现
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lrem">Redis 文档: LREM</a>
     */
    Long remove(long count, Object value);

    /**
     * 在绑定键的 {@code index} 表单列表中获取元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lindex">Redis 文档: LINDEX</a>
     */
    V index(long index);

    /**
     * 返回指定值在列表中第一次出现的索引，位于 {@code key}。 <br />
     * 需要 Redis 6.0.6 或更新版本。
     *
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用或未包含在列表中时
     * @see <a href="https://redis.io/commands/lpos">Redis 文档: LPOS</a>
     */
    Long indexOf(V value);

    /**
     * 返回指定值在列表中最后一次出现的索引，位于 {@code key} 。<br />
     * 需要 Redis 6.0.6 或更新版本。
     *
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用或未包含在列表中时
     * @see <a href="https://redis.io/commands/lpos">Redis 文档: LPOS</a>
     */
    Long lastIndexOf(V value);

    /**
     * 删除并返回存储在绑定键处的列表中的第一个元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/lpop">Redis 文档: LPOP</a>
     */
    V leftPop();

    /**
     * 删除并返回存储在 {@code key} 列表中的第一个 {@code} 元素
     *
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/lpop">Redis 文档: LPOP</a>
     */
    List<V> leftPop(long count);

    /**
     * 从存储在绑定键的列表中移除并返回第一个元素。<br>
     * <b>阻止连接</b>直到元素可用或达到{@code timeout}。
     *
     * @param timeout 不得为 {@literal null}
     * @param unit    不得为 {@literal null}
     * @return {@literal null} 当达到超时或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/blpop">Redis 文档: BLPOP</a>
     */
    V leftPop(long timeout, TimeUnit unit);

    /**
     * 从存储在绑定键的列表中移除并返回第一个元素。 <br>
     * <b>阻止连接</b>直到元素可用或达到{@code timeout}。
     *
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 当达到超时或在管道/事务中使用时
     * @throws IllegalArgumentException 如果超时为 {@literal null} 或负数
     * @see <a href="https://redis.io/commands/blpop">Redis 文档: BLPOP</a>
     */
    default V leftPop(Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return leftPop(TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 删除并返回存储在绑定键处的列表中的最后一个元素
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/rpop">Redis 文档: RPOP</a>
     */
    V rightPop();

    /**
     * 删除并返回存储在 {@code key} 列表中的最后一个 {@code} 元素
     *
     * @return 可以是 {@literal null}。
     * @see <a href="https://redis.io/commands/rpop">Redis 文档: RPOP</a>
     */
    List<V> rightPop(long count);

    /**
     * 从存储在绑定键的列表中删除并返回最后一个元素。 <br>
     * <b>阻止连接</b>直到元素可用或达到{@code timeout}
     *
     * @param timeout 不得为 {@literal null}
     * @param unit    不得为 {@literal null}
     * @return {@literal null} 当达到超时或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/brpop">Redis 文档: BRPOP</a>
     */
    V rightPop(long timeout, TimeUnit unit);

    /**
     * 从存储在绑定键的列表中删除并返回最后一个元素。 <br>
     * <b>阻止连接</b>直到元素可用或达到{@code timeout}
     *
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 当达到超时或在管道/事务中使用时
     * @throws IllegalArgumentException 如果超时为 {@literal null} 或负数
     * @see <a href="https://redis.io/commands/brpop">Redis 文档: BRPOP</a>
     */
    default V rightPop(Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        return rightPop(TimeoutUtils.toSeconds(timeout), TimeUnit.SECONDS);
    }

    RedisOperations<K, V> getOperations();
}
