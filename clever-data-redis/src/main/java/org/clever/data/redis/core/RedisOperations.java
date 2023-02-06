package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;
import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.core.query.SortQuery;
import org.clever.data.redis.core.script.RedisScript;
import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.data.redis.hash.HashMapper;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.util.Assert;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 指定Redis基本操作集的接口，由 {@link RedisTemplate} 实现。<br/>
 * 不经常使用，但对于可扩展性和可测试性是一个有用的选项(因为它很容易被模仿或存根)。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:14 <br/>
 */
public interface RedisOperations<K, V> {
    /**
     * 在 Redis 连接中执行给定的操作。<br/>
     * 只要有可能，操作对象抛出的应用程序异常就会传播给调用者（只能取消检查）。<br/>
     * Redis 异常被转换为适当的 DAO 异常。<br/>
     * 允许返回结果对象，即域对象或域对象的集合。<br/>
     * 对给定对象执行自动序列化/反序列化，将二进制数据转换为适合 Redis 存储的二进制数据。<br/>
     * 注意：回调代码不应该自己处理事务！使用适当的事务管理器。<br/>
     * 通常，回调代码不得触及任何 Connection 生命周期方法，如关闭，以让模板完成其工作。
     *
     * @param <T>    返回类型
     * @param action 指定 Redis 操作的回调对象。不得为 {@literal null}。
     * @return 操作返回的结果对象或 {@literal null}
     */
    <T> T execute(RedisCallback<T> action);

    /**
     * 执行 Redis 会话。
     * 允许在同一会话中执行多个操作，通过 {@link #multi()} 和 {@link #watch(Collection)} 操作启用“事务”功能。
     *
     * @param <T>     返回类型
     * @param session 会话回调。不得为 {@literal null}
     * @return 操作返回的结果对象或 {@literal null}
     */
    <T> T execute(SessionCallback<T> session);

    /**
     * 在管道连接上执行给定的操作对象，返回结果。
     * 请注意，回调<b>不能<b>返回一个非空值，因为它被管道覆盖。
     * 此方法将使用默认序列化程序反序列化结果
     *
     * @param action 要执行的回调对象
     * @return 管道返回的对象列表
     */
    List<Object> executePipelined(RedisCallback<?> action);

    /**
     * 在管道连接上执行给定的操作对象，使用专用序列化器返回结果。
     * 请注意，回调<b>不能<b>返回一个非空值，因为它被管道覆盖。
     *
     * @param action           要执行的回调对象
     * @param resultSerializer 用于单个值或值集合的序列化程序。如果任何返回值是散列值，则此序列化程序将用于反序列化键和值
     * @return 管道返回的对象列表
     */
    List<Object> executePipelined(final RedisCallback<?> action, final RedisSerializer<?> resultSerializer);

    /**
     * 在管道连接上执行给定的 Redis 会话。允许事务流水线化。
     * 请注意，回调<b>不能<b>返回一个非空值，因为它被管道覆盖。
     *
     * @param session 会话回调
     * @return 管道返回的对象列表
     */
    List<Object> executePipelined(final SessionCallback<?> session);

    /**
     * 在管道连接上执行给定的 Redis 会话，使用专用序列化器返回结果。
     * 允许事务流水线化。
     * 请注意，回调<b>不能<b>返回一个非空值，因为它被管道覆盖。
     *
     * @param session 会话回调
     * @return 管道返回的对象列表
     */
    List<Object> executePipelined(final SessionCallback<?> session, final RedisSerializer<?> resultSerializer);

    /**
     * 执行给定的 {@link RedisScript}
     *
     * @param script 要执行的脚本
     * @param keys   任何需要传递给脚本的键
     * @param args   任何需要传递给脚本的参数
     * @return 脚本的返回值，如果 {@link RedisScript#getResultType()} 为 null，则为 null，可能表示一次性状态回复（即“OK”）
     */
    <T> T execute(RedisScript<T> script, List<K> keys, Object... args);

    /**
     * 执行给定的 {@link RedisScript}，使用提供的 {@link RedisSerializer} 序列化脚本参数和结果
     *
     * @param script           要执行的脚本
     * @param argsSerializer   用于序列化 args 的 {@link RedisSerializer}
     * @param resultSerializer 用于序列化脚本返回值的 {@link RedisSerializer}
     * @param keys             任何需要传递给脚本的键
     * @param args             任何需要传递给脚本的参数
     * @return 脚本的返回值，如果 {@link RedisScript#getResultType()} 为 null，则为 null，可能表示一次性状态回复（即“OK”）
     */
    <T> T execute(RedisScript<T> script, RedisSerializer<?> argsSerializer, RedisSerializer<T> resultSerializer, List<K> keys, Object... args);

    /**
     * 分配一个新的 {@link RedisConnection} 并将其绑定到方法的实际返回类型。由调用者在使用后释放资源
     *
     * @param callback 不得为 {@literal null}
     */
    <T extends Closeable> T executeWithStickyConnection(RedisCallback<T> callback);

    // -------------------------------------------------------------------------
    // 处理Redis key的方法
    // -------------------------------------------------------------------------

    /**
     * 将给定的 {@code sourceKey} 复制到 {@code targetKey}
     *
     * @param sourceKey 不得为 {@literal null}
     * @param targetKey 不得为 {@literal null}
     * @param replace   key是否被复制。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/copy">Redis 文档: COPY</a>
     */
    Boolean copy(K sourceKey, K targetKey, boolean replace);

    /**
     * 确定给定的 {@code key} 是否存在
     *
     * @param key 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/exists">Redis 文档: EXISTS</a>
     */
    Boolean hasKey(K key);

    /**
     * 计算存在的 {@code keys} 的数量
     *
     * @param keys 不得为 {@literal null}
     * @return 指定为参数的键中存在的键数。多次提及和存在的键被计算多次
     * @see <a href="https://redis.io/commands/exists">Redis 文档: EXISTS</a>
     */
    Long countExistingKeys(Collection<K> keys);

    /**
     * 删除给定的{@code key}
     *
     * @param key 不得为 {@literal null}
     * @return {@literal true} if the key was removed.
     * @see <a href="https://redis.io/commands/del">Redis 文档: DEL</a>
     */
    Boolean delete(K key);

    /**
     * 删除给定的 {@code keys}
     *
     * @param keys 不得为 {@literal null}
     * @return 删除的key数。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/del">Redis 文档: DEL</a>
     */
    Long delete(Collection<K> keys);

    /**
     * 从键空间中取消链接 {@code key}。与 {@link #delete(Object)} 不同，这里的实际内存回收是异步发生的。
     *
     * @param key 不得为 {@literal null}
     * @return 删除的key数。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/unlink">Redis 文档: UNLINK</a>
     */
    Boolean unlink(K key);

    /**
     * 从键空间中取消链接 {@code keys}。与 {@link #delete(Collection)} 不同，这里的实际内存回收是异步发生的。
     *
     * @param keys 不得为 {@literal null}
     * @return 删除的key数。 {@literal null} 在管道事务中使用时。
     * @see <a href="https://redis.io/commands/unlink">Redis 文档: UNLINK</a>
     */
    Long unlink(Collection<K> keys);

    /**
     * 确定存储在 {@code key} 的类型
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/type">Redis 文档: TYPE</a>
     */
    DataType type(K key);

    /**
     * 查找与给定的 {@code pattern} 匹配的所有键
     *
     * @param pattern 不得为 {@literal null}
     * @return {@literal null} 在管道事务中使用时
     * @see <a href="https://redis.io/commands/keys">Redis 文档: KEYS</a>
     */
    Set<K> keys(K pattern);

    /**
     * 从键空间返回一个随机键
     *
     * @return {@literal null} 不存在键或在管道事务中使用时
     * @see <a href="https://redis.io/commands/randomkey">Redis 文档: RANDOMKEY</a>
     */
    K randomKey();

    /**
     * 将key {@code oldKey} 重命名为 {@code newKey}
     *
     * @param oldKey 不得为 {@literal null}
     * @param newKey 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/rename">Redis 文档: RENAME</a>
     */
    void rename(K oldKey, K newKey);

    /**
     * 仅当 {@code newKey} 不存在时，才将键 {@code oldKey} 重命名为 {@code newKey}
     *
     * @param oldKey 不得为 {@literal null}
     * @param newKey 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/renamenx">Redis 文档: RENAMENX</a>
     */
    Boolean renameIfAbsent(K oldKey, K newKey);

    /**
     * 为给定的 {@code key} 设置生存时间
     *
     * @param key     不得为 {@literal null}
     * @param timeout timeout
     * @param unit    不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Boolean expire(K key, long timeout, TimeUnit unit);

    /**
     * 为给定的 {@code key} 设置生存时间
     *
     * @param key     不得为 {@literal null}
     * @param timeout 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果超时为 {@literal null}
     */
    default Boolean expire(K key, Duration timeout) {
        Assert.notNull(timeout, "Timeout must not be null");
        return TimeoutUtils.hasMillis(timeout) ?
                expire(key, timeout.toMillis(), TimeUnit.MILLISECONDS) :
                expire(key, timeout.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 将给定 {@code key} 的到期时间设置为 {@literal date} 时间戳
     *
     * @param key  不得为 {@literal null}
     * @param date 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Boolean expireAt(K key, Date date);

    /**
     * 将给定 {@code key} 的到期时间设置为 {@literal date} 时间戳
     *
     * @param key      不得为 {@literal null}
     * @param expireAt 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @throws IllegalArgumentException 如果瞬间是 {@literal null} 或太大而无法表示为 {@code Date}
     */
    default Boolean expireAt(K key, Instant expireAt) {
        Assert.notNull(expireAt, "Timestamp must not be null");
        return expireAt(key, Date.from(expireAt));
    }

    /**
     * 删除给定 {@code key} 的过期时间
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/persist">Redis 文档: PERSIST</a>
     */
    Boolean persist(K key);

    /**
     * 使用 {@code index} 将给定的 {@code key} 移动到数据库
     *
     * @param key     不得为 {@literal null}
     * @param dbIndex dbIndex
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/move">Redis 文档: MOVE</a>
     */

    Boolean move(K key, int dbIndex);

    /**
     * 检索存储在 {@code key} 中的值的序列化版本
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/dump">Redis 文档: DUMP</a>
     */
    byte[] dump(K key);

    /**
     * 使用之前使用 {@link #dump(Object)} 获得的 {@code serializedValue} 创建 {@code key}
     *
     * @param key        不得为 {@literal null}
     * @param value      不得为 {@literal null}
     * @param timeToLive 生存时间
     * @param unit       不得为 {@literal null}
     * @see <a href="https://redis.io/commands/restore">Redis 文档: RESTORE</a>
     */
    default void restore(K key, byte[] value, long timeToLive, TimeUnit unit) {
        restore(key, value, timeToLive, unit, false);
    }

    /**
     * 使用之前使用 {@link #dump(Object)} 获得的 {@code serializedValue} 创建 {@code key}
     *
     * @param key        不得为 {@literal null}
     * @param value      不得为 {@literal null}
     * @param timeToLive 生存时间
     * @param unit       不得为 {@literal null}
     * @param replace    使用 {@literal true} 替换可能存在的值而不是错误
     * @see <a href="https://redis.io/commands/restore">Redis 文档: RESTORE</a>
     */
    void restore(K key, byte[] value, long timeToLive, TimeUnit unit, boolean replace);

    /**
     * 以秒为单位获取 {@code key} 的生存时间
     *
     * @param key 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/ttl">Redis 文档: TTL</a>
     */
    Long getExpire(K key);

    /**
     * 获取 {@code key} 的生存时间并将其转换为给定的 {@link TimeUnit}
     *
     * @param key      不得为 {@literal null}
     * @param timeUnit 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时。
     */
    Long getExpire(K key, TimeUnit timeUnit);

    /**
     * 对 {@code query} 的元素进行排序
     *
     * @param query 不得为 {@literal null}
     * @return 排序的结果。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sort">Redis 文档: SORT</a>
     */
    List<V> sort(SortQuery<K> query);

    /**
     * 应用 {@link RedisSerializer} 对 {@code query} 的元素进行排序
     *
     * @param query 不得为 {@literal null}
     * @return 排序的反序列化结果。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sort">Redis 文档: SORT</a>
     */
    <T> List<T> sort(SortQuery<K> query, RedisSerializer<T> resultSerializer);

    /**
     * 应用 {@link BulkMapper} 对 {@code query} 的元素进行排序
     *
     * @param query 不得为 {@literal null}
     * @return 排序的反序列化结果。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sort">Redis 文档: SORT</a>
     */
    <T> List<T> sort(SortQuery<K> query, BulkMapper<T, V> bulkMapper);

    /**
     * 应用 {@link BulkMapper} 和 {@link RedisSerializer} 对 {@code query} 的元素进行排序。
     *
     * @param query 不得为 {@literal null}
     * @return 排序的反序列化结果。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sort">Redis 文档: SORT</a>
     */
    <T, S> List<T> sort(SortQuery<K> query, BulkMapper<T, S> bulkMapper, RedisSerializer<S> resultSerializer);

    /**
     * 对 {@code query} 的元素进行排序并将结果存储在 {@code storeKey} 中
     *
     * @param query    不得为 {@literal null}
     * @param storeKey 不得为 {@literal null}
     * @return 值的数量。 {@literal null} 在管道/事务中使用时。
     * @see <a href="https://redis.io/commands/sort">Redis 文档: SORT</a>
     */
    Long sort(SortQuery<K> query, K storeKey);

    // -------------------------------------------------------------------------
    // 处理Redis事务的方法
    // -------------------------------------------------------------------------

    /**
     * 在以 {@link #multi()} 开始的事务期间观察给定的 {@code key} 的修改。
     *
     * @param key 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/watch">Redis 文档: WATCH</a>
     */
    void watch(K key);

    /**
     * 在以 {@link #multi()} 开始的事务期间观察给定的 {@code keys} 的修改
     *
     * @param keys 不得为 {@literal null}
     * @see <a href="https://redis.io/commands/watch">Redis 文档: WATCH</a>
     */
    void watch(Collection<K> keys);

    /**
     * 刷新所有以前的 {@link #watch(Object)} 键
     *
     * @see <a href="https://redis.io/commands/unwatch">Redis 文档: UNWATCH</a>
     */
    void unwatch();

    /**
     * 标记事务块的开始。 <br>
     * 命令将排队，然后可以通过调用 {@link #exec()} 执行或使用 {@link #discard()} 回滚
     *
     * @see <a href="https://redis.io/commands/multi">Redis 文档: MULTI</a>
     */
    void multi();

    /**
     * 丢弃在 {@link #multi()} 之后发出的所有命令
     *
     * @see <a href="https://redis.io/commands/discard">Redis 文档: DISCARD</a>
     */
    void discard();

    /**
     * 在以 {@link #multi()} 开始的事务中执行所有排队的命令。 <br>
     * 如果与 {@link #watch(Object)} 一起使用，如果任何被监视的键被修改，操作将失败。
     *
     * @return 每个已执行命令的回复列表
     * @see <a href="https://redis.io/commands/exec">Redis 文档: EXEC</a>
     */
    List<Object> exec();

    /**
     * 执行事务，使用提供的 {@link RedisSerializer} 反序列化 byte[]s 或 byte[]s 集合的任何结果。
     * 如果结果是 Map，则提供的 {@link RedisSerializer} 将同时用于键和值。
     * 其他结果类型（长整型、布尔型等）在转换后的结果中保持原样。
     * 元组结果自动转换为 TypedTuples。
     *
     * @param valueSerializer 用于反序列化事务执行结果的 {@link RedisSerializer}
     * @return 事务执行的反序列化结果
     */
    List<Object> exec(RedisSerializer<?> valueSerializer);

    // -------------------------------------------------------------------------
    // 处理Redis服务器命令的方法
    // -------------------------------------------------------------------------

    /**
     * 请求有关已连接客户端的信息和统计信息
     *
     * @return {@link RedisClientInfo} 对象的{@link List}
     */
    List<RedisClientInfo> getClientList();

    /**
     * 关闭由 {@code client} 中的 {@literal ip:port} 标识的给定客户端连接
     *
     * @param host host
     * @param port port
     */
    void killClient(String host, int port);

    /**
     * 更改 redis replication 设置为新的 master
     *
     * @param host 不能为{@literal null}
     * @param port 端口
     * @see <a href="https://redis.io/commands/slaveof">Redis 文档: SLAVEOF</a>
     */
    void slaveOf(String host, int port);

    /**
     * 更改服务器为 master 服务器
     *
     * @see <a href="https://redis.io/commands/slaveof">Redis 文档: SLAVEOF</a>
     */
    void slaveOfNoOne();

    /**
     * 将给定的消息发布到给定的通道
     *
     * @param destination 要发布到的通道不能是 {@literal null}
     * @param message     要发布的消息
     * @see <a href="https://redis.io/commands/publish">Redis 文档: PUBLISH</a>
     */
    void convertAndSend(String destination, Object message);

    // -------------------------------------------------------------------------
    // 获取特定操作接口对象的方法
    // -------------------------------------------------------------------------

    ClusterOperations<K, V> opsForCluster();

    GeoOperations<K, V> opsForGeo();

    BoundGeoOperations<K, V> boundGeoOps(K key);

    <HK, HV> HashOperations<K, HK, HV> opsForHash();

    <HK, HV> BoundHashOperations<K, HK, HV> boundHashOps(K key);

    HyperLogLogOperations<K, V> opsForHyperLogLog();

    ListOperations<K, V> opsForList();

    BoundListOperations<K, V> boundListOps(K key);

    SetOperations<K, V> opsForSet();

    BoundSetOperations<K, V> boundSetOps(K key);

    <HK, HV> StreamOperations<K, HK, HV> opsForStream();

    <HK, HV> StreamOperations<K, HK, HV> opsForStream(HashMapper<? super K, ? super HK, ? super HV> hashMapper);

    <HK, HV> BoundStreamOperations<K, HK, HV> boundStreamOps(K key);

    ValueOperations<K, V> opsForValue();

    BoundValueOperations<K, V> boundValueOps(K key);

    ZSetOperations<K, V> opsForZSet();

    BoundZSetOperations<K, V> boundZSetOps(K key);

    RedisSerializer<?> getKeySerializer();

    RedisSerializer<?> getValueSerializer();

    RedisSerializer<?> getHashKeySerializer();

    RedisSerializer<?> getHashValueSerializer();
}
