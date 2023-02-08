package org.clever.data.redis.core;

import org.clever.data.redis.connection.BitFieldSubCommands;
import org.clever.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 简单（或 Redis 术语“string”）值的 Redis 操作。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:42 <br/>
 */
public interface ValueOperations<K, V> {

    /**
     * 为 {@code key} 设置 {@code value}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    void set(K key, V value);

    /**
     * 为 {@code key} 设置 {@code value} 和过期时间 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @param timeout the key expiration timeout.
     * @param unit    不得为 {@literal null}
     * @see <a href="https://redis.io/commands/setex">Redis 文档: SETEX</a>
     */
    void set(K key, V value, long timeout, TimeUnit unit);

    /**
     * 为 {@code key} 设置 {@code value} 和过期时间 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @throws IllegalArgumentException 如果 {@code key}、{@code value} 或 {@code timeout} 不存在
     * @see <a href="https://redis.io/commands/setex">Redis 文档: SETEX</a>
     */
    default void set(K key, V value, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null!");
        if (TimeoutUtils.hasMillis(timeout)) {
            set(key, value, timeout.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            set(key, value, timeout.getSeconds(), TimeUnit.SECONDS);
        }
    }

    /**
     * 如果 {@code key} 不存在，则设置 {@code key} 以保存字符串 {@code value}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/setnx">Redis 文档: SETNX</a>
     */
    Boolean setIfAbsent(K key, V value);

    /**
     * 如果 {@code key} 不存在，则设置 {@code key} 以保存字符串 {@code value} 和过期 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @param timeout the key expiration timeout.
     * @param unit    不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean setIfAbsent(K key, V value, long timeout, TimeUnit unit);

    /**
     * 如果 {@code key} 不存在，则设置 {@code key} 以保存字符串 {@code value} 和过期 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果 {@code key}、{@code value} 或 {@code timeout} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    default Boolean setIfAbsent(K key, V value, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null!");
        if (TimeoutUtils.hasMillis(timeout)) {
            return setIfAbsent(key, value, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return setIfAbsent(key, value, timeout.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 如果存在 {@code key}，则设置 {@code key} 以保存字符串 {@code value}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return 命令结果指示是否已设置key
     * @throws IllegalArgumentException 如果 {@code key} 或 {@code value} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean setIfPresent(K key, V value);

    /**
     * 如果存在 {@code key}，则设置 {@code key} 以保存字符串 {@code value} 和过期 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @param timeout the key expiration timeout.
     * @param unit    不得为 {@literal null}
     * @return 命令结果指示是否已设置key
     * @throws IllegalArgumentException 如果 {@code key}、{@code value} 或 {@code timeout} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    Boolean setIfPresent(K key, V value, long timeout, TimeUnit unit);

    /**
     * 如果存在 {@code key}，则设置 {@code key} 以保存字符串 {@code value} 和过期 {@code timeout}
     *
     * @param key     不得为 {@literal null}
     * @param value   不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果 {@code key}、{@code value} 或 {@code timeout} 不存在
     * @see <a href="https://redis.io/commands/set">Redis 文档: SET</a>
     */
    default Boolean setIfPresent(K key, V value, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null!");
        if (TimeoutUtils.hasMillis(timeout)) {
            return setIfPresent(key, value, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return setIfPresent(key, value, timeout.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 使用 {@code tuple} 中提供的键值对将多个键设置为多个值
     *
     * @param map 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/mset">Redis 文档: MSET</a>
     */
    void multiSet(Map<? extends K, ? extends V> map);

    /**
     * 仅当提供的键不存在时，才使用 {@code tuple} 中提供的键值对将多个键设置为多个值
     *
     * @param map 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/msetnx">Redis 文档: MSETNX</a>
     */
    Boolean multiSetIfAbsent(Map<? extends K, ? extends V> map);

    /**
     * 获取 {@code key} 的值
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当key不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/get">Redis 文档: GET</a>
     */
    V get(Object key);

    /**
     * 返回 {@code key} 处的值并删除该键
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当key不存在或在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/getdel">Redis 文档: GETDEL</a>
     */
    V getAndDelete(K key);

    /**
     * 返回 {@code key} 处的值并通过应用 {@code timeout} 使key过期
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @param unit    不得为 {@literal null}
     * @return {@literal null} 当key不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getex">Redis 文档: GETEX</a>
     */

    V getAndExpire(K key, long timeout, TimeUnit unit);

    /**
     * 返回 {@code key} 处的值并通过应用 {@code timeout} 使key过期
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 当key不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getex">Redis 文档: GETEX</a>
     */
    V getAndExpire(K key, Duration timeout);

    /**
     * 返回 {@code key} 处的值并保留该键。此操作删除与 {@code key} 关联的任何 TTL
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当key不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getex">Redis 文档: GETEX</a>
     */
    V getAndPersist(K key);

    /**
     * 设置 {@code key} 的 {@code value} 并返回其旧值
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 当key不存在或在管道/事务中使用时
     * @see <a href="https://redis.io/commands/getset">Redis 文档: GETSET</a>
     */
    V getAndSet(K key, V value);

    /**
     * 获取多个{@code keys}。值按请求键的顺序排列。缺少的字段值在结果 {@link List} 中使用 {@code null} 表示
     *
     * @param keys 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/mget">Redis 文档: MGET</a>
     */
    List<V> multiGet(Collection<K> keys);

    /**
     * 将在 {@code key} 下存储为字符串值的整数值增加 1
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/incr">Redis 文档: INCR</a>
     */
    Long increment(K key);

    /**
     * 将存储为字符串值的整数值增加 {@code key} 下的 {@code delta}
     *
     * @param key   不得为 {@literal null}
     * @param delta 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/incrby">Redis 文档: INCRBY</a>
     */
    Long increment(K key, long delta);

    /**
     * 将存储为字符串值的浮点数值增加 {@code key} 下的 {@code delta}
     *
     * @param key   不得为 {@literal null}
     * @param delta 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/incrbyfloat">Redis 文档: INCRBYFLOAT</a>
     */
    Double increment(K key, double delta);

    /**
     * 将在 {@code key} 下存储为字符串值的整数值减一
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/decr">Redis 文档: DECR</a>
     */
    Long decrement(K key);

    /**
     * 通过 {@code delta} 减少存储在 {@code key} 下作为字符串值的整数值
     *
     * @param key   不得为 {@literal null}
     * @param delta 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/decrby">Redis 文档: DECRBY</a>
     */
    Long decrement(K key, long delta);

    /**
     * 将 {@code value} 附加到 {@code key}
     *
     * @param key   不得为 {@literal null}
     * @param value 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/append">Redis 文档: APPEND</a>
     */
    Integer append(K key, String value);

    /**
     * 获取 {@code begin} 和 {@code end} 之间的 {@code key} 值的子串
     *
     * @param key   不得为 {@literal null}
     * @param start 不得为 {@literal null}
     * @param end   不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/getrange">Redis 文档: GETRANGE</a>
     */
    String get(K key, long start, long end);

    /**
     * 用给定的 {@code value} 覆盖从指定的 {@code offset} 开始的 {@code key} 部分
     *
     * @param key    不得为 {@literal null}
     * @param value  不得为 {@literal null}
     * @param offset 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/setrange">Redis 文档: SETRANGE</a>
     */
    void set(K key, V value, long offset);

    /**
     * 获取存储在 {@code key} 中的值的长度
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/strlen">Redis 文档: STRLEN</a>
     */
    Long size(K key);

    /**
     * 将存储在 {@code key} 中的值设置为 {@code offset} 的位
     *
     * @param key    不得为 {@literal null}
     * @param offset 不得为 {@literal null}
     * @param value  不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/setbit">Redis 文档: SETBIT</a>
     */
    Boolean setBit(K key, long offset, boolean value);

    /**
     * 获取 {@code key} 值的 {@code offset} 位值
     *
     * @param key    不得为 {@literal null}
     * @param offset 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/getbit">Redis 文档: GETBIT</a>
     */
    Boolean getBit(K key, long offset);

    /**
     * 获取存储在给定 {@code key} 处的不同位宽和任意非（必要）对齐偏移量的操作特定整数字段
     *
     * @param key         不得为 {@literal null}
     * @param subCommands 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/bitfield">Redis 文档: BITFIELD</a>
     */
    List<Long> bitField(K key, BitFieldSubCommands subCommands);

    RedisOperations<K, V> getOperations();
}
