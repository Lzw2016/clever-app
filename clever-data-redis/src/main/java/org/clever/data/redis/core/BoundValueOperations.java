package org.clever.data.redis.core;

import org.clever.util.Assert;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 绑定到某个键的值（或Redis术语中的字符串）操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:43 <br/>
 */
public interface BoundValueOperations<K, V> extends BoundKeyOperations<K> {
    /**
     * 为绑定键设置 {@code value}
     *
     * @param value 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    void set(V value);

    /**
     * 为绑定键设置 {@code value} 和到期时间 {@code timeout}
     *
     * @param value 不得为 {@literal null}
     * @param unit  不得为 {@literal null}
     * @see <a href="https://redis.io/commands/setex">Redis 文档: SETEX</a>
     */
    void set(V value, long timeout, TimeUnit unit);

    /**
     * 为绑定键设置 {@code value} 和到期时间 {@code timeout}
     *
     * @param value   不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @throws IllegalArgumentException if either {@code value} or {@code timeout} is not present.
     * @see <a href="https://redis.io/commands/setex">Redis 文档: SETEX</a>
     */
    default void set(V value, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null!");
        if (TimeoutUtils.hasMillis(timeout)) {
            set(value, timeout.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            set(value, timeout.getSeconds(), TimeUnit.SECONDS);
        }
    }

    /**
     * 如果绑定键不存在，则设置绑定键以保存字符串 {@code value}
     *
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/setnx">Redis 文档: SETNX</a>
     */
    Boolean setIfAbsent(V value);

    /**
     * 如果绑定键不存在，则设置绑定键以保存字符串 {@code value} 和到期时间 {@code timeout}
     *
     * @param value 不得为 {@literal null}
     * @param unit  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean setIfAbsent(V value, long timeout, TimeUnit unit);

    /**
     * 如果绑定键不存在，则设置绑定键以保存字符串 {@code value} 和到期时间 {@code timeout}
     *
     * @param value   不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果 {@code value} 或 {@code timeout} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    default Boolean setIfAbsent(V value, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null!");
        if (TimeoutUtils.hasMillis(timeout)) {
            return setIfAbsent(value, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return setIfAbsent(value, timeout.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 如果存在绑定键，则将绑定键设置为保存字符串 {@code value}
     *
     * @param value 不得为 {@literal null}
     * @return 命令结果指示是否已设置密钥
     * @throws IllegalArgumentException 如果 {@code value} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean setIfPresent(V value);

    /**
     * 如果存在绑定键，则设置绑定键以保存字符串 {@code value} 和到期时间 {@code timeout}
     *
     * @param value   不得为 {@literal null}
     * @param timeout 密钥过期超时
     * @param unit    不得为 {@literal null}
     * @return 命令结果指示是否已设置密钥
     * @throws IllegalArgumentException 如果 {@code value} 或 {@code timeout} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean setIfPresent(V value, long timeout, TimeUnit unit);

    /**
     * 如果存在绑定键，则设置绑定键以保存字符串 {@code value} 和到期时间 {@code timeout}
     *
     * @param value   不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果 {@code value} 或 {@code timeout} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    default Boolean setIfPresent(V value, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null!");
        if (TimeoutUtils.hasMillis(timeout)) {
            return setIfPresent(value, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return setIfPresent(value, timeout.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 获取绑定键的值
     *
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/get">Redis 文档: GET</a>
     */
    V get();

    /**
     * 返回绑定键处的值并删除该键
     *
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getdel">Redis 文档: GETDEL</a>
     */
    V getAndDelete();

    /**
     * 返回绑定键的值并通过应用 {@code timeout} 使键过期
     *
     * @param unit 不得为 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getex">Redis 文档: GETEX</a>
     */
    V getAndExpire(long timeout, TimeUnit unit);

    /**
     * 返回绑定键的值并通过应用 {@code timeout} 使键过期
     *
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getex">Redis 文档: GETEX</a>
     */
    V getAndExpire(Duration timeout);

    /**
     * 返回绑定键的值并保留该键。此操作删除与绑定密钥关联的任何 TTL
     *
     * @return {@literal null} 当密钥不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getex">Redis 文档: GETEX</a>
     */
    V getAndPersist();

    /**
     * 设置绑定键的 {@code value} 并返回其旧值
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/getset">Redis 文档: GETSET</a>
     */
    V getAndSet(V value);

    /**
     * 将绑定键下存储为字符串值的整数值增加 1
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/incr">Redis 文档: INCR</a>
     */
    Long increment();

    /**
     * 将绑定键下存储为字符串值的整数值增加 {@code delta}
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/incrby">Redis 文档: INCRBY</a>
     */
    Long increment(long delta);

    /**
     * 将绑定键下存储为字符串值的浮点数值增加 {@code delta}
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/incrbyfloat">Redis 文档: INCRBYFLOAT</a>
     */
    Double increment(double delta);

    /**
     * 将绑定键下存储为字符串值的整数值减一
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/decr">Redis 文档: DECR</a>
     */
    Long decrement();

    /**
     * 通过 {@code delta} 减少绑定键下存储为字符串值的整数值
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/decrby">Redis 文档: DECRBY</a>
     */
    Long decrement(long delta);

    /**
     * 将 {@code value} 附加到绑定键
     *
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/append">Redis 文档: APPEND</a>
     */
    Integer append(String value);

    /**
     * 获取 {@code begin} 和 {@code end} 之间绑定键值的子串
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/getrange">Redis 文档: GETRANGE</a>
     */
    String get(long start, long end);

    /**
     * 用给定的 {@code value} 覆盖从指定的 {@code offset} 开始的部分绑定键
     *
     * @param value 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/setrange">Redis 文档: SETRANGE</a>
     */
    void set(V value, long offset);

    /**
     * 获取存储在绑定键中的值的长度
     *
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/strlen">Redis 文档: STRLEN</a>
     */
    Long size();

    /**
     * @return 从不为 {@literal null}
     */
    RedisOperations<K, V> getOperations();
}
