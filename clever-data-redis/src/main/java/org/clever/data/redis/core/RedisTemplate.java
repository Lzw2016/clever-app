package org.clever.data.redis.core;

import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.data.redis.RedisSystemException;
import org.clever.data.redis.connection.*;
import org.clever.data.redis.connection.RedisZSetCommands.Tuple;
import org.clever.data.redis.core.ZSetOperations.TypedTuple;
import org.clever.data.redis.core.query.QueryUtils;
import org.clever.data.redis.core.query.SortQuery;
import org.clever.data.redis.core.script.DefaultScriptExecutor;
import org.clever.data.redis.core.script.RedisScript;
import org.clever.data.redis.core.script.ScriptExecutor;
import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.data.redis.hash.HashMapper;
import org.clever.data.redis.serializer.JdkSerializationRedisSerializer;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.data.redis.serializer.SerializationUtils;
import org.clever.data.redis.serializer.StringRedisSerializer;
import org.clever.transaction.support.TransactionSynchronizationManager;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.CollectionUtils;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 简化 Redis 数据访问代码的帮助程序类。
 * <p>
 * 在给定对象和 Redis 存储中的底层二进制数据之间执行自动序列化反序列化。<br />
 * 默认情况下，它对其对象使用 Java 序列化（通过 {@link JdkSerializationRedisSerializer}）。
 * <p>
 * 核心方法是execute，支持实现{@link RedisCallback}接口的Redis访问代码。<br />
 * 它提供 {@link RedisConnection} 处理，这样 {@link RedisCallback} 实现和调用代码都不需要明确关心检索关闭 Redis 连接或处理连接生命周期异常。
 * <p>
 * 配置后，此类是线程安全的。
 * <p>
 * 请注意，虽然模板是通用的，但序列化器反序列化器将给定的对象与二进制数据进行正确的转换。
 * <p>
 * <b>这是 Redis 支持的中心类</b>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:12 <br/>
 *
 * @param <K> 模板工作所针对的 Redis 键类型（通常是字符串）
 * @param <V> 模板适用的 Redis 值类型
 */
public class RedisTemplate<K, V> extends RedisAccessor implements RedisOperations<K, V> {
    private boolean initialized = false;
    private boolean enableTransactionSupport = false;
    private boolean exposeConnection = false;
    private boolean enableDefaultSerializer = true;
    private ClassLoader classLoader;
    private RedisSerializer<?> defaultSerializer;
    private RedisSerializer<String> stringSerializer = RedisSerializer.string();

    @SuppressWarnings("rawtypes")
    private RedisSerializer keySerializer = null;
    @SuppressWarnings("rawtypes")
    private RedisSerializer valueSerializer = null;
    @SuppressWarnings("rawtypes")
    private RedisSerializer hashKeySerializer = null;
    @SuppressWarnings("rawtypes")
    private RedisSerializer hashValueSerializer = null;

    private ScriptExecutor<K> scriptExecutor;
    private final ValueOperations<K, V> valueOps = new DefaultValueOperations<>(this);
    private final ListOperations<K, V> listOps = new DefaultListOperations<>(this);
    private final SetOperations<K, V> setOps = new DefaultSetOperations<>(this);
    private final ZSetOperations<K, V> zSetOps = new DefaultZSetOperations<>(this);
    private final HyperLogLogOperations<K, V> hllOps = new DefaultHyperLogLogOperations<>(this);
    private final GeoOperations<K, V> geoOps = new DefaultGeoOperations<>(this);
    private final StreamOperations<K, ?, ?> streamOps = new DefaultStreamOperations<>(this,
            null
            // TODO     ObjectHashMapper.getSharedInstance()
    );
    private final ClusterOperations<K, V> clusterOps = new DefaultClusterOperations<>(this);

    /**
     * 构造一个新的 <code>RedisTemplate<code> 实例
     */
    public RedisTemplate() {
    }

    // --------------------------------------------------------------------------------------------
    // RedisTemplate 配置
    // --------------------------------------------------------------------------------------------

    /**
     * 如果设置为 {@code true} {@link RedisTemplate} 将使用 {@literal MULTI...EXEC|DISCARD} 参与正在进行的事务，以跟踪操作
     *
     * @param enableTransactionSupport 是否参与正在进行的交易
     * @see RedisConnectionUtils#getConnection(RedisConnectionFactory, boolean)
     * @see TransactionSynchronizationManager#isActualTransactionActive()
     */
    public void setEnableTransactionSupport(boolean enableTransactionSupport) {
        this.enableTransactionSupport = enableTransactionSupport;
    }

    /**
     * 设置是否将 Redis 连接暴露给 {@link RedisCallback} 代码。
     * 默认为“false”：将返回代理，抑制 {@code quit} 和 {@code disconnect} 调用。
     */
    public void setExposeConnection(boolean exposeConnection) {
        this.exposeConnection = exposeConnection;
    }

    /**
     * 返回是否将本机 Redis 连接公开给 RedisCallback 代码，或者更确切地说是连接代理（默认）
     *
     * @return 是否暴露原生Redis连接
     */
    public boolean isExposeConnection() {
        return exposeConnection;
    }

    /**
     * @param enableDefaultSerializer 是否应使用默认序列化程序。否则，任何未显式设置的序列化程序都将保持为 null，并且不会对值进行序列化或反序列化
     */
    public void setEnableDefaultSerializer(boolean enableDefaultSerializer) {
        this.enableDefaultSerializer = enableDefaultSerializer;
    }

    /**
     * @return 是否应使用默认序列化程序。否则，任何未显式设置的序列化程序都将保持为 null，并且不会对值进行序列化或反序列化。
     */
    public boolean isEnableDefaultSerializer() {
        return enableDefaultSerializer;
    }

    /**
     * 将 {@link ClassLoader} 设置为用于默认 {@link JdkSerializationRedisSerializer}，
     * 以防没有其他 {@link RedisSerializer} 被明确设置为默认值。
     *
     * @param classLoader 可以是 {@literal null}
     */
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 设置用于此模板的默认序列化程序。
     * 除非明确设置，否则所有序列化程序（{@link #setStringSerializer(RedisSerializer)} 除外）都初始化为此值。
     * 默认为 {@link JdkSerializationRedisSerializer}。
     *
     * @param serializer 使用的默认序列化程序
     */
    public void setDefaultSerializer(RedisSerializer<?> serializer) {
        this.defaultSerializer = serializer;
    }

    /**
     * 返回此模板使用的默认序列化程序
     *
     * @return 模板默认序列化器
     */
    public RedisSerializer<?> getDefaultSerializer() {
        return defaultSerializer;
    }

    /**
     * 设置此模板要使用的字符串值序列化程序（当参数或返回类型始终为字符串时）。默认为 {@link StringRedisSerializer}
     *
     * @param stringSerializer 要设置的 stringValueSerializer
     * @see ValueOperations#get(Object, long, long)
     */
    public void setStringSerializer(RedisSerializer<String> stringSerializer) {
        this.stringSerializer = stringSerializer;
    }

    /**
     * 返回 stringSerializer
     *
     * @return 返回 stringSerializer
     */
    public RedisSerializer<String> getStringSerializer() {
        return stringSerializer;
    }

    /**
     * 设置此模板要使用的密钥序列化程序。默认为 {@link #getDefaultSerializer()}。
     *
     * @param serializer 此模板要使用的密钥序列化程序
     */
    public void setKeySerializer(RedisSerializer<?> serializer) {
        this.keySerializer = serializer;
    }

    /**
     * 返回此模板使用的密钥序列化程序
     *
     * @return 此模板使用的密钥序列化程序
     */
    @Override
    public RedisSerializer<?> getKeySerializer() {
        return keySerializer;
    }

    /**
     * 设置此模板要使用的值序列化程序。默认为 {@link #getDefaultSerializer()}
     *
     * @param serializer 此模板要使用的值序列化程序
     */
    public void setValueSerializer(RedisSerializer<?> serializer) {
        this.valueSerializer = serializer;
    }

    /**
     * 返回此模板使用的值序列化程序
     *
     * @return 此模板使用的值序列化程序
     */
    @Override
    public RedisSerializer<?> getValueSerializer() {
        return valueSerializer;
    }

    /**
     * 设置此模板要使用的哈希键（或字段）序列化程序。默认为 {@link #getDefaultSerializer()}
     *
     * @param hashKeySerializer 要设置的 hashKeySerializer
     */
    public void setHashKeySerializer(RedisSerializer<?> hashKeySerializer) {
        this.hashKeySerializer = hashKeySerializer;
    }

    /**
     * 返回 hashKeySerializer
     *
     * @return 返回 hashKeySerializer
     */
    @Override
    public RedisSerializer<?> getHashKeySerializer() {
        return hashKeySerializer;
    }

    /**
     * 设置此模板要使用的哈希值序列化程序。默认为 {@link #getDefaultSerializer()}
     *
     * @param hashValueSerializer 要设置的 hashValueSerializer
     */
    public void setHashValueSerializer(RedisSerializer<?> hashValueSerializer) {
        this.hashValueSerializer = hashValueSerializer;
    }

    /**
     * 返回 hashValueSerializer
     *
     * @return 返回 hashValueSerializer
     */
    @Override
    public RedisSerializer<?> getHashValueSerializer() {
        return hashValueSerializer;
    }

    /**
     * @param scriptExecutor 用于执行 Redis 脚本的 {@link ScriptExecutor}
     */
    public void setScriptExecutor(ScriptExecutor<K> scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    /**
     * 初始化 RedisTemplate
     */
    @Override
    public void afterPropertiesSet() {
        boolean defaultUsed = false;
        if (defaultSerializer == null) {
            defaultSerializer = new JdkSerializationRedisSerializer(
                    classLoader != null ? classLoader : this.getClass().getClassLoader()
            );
        }
        if (enableDefaultSerializer) {
            if (keySerializer == null) {
                keySerializer = defaultSerializer;
                defaultUsed = true;
            }
            if (valueSerializer == null) {
                valueSerializer = defaultSerializer;
                defaultUsed = true;
            }
            if (hashKeySerializer == null) {
                hashKeySerializer = defaultSerializer;
                defaultUsed = true;
            }
            if (hashValueSerializer == null) {
                hashValueSerializer = defaultSerializer;
                defaultUsed = true;
            }
        }
        if (enableDefaultSerializer && defaultUsed) {
            Assert.notNull(defaultSerializer, "default serializer null and not all serializers initialized");
        }
        if (scriptExecutor == null) {
            this.scriptExecutor = new DefaultScriptExecutor<>(this);
        }
        initialized = true;
    }

    // --------------------------------------------------------------------------------------------
    // Redis execute
    // --------------------------------------------------------------------------------------------

    @Override
    public <T> T execute(RedisCallback<T> action) {
        return execute(action, isExposeConnection());
    }

    /**
     * 在连接中执行给定的操作对象，可以公开也可以不公开
     *
     * @param <T>              返回类型
     * @param action           指定 Redis 操作的回调对象
     * @param exposeConnection 是否强制将本机 Redis 连接暴露给回调代码
     * @return 动作返回的对象
     */
    public <T> T execute(RedisCallback<T> action, boolean exposeConnection) {
        return execute(action, exposeConnection, false);
    }

    /**
     * 在可以公开或不公开的连接中执行给定的操作对象。此外，连接可以是流水线的。请注意管道的结果被丢弃（使其适用于只写场景）。
     *
     * @param <T>              返回类型
     * @param action           要执行的回调对象
     * @param exposeConnection 是否强制将本机 Redis 连接暴露给回调代码
     * @param pipeline         是否流水线连接执行
     * @return 动作返回的对象
     */
    public <T> T execute(RedisCallback<T> action, boolean exposeConnection, boolean pipeline) {
        Assert.isTrue(initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(action, "Callback object must not be null");
        RedisConnectionFactory factory = getRequiredConnectionFactory();
        RedisConnection conn = RedisConnectionUtils.getConnection(factory, enableTransactionSupport);
        try {
            boolean existingConnection = TransactionSynchronizationManager.hasResource(factory);
            RedisConnection connToUse = preProcessConnection(conn, existingConnection);
            boolean pipelineStatus = connToUse.isPipelined();
            if (pipeline && !pipelineStatus) {
                connToUse.openPipeline();
            }
            RedisConnection connToExpose = (exposeConnection ? connToUse : createRedisConnectionProxy(connToUse));
            T result = action.doInRedis(connToExpose);
            // close pipeline
            if (pipeline && !pipelineStatus) {
                connToUse.closePipeline();
            }
            return postProcessResult(result, connToUse, existingConnection);
        } finally {
            RedisConnectionUtils.releaseConnection(conn, factory);
        }
    }

    @Override
    public <T> T execute(SessionCallback<T> session) {
        Assert.isTrue(initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(session, "Callback object must not be null");
        RedisConnectionFactory factory = getRequiredConnectionFactory();
        // noinspection resource | bind connection
        RedisConnectionUtils.bindConnection(factory, enableTransactionSupport);
        try {
            return session.execute(this);
        } finally {
            RedisConnectionUtils.unbindConnection(factory);
        }
    }

    @Override
    public List<Object> executePipelined(SessionCallback<?> session) {
        return executePipelined(session, valueSerializer);
    }

    @Override
    public List<Object> executePipelined(SessionCallback<?> session, RedisSerializer<?> resultSerializer) {
        Assert.isTrue(initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(session, "Callback object must not be null");
        RedisConnectionFactory factory = getRequiredConnectionFactory();
        // noinspection resource | bind connection
        RedisConnectionUtils.bindConnection(factory, enableTransactionSupport);
        try {
            return execute((RedisCallback<List<Object>>) connection -> {
                connection.openPipeline();
                boolean pipelinedClosed = false;
                try {
                    Object result = executeSession(session);
                    if (result != null) {
                        throw new InvalidDataAccessApiUsageException(
                                "Callback cannot return a non-null value as it gets overwritten by the pipeline"
                        );
                    }
                    List<Object> closePipeline = connection.closePipeline();
                    pipelinedClosed = true;
                    return deserializeMixedResults(closePipeline, resultSerializer, hashKeySerializer, hashValueSerializer);
                } finally {
                    if (!pipelinedClosed) {
                        connection.closePipeline();
                    }
                }
            });
        } finally {
            RedisConnectionUtils.unbindConnection(factory);
        }
    }

    @Override
    public List<Object> executePipelined(RedisCallback<?> action) {
        return executePipelined(action, valueSerializer);
    }

    @Override
    public List<Object> executePipelined(RedisCallback<?> action, RedisSerializer<?> resultSerializer) {
        return execute((RedisCallback<List<Object>>) connection -> {
            connection.openPipeline();
            boolean pipelinedClosed = false;
            try {
                Object result = action.doInRedis(connection);
                if (result != null) {
                    throw new InvalidDataAccessApiUsageException(
                            "Callback cannot return a non-null value as it gets overwritten by the pipeline"
                    );
                }
                List<Object> closePipeline = connection.closePipeline();
                pipelinedClosed = true;
                return deserializeMixedResults(closePipeline, resultSerializer, hashKeySerializer, hashValueSerializer);
            } finally {
                if (!pipelinedClosed) {
                    connection.closePipeline();
                }
            }
        });
    }

    @Override
    public <T> T execute(RedisScript<T> script, List<K> keys, Object... args) {
        return scriptExecutor.execute(script, keys, args);
    }

    @Override
    public <T> T execute(RedisScript<T> script, RedisSerializer<?> argsSerializer, RedisSerializer<T> resultSerializer, List<K> keys, Object... args) {
        return scriptExecutor.execute(script, argsSerializer, resultSerializer, keys, args);
    }

    @Override
    public <T extends Closeable> T executeWithStickyConnection(RedisCallback<T> callback) {
        Assert.isTrue(initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(callback, "Callback object must not be null");
        RedisConnectionFactory factory = getRequiredConnectionFactory();
        RedisConnection connection = preProcessConnection(
                RedisConnectionUtils.doGetConnection(factory, true, false, false),
                false
        );
        return callback.doInRedis(connection);
    }

    // --------------------------------------------------------------------------------------------
    // 私有方法
    // --------------------------------------------------------------------------------------------

    private Object executeSession(SessionCallback<?> session) {
        return session.execute(this);
    }

    protected RedisConnection createRedisConnectionProxy(RedisConnection connection) {
        Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(connection.getClass(), getClass().getClassLoader());
        return (RedisConnection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                ifcs,
                new CloseSuppressingInvocationHandler(connection)
        );
    }

    /**
     * 处理连接（在对其执行任何设置之前）。默认实现按原样返回连接。
     *
     * @param connection redis 连接
     */
    protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
        return connection;
    }

    protected <T> T postProcessResult(T result, RedisConnection conn, boolean existingConnection) {
        return result;
    }

    @SuppressWarnings("unchecked")
    private byte[] rawKey(Object key) {
        Assert.notNull(key, "non null key required");
        if (keySerializer == null && key instanceof byte[]) {
            return (byte[]) key;
        }
        assert keySerializer != null;
        return keySerializer.serialize(key);
    }

    private byte[] rawString(String key) {
        return stringSerializer.serialize(key);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawValue(Object value) {
        if (valueSerializer == null && value instanceof byte[]) {
            return (byte[]) value;
        }
        assert valueSerializer != null;
        return valueSerializer.serialize(value);
    }

    private byte[][] rawKeys(Collection<K> keys) {
        final byte[][] rawKeys = new byte[keys.size()][];
        int i = 0;
        for (K key : keys) {
            rawKeys[i++] = rawKey(key);
        }
        return rawKeys;
    }

    @SuppressWarnings("unchecked")
    private K deserializeKey(byte[] value) {
        return keySerializer != null ? (K) keySerializer.deserialize(value) : (K) value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Object> deserializeMixedResults(List<Object> rawValues,
                                                 RedisSerializer valueSerializer,
                                                 RedisSerializer hashKeySerializer,
                                                 RedisSerializer hashValueSerializer) {
        if (rawValues == null) {
            return null;
        }
        List<Object> values = new ArrayList<>();
        for (Object rawValue : rawValues) {
            if (rawValue instanceof byte[] && valueSerializer != null) {
                values.add(valueSerializer.deserialize((byte[]) rawValue));
            } else if (rawValue instanceof List) {
                // 列表是唯一可能的混合值集合...
                values.add(deserializeMixedResults((List) rawValue, valueSerializer, hashKeySerializer, hashValueSerializer));
            } else if (rawValue instanceof Set && !(((Set) rawValue).isEmpty())) {
                values.add(deserializeSet((Set) rawValue, valueSerializer));
            } else if (rawValue instanceof Map && !(((Map) rawValue).isEmpty())
                    && ((Map) rawValue).values().iterator().next() instanceof byte[]) {
                values.add(SerializationUtils.deserialize((Map) rawValue, hashKeySerializer, hashValueSerializer));
            } else {
                values.add(rawValue);
            }
        }
        return values;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Set<?> deserializeSet(Set rawSet, RedisSerializer valueSerializer) {
        if (rawSet.isEmpty()) {
            return rawSet;
        }
        Object setValue = rawSet.iterator().next();
        if (setValue instanceof byte[] && valueSerializer != null) {
            return (SerializationUtils.deserialize(rawSet, valueSerializer));
        } else if (setValue instanceof Tuple) {
            return convertTupleValues(rawSet, valueSerializer);
        } else {
            return rawSet;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<TypedTuple<V>> convertTupleValues(Set<Tuple> rawValues, RedisSerializer valueSerializer) {
        Set<TypedTuple<V>> set = new LinkedHashSet<>(rawValues.size());
        for (Tuple rawValue : rawValues) {
            Object value = rawValue.getValue();
            if (valueSerializer != null) {
                value = valueSerializer.deserialize(rawValue.getValue());
            }
            set.add(new DefaultTypedTuple(value, rawValue.getScore()));
        }
        return set;
    }

    // --------------------------------------------------------------------------------------------
    // Redis 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 执行事务，使用默认的 {@link RedisSerializer} 反序列化 byte[] 或 byte[] 的集合或映射或元组的任何结果。<br/>
     * 其他结果类型（长整型、布尔型等）在转换后的结果中保持原样。<br/>
     * 如果在 {@link RedisConnectionFactory} 中禁用了 tx 结果的转换，则 exec 的结果将在不反序列化的情况下返回。<br/>
     * 此检查主要是为了向后兼容 1.0。
     *
     * @return transaction exec 的（可能反序列化的）结果
     */
    @Override
    public List<Object> exec() {
        List<Object> results = execRaw();
        if (getRequiredConnectionFactory().getConvertPipelineAndTxResults()) {
            return deserializeMixedResults(results, valueSerializer, hashKeySerializer, hashValueSerializer);
        } else {
            return results;
        }
    }

    @Override
    public List<Object> exec(RedisSerializer<?> valueSerializer) {
        return deserializeMixedResults(execRaw(), valueSerializer, valueSerializer, valueSerializer);
    }

    protected List<Object> execRaw() {
        List<Object> raw = execute(RedisTxCommands::exec);
        return raw == null ? Collections.emptyList() : raw;
    }

    @Override
    public Boolean copy(K source, K target, boolean replace) {
        byte[] sourceKey = rawKey(source);
        byte[] targetKey = rawKey(target);
        return execute(connection -> connection.copy(sourceKey, targetKey, replace), true);
    }

    /**
     * 执行 Redis 恢复命令。传入的值应该是从 {@link #dump(Object)} 返回的精确序列化数据，因为 Redis 使用非标准序列化机制。
     *
     * @param key        恢复的key
     * @param value      要恢复的值，由 {@link #dump(Object)} 返回
     * @param timeToLive 恢复密钥的过期时间，或 0 表示无过期时间
     * @param unit       timeToLive 的时间单位
     * @param replace    使用 {@literal true} 替换可能存在的值而不是错误。
     * @throws RedisSystemException 如果您尝试恢复的密钥已经存在并且 {@code replace} 设置为 {@literal false}
     */
    @Override
    public void restore(K key, final byte[] value, long timeToLive, TimeUnit unit, boolean replace) {
        byte[] rawKey = rawKey(key);
        long rawTimeout = TimeoutUtils.toMillis(timeToLive, unit);
        execute(connection -> {
            connection.restore(rawKey, rawTimeout, value, replace);
            return null;
        }, true);
    }

    @Override
    public void multi() {
        execute(connection -> {
            connection.multi();
            return null;
        }, true);
    }

    @Override
    public void discard() {
        execute(connection -> {
            connection.discard();
            return null;
        }, true);
    }

    @Override
    public void convertAndSend(String channel, Object message) {
        Assert.hasText(channel, "a non-empty channel is required");
        byte[] rawChannel = rawString(channel);
        byte[] rawMessage = rawValue(message);
        execute(connection -> {
            connection.publish(rawChannel, rawMessage);
            return null;
        }, true);
    }

    @Override
    public void killClient(final String host, final int port) {
        execute((RedisCallback<Void>) connection -> {
            connection.killClient(host, port);
            return null;
        });
    }

    @Override
    public List<RedisClientInfo> getClientList() {
        return execute(RedisServerCommands::getClientList);
    }

    @Override
    public void slaveOf(final String host, final int port) {
        execute((RedisCallback<Void>) connection -> {
            connection.slaveOf(host, port);
            return null;
        });
    }

    @Override
    public void slaveOfNoOne() {
        execute((RedisCallback<Void>) connection -> {
            connection.slaveOfNoOne();
            return null;
        });
    }

    // --------------------------------------------------------------------------------------------
    // Key 操作
    // --------------------------------------------------------------------------------------------

    @Override
    public Boolean delete(K key) {
        byte[] rawKey = rawKey(key);
        Long result = execute(connection -> connection.del(rawKey), true);
        return result != null && result.intValue() == 1;
    }

    @Override
    public Long delete(Collection<K> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        byte[][] rawKeys = rawKeys(keys);
        return execute(connection -> connection.del(rawKeys), true);
    }

    @Override
    public Boolean unlink(K key) {
        byte[] rawKey = rawKey(key);
        Long result = execute(connection -> connection.unlink(rawKey), true);
        return result != null && result.intValue() == 1;
    }

    @Override
    public Long unlink(Collection<K> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        byte[][] rawKeys = rawKeys(keys);
        return execute(connection -> connection.unlink(rawKeys), true);
    }

    @Override
    public Boolean hasKey(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.exists(rawKey), true);
    }

    @Override
    public Long countExistingKeys(Collection<K> keys) {
        Assert.notNull(keys, "Keys must not be null!");
        byte[][] rawKeys = rawKeys(keys);
        return execute(connection -> connection.exists(rawKeys), true);
    }

    @Override
    public Boolean expire(K key, final long timeout, final TimeUnit unit) {
        byte[] rawKey = rawKey(key);
        long rawTimeout = TimeoutUtils.toMillis(timeout, unit);
        return execute(connection -> {
            try {
                return connection.pExpire(rawKey, rawTimeout);
            } catch (Exception e) {
                // 驱动程序可能不支持 pExpire 或者我们可能在 Redis 2.4 上运行
                return connection.expire(rawKey, TimeoutUtils.toSeconds(timeout, unit));
            }
        }, true);
    }

    @Override
    public Boolean expireAt(K key, final Date date) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> {
            try {
                return connection.pExpireAt(rawKey, date.getTime());
            } catch (Exception e) {
                return connection.expireAt(rawKey, date.getTime() / 1000);
            }
        }, true);
    }

    @Override
    public Long getExpire(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.ttl(rawKey), true);
    }

    @Override
    public Long getExpire(K key, final TimeUnit timeUnit) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> {
            try {
                return connection.pTtl(rawKey, timeUnit);
            } catch (Exception e) {
                // 驱动程序可能不支持pTtl，或者我们可能在Redis 2.4上运行
                return connection.ttl(rawKey, timeUnit);
            }
        }, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<K> keys(K pattern) {
        byte[] rawKey = rawKey(pattern);
        Set<byte[]> rawKeys = execute(connection -> connection.keys(rawKey), true);
        return keySerializer != null ? SerializationUtils.deserialize(rawKeys, keySerializer) : (Set<K>) rawKeys;
    }

    @Override
    public Boolean persist(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.persist(rawKey), true);
    }

    @Override
    public Boolean move(K key, final int dbIndex) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.move(rawKey, dbIndex), true);
    }

    @Override
    public K randomKey() {
        byte[] rawKey = execute(RedisKeyCommands::randomKey, true);
        return deserializeKey(rawKey);
    }

    @Override
    public void rename(K oldKey, K newKey) {
        byte[] rawOldKey = rawKey(oldKey);
        byte[] rawNewKey = rawKey(newKey);
        execute(connection -> {
            connection.rename(rawOldKey, rawNewKey);
            return null;
        }, true);
    }

    @Override
    public Boolean renameIfAbsent(K oldKey, K newKey) {
        byte[] rawOldKey = rawKey(oldKey);
        byte[] rawNewKey = rawKey(newKey);
        return execute(connection -> connection.renameNX(rawOldKey, rawNewKey), true);
    }

    @Override
    public DataType type(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.type(rawKey), true);
    }

    /**
     * 执行 Redis dump 命令并返回结果。<br/>
     * Redis 使用非标准序列化机制并包含校验和信息，因此返回原始字节而不是使用 valueSerializer 进行反序列化。<br/>
     * 使用 dump 的返回值作为 value 参数来恢复
     *
     * @param key 转储的 key
     * @return results 转储操作的结果
     */
    @Override
    public byte[] dump(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.dump(rawKey), true);
    }

    @Override
    public void watch(K key) {
        byte[] rawKey = rawKey(key);
        execute(connection -> {
            connection.watch(rawKey);
            return null;
        }, true);
    }

    @Override
    public void watch(Collection<K> keys) {
        byte[][] rawKeys = rawKeys(keys);
        execute(connection -> {
            connection.watch(rawKeys);
            return null;
        }, true);
    }

    @Override
    public void unwatch() {
        execute(connection -> {
            connection.unwatch();
            return null;
        }, true);
    }

    // --------------------------------------------------------------------------------------------
    // Sort 操作
    // --------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public List<V> sort(SortQuery<K> query) {
        return sort(query, valueSerializer);
    }

    @Override
    public <T> List<T> sort(SortQuery<K> query, RedisSerializer<T> resultSerializer) {
        byte[] rawKey = rawKey(query.getKey());
        SortParameters params = QueryUtils.convertQuery(query, stringSerializer);
        List<byte[]> vals = execute(connection -> connection.sort(rawKey, params), true);
        return SerializationUtils.deserialize(vals, resultSerializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> sort(SortQuery<K> query, BulkMapper<T, V> bulkMapper) {
        return sort(query, bulkMapper, valueSerializer);
    }

    @Override
    public <T, S> List<T> sort(SortQuery<K> query, BulkMapper<T, S> bulkMapper, RedisSerializer<S> resultSerializer) {
        List<S> values = sort(query, resultSerializer);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        int bulkSize = query.getGetPattern().size();
        List<T> result = new ArrayList<>(values.size() / bulkSize + 1);
        List<S> bulk = new ArrayList<>(bulkSize);
        for (S s : values) {
            bulk.add(s);
            if (bulk.size() == bulkSize) {
                result.add(bulkMapper.mapBulk(Collections.unmodifiableList(bulk)));
                // 创建一个新列表（我们可以重用旧列表，但客户可能会因为某些原因而挂起它）
                bulk = new ArrayList<>(bulkSize);
            }
        }
        return result;
    }

    @Override
    public Long sort(SortQuery<K> query, K storeKey) {
        byte[] rawStoreKey = rawKey(storeKey);
        byte[] rawKey = rawKey(query.getKey());
        SortParameters params = QueryUtils.convertQuery(query, stringSerializer);
        return execute(connection -> connection.sort(rawKey, params, rawStoreKey), true);
    }

    // --------------------------------------------------------------------------------------------
    // 返回 Redis 操作对象
    // --------------------------------------------------------------------------------------------

    @Override
    public ValueOperations<K, V> opsForValue() {
        return valueOps;
    }

    @Override
    public BoundValueOperations<K, V> boundValueOps(K key) {
        return new DefaultBoundValueOperations<>(key, this);
    }

    @Override
    public ListOperations<K, V> opsForList() {
        return listOps;
    }

    @Override
    public BoundListOperations<K, V> boundListOps(K key) {
        return new DefaultBoundListOperations<>(key, this);
    }

    @Override
    public SetOperations<K, V> opsForSet() {
        return setOps;
    }

    @Override
    public BoundSetOperations<K, V> boundSetOps(K key) {
        return new DefaultBoundSetOperations<>(key, this);
    }

    @Override
    public ZSetOperations<K, V> opsForZSet() {
        return zSetOps;
    }

    @Override
    public BoundZSetOperations<K, V> boundZSetOps(K key) {
        return new DefaultBoundZSetOperations<>(key, this);
    }

    @Override
    public <HK, HV> HashOperations<K, HK, HV> opsForHash() {
        return new DefaultHashOperations<>(this);
    }

    @Override
    public <HK, HV> BoundHashOperations<K, HK, HV> boundHashOps(K key) {
        return new DefaultBoundHashOperations<>(key, this);
    }

    @Override
    public HyperLogLogOperations<K, V> opsForHyperLogLog() {
        return hllOps;
    }

    @Override
    public GeoOperations<K, V> opsForGeo() {
        return geoOps;
    }

    @Override
    public BoundGeoOperations<K, V> boundGeoOps(K key) {
        return new DefaultBoundGeoOperations<>(key, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <HK, HV> StreamOperations<K, HK, HV> opsForStream() {
        return (StreamOperations<K, HK, HV>) streamOps;
    }

    @Override
    public <HK, HV> StreamOperations<K, HK, HV> opsForStream(HashMapper<? super K, ? super HK, ? super HV> hashMapper) {
        return new DefaultStreamOperations<>(this, hashMapper);
    }

    @Override
    public <HK, HV> BoundStreamOperations<K, HK, HV> boundStreamOps(K key) {
        return new DefaultBoundStreamOperations<>(key, this);
    }

    @Override
    public ClusterOperations<K, V> opsForCluster() {
        return clusterOps;
    }
}
