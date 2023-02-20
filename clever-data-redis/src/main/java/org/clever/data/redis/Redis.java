package org.clever.data.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.DateUtils;
import org.clever.core.io.ClassPathResource;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.AbstractDataSource;
import org.clever.data.geo.Distance;
import org.clever.data.geo.Point;
import org.clever.data.redis.config.RedisProperties;
import org.clever.data.redis.connection.DataType;
import org.clever.data.redis.connection.RedisConnectionFactory;
import org.clever.data.redis.connection.RedisGeoCommands;
import org.clever.data.redis.connection.RedisZSetCommands;
import org.clever.data.redis.core.*;
import org.clever.data.redis.core.script.DefaultRedisScript;
import org.clever.data.redis.core.script.RedisScript;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.data.redis.support.*;
import org.clever.scripting.support.ResourceScriptSource;
import org.clever.util.Assert;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 包含 RedisTemplate 和 RedissonClient 的Redis客户端工具
 * <p>
 * 作者： lzw<br/>
 * 创建时间：2019-10-06 22:01 <br/>
 */
@Slf4j
public class Redis extends AbstractDataSource {
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();

    static {
        RATE_LIMIT_SCRIPT.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/request_rate_limiter.lua")
        ));
        RATE_LIMIT_SCRIPT.setResultType(List.class);
    }

    /**
     * Redis 名称
     */
    @Getter
    private final String redisName;
    /**
     * 当前 Redis 配置
     */
    private final RedisProperties properties;
    @Getter
    private final ClientResources clientResources;
    @Getter
    private final JacksonMapper jacksonMapper;
    @Getter
    private final RedisConnectionFactory connectionFactory;
    @Getter
    private final RedisTemplate<String, String> redisTemplate;
    @Getter
    private final RedissonClient redisson;

    /**
     * @param redisTemplate RedisTemplate
     */
    public Redis(RedisTemplate<String, String> redisTemplate) {
        Assert.notNull(redisTemplate, "参数 redisTemplate 不能为 null");
        this.redisName = null;
        this.properties = null;
        this.clientResources = null;
        this.jacksonMapper = JacksonMapper.getInstance();
        this.connectionFactory = redisTemplate.getRequiredConnectionFactory();
        this.redisTemplate = redisTemplate;
        this.redisson = null;
        initCheck();
    }

    /**
     * @param redisName          Redis 名称
     * @param properties         当前 Redis 配置
     * @param clientResources    ClientResources
     * @param objectMapper       JSON序列化反序列化对象
     * @param builderCustomizers 自定义构建器
     */
    public Redis(String redisName,
                 RedisProperties properties,
                 ClientResources clientResources,
                 ObjectMapper objectMapper,
                 List<LettuceClientConfigurationBuilderCustomizer> builderCustomizers) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(clientResources, "参数 clientResources 不能为 null");
        Assert.notNull(objectMapper, "参数 objectMapper 不能为 null");
        Assert.notNull(builderCustomizers, "参数 builderCustomizers 不能为 null");
        this.redisName = redisName;
        this.properties = properties;
        this.clientResources = clientResources;
        this.jacksonMapper = new JacksonMapper(objectMapper);
        RedisTemplate<String, String> redisTemplate = RedisTemplateFactory.createRedisTemplate(
                properties, clientResources, builderCustomizers, objectMapper
        );
        this.connectionFactory = redisTemplate.getRequiredConnectionFactory();
        this.redisTemplate = redisTemplate;
        this.redisson = Redisson.create(RedissonClientFactory.createConfig(properties, null));
        initCheck();
    }

    /**
     * @param redisName       Redis 名称
     * @param properties      当前 Redis 配置
     * @param clientResources ClientResources
     * @param objectMapper    JSON序列化反序列化对象
     */
    public Redis(String redisName,
                 RedisProperties properties,
                 ClientResources clientResources,
                 ObjectMapper objectMapper,
                 LettuceClientConfigurationBuilderCustomizer builderCustomizer) {
        this(redisName, properties, clientResources, objectMapper, Collections.singletonList(builderCustomizer));
    }

    /**
     * @param redisName       Redis 名称
     * @param properties      当前 Redis 配置
     * @param clientResources ClientResources
     * @param objectMapper    JSON序列化反序列化对象
     */
    public Redis(String redisName, RedisProperties properties, ClientResources clientResources, ObjectMapper objectMapper) {
        this(redisName, properties, clientResources, objectMapper, Collections.emptyList());
    }

    /**
     * @param redisName       Redis 名称
     * @param properties      当前 Redis 配置
     * @param clientResources ClientResources
     */
    public Redis(String redisName, RedisProperties properties, ClientResources clientResources) {
        this(redisName, properties, clientResources, JacksonMapper.getInstance().getMapper(), Collections.emptyList());
    }

    /**
     * @param redisName  Redis 名称
     * @param properties 当前 Redis 配置
     */
    public Redis(String redisName, RedisProperties properties) {
        this(redisName, properties, DefaultClientResources.create(), JacksonMapper.getInstance().getMapper(), Collections.emptyList());
    }

    /**
     * @param redisName    Redis 名称
     * @param properties   当前 Redis 配置
     * @param objectMapper JSON序列化反序列化对象
     */
    public Redis(String redisName, RedisProperties properties, ObjectMapper objectMapper) {
        this(redisName, properties, DefaultClientResources.create(), objectMapper, Collections.emptyList());
    }

    /**
     * @param properties   当前 Redis 配置
     * @param objectMapper JSON序列化反序列化对象
     */
    public Redis(RedisProperties properties, ObjectMapper objectMapper) {
        this(null, properties, DefaultClientResources.create(), objectMapper, Collections.emptyList());
    }

    /**
     * @param properties 当前 Redis 配置
     */
    public Redis(RedisProperties properties) {
        this(null, properties, DefaultClientResources.create(), JacksonMapper.getInstance().getMapper(), Collections.emptyList());
    }

    /**
     * 校验数据源是否可用
     */
    @Override
    public void initCheck() {
        RedisCallback<Void> callback = connection -> {
            String res = connection.ping();
            log.debug("ping -> {}", res);
            return null;
        };
        try {
            redisTemplate.execute(callback);
        } catch (Exception e) {
            throw new RuntimeException("Redis 创建失败", e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        super.close();
        try {
            clientResources.shutdown();
        } catch (Throwable e) {
            log.error("ClientResources shutdown 错误", e);
        }
        try {
            connectionFactory.destroy();
        } catch (Throwable e) {
            log.error("RedisConnectionFactory destroy 错误", e);
        }
        try {
            redisson.shutdown();
        } catch (Throwable e) {
            log.error("Redisson shutdown 错误", e);
        }
    }

    // --------------------------------------------------------------------------------------------
    // Key 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 删除 key
     *
     * @param key key
     */
    public Boolean kDelete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除 key
     *
     * @param keys keys
     */
    public Long kDelete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 删除 key
     *
     * @param keys keys
     */
    public Long kDelete(String... keys) {
        return redisTemplate.delete(Arrays.asList(keys));
    }

    /**
     * 序列化给定 key ，并返回被序列化的值
     *
     * @param key key
     */
    public byte[] kDump(String key) {
        return redisTemplate.dump(key);
    }

    /**
     * 检查给定 key 是否存在
     *
     * @param key key
     */
    public Boolean kHasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 为给定 key 设置过期时间，以毫秒计
     *
     * @param key     key
     * @param timeout timeout以毫秒计
     */
    public Boolean kExpire(String key, Number timeout) {
        return redisTemplate.expire(key, timeout.longValue(), TimeUnit.MILLISECONDS);
    }

    /**
     * 为给定 key 设置过期时间
     *
     * @param key  key
     * @param date 过期时间
     */
    public Boolean kExpireAt(String key, Date date) {
        return redisTemplate.expireAt(key, date);
    }

    /**
     * 为给定 key 设置过期时间
     *
     * @param key     key
     * @param dateObj 过期时间(支持: Date、字符串时间、时间戳)
     */
    public Boolean kExpireAt(String key, Object dateObj) {
        Date date = DateUtils.parseDate(dateObj);
        if (date == null) {
            throw new IllegalArgumentException("过期时间必须是一个时间字符串");
        }
        return redisTemplate.expireAt(key, date);
    }

    /**
     * 查找所有符合给定模式( pattern)的 key
     *
     * @param pattern 模式( pattern)
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 将当前数据库的 key 移动到给定的数据库 db 当中
     *
     * @param key     key
     * @param dbIndex dbIndex
     */
    public Boolean kMove(String key, int dbIndex) {
        return redisTemplate.move(key, dbIndex);
    }

    /**
     * 移除 key 的过期时间，key 将持久保持
     *
     * @param key key
     */
    public Boolean kPersist(String key) {
        return redisTemplate.persist(key);
    }

    /**
     * 以毫秒为单位返回 key 的剩余的过期时间
     *
     * @param key key
     */
    public Long kGetExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    }

    /**
     * 从当前数据库中随机返回一个 key
     */
    public String kRandomKey() {
        return redisTemplate.randomKey();
    }

    /**
     * 修改 key 的名称
     *
     * @param oldKey oldKey
     * @param newKey newKey
     */
    public void kRename(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * 仅当 newKey 不存在时，将 key 改名为 newKey
     *
     * @param oldKey oldKey
     * @param newKey newKey
     */
    public Boolean kRenameIfAbsent(String oldKey, String newKey) {
        return redisTemplate.renameIfAbsent(oldKey, newKey);
    }

    /**
     * 返回 key 所储存的值的类型
     *
     * @param key key
     */
    public DataType kType(String key) {
        return redisTemplate.type(key);
    }


    // --------------------------------------------------------------------------------------------
    // Value 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 设置指定 key 的值
     *
     * @param key   key
     * @param value value
     */
    public void vSet(String key, Object value) {
        redisTemplate.opsForValue().set(key, serializer(value));
    }

    /**
     * 将值 value 关联到 key ，并将 key 的过期时间设为 seconds (以毫秒为单位)
     *
     * @param key     key
     * @param value   value
     * @param timeout 过期时间毫秒
     */
    public void vSet(String key, Object value, Number timeout) {
        redisTemplate.opsForValue().set(key, serializer(value), Duration.ofMillis(timeout.longValue()));
    }

    /**
     * 只有在 key 不存在时设置 key 的值
     *
     * @param key   key
     * @param value value
     */
    public Boolean vSetIfAbsent(String key, Object value) {
        return redisTemplate.opsForValue().setIfAbsent(key, serializer(value));
    }

    /**
     * @param key     key
     * @param value   value
     * @param timeout 过期时间毫秒
     */
    public Boolean vSetIfAbsent(String key, Object value, Number timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, serializer(value), Duration.ofMillis(timeout.longValue()));
    }

    /**
     * 获取Value的值
     *
     * @param key   key
     * @param clazz 返回数据类型
     */
    public <T> T vGet(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().get(key);
        return deserializer(value, clazz);
    }

    /**
     * 获取Value的值
     *
     * @param key key
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> vGet(String key) {
        return vGet(key, Map.class);
    }

    /**
     * 返回 Value 中字符串值的子字符
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    public String vGet(String key, Number start, Number end) {
        return redisTemplate.opsForValue().get(key, start.longValue(), end.longValue());
    }

    /**
     * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)
     *
     * @param key   key
     * @param value value
     * @param clazz 返回数据类型
     */
    public <T> T vGetAndSet(String key, Object value, Class<T> clazz) {
        String res = redisTemplate.opsForValue().getAndSet(key, serializer(value));
        return deserializer(res, clazz);
    }

    /**
     * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)
     *
     * @param key   key
     * @param value value
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> vGetAndSet(String key, Object value) {
        return vGetAndSet(key, value, Map.class);
    }

    /**
     * 对 key 所储存的字符串值，获取指定偏移量上的位(bit)
     *
     * @param key    key
     * @param offset 偏移量
     */
    public Boolean vGetBit(String key, Number offset) {
        return redisTemplate.opsForValue().getBit(key, offset.longValue());
    }

    /**
     * 获取所有(一个或多个)给定 key 的值
     *
     * @param keys  keys
     * @param clazz 返回数据类型
     */
    public <T> List<T> vMultiGet(Collection<String> keys, Class<T> clazz) {
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        return deserializer(values, clazz);
    }

    /**
     * 获取所有(一个或多个)给定 key 的值
     *
     * @param keys keys
     */
    @SuppressWarnings("rawtypes")
    public List<Map> vMultiGet(Collection<String> keys) {
        return vMultiGet(keys, Map.class);
    }

    /**
     * 获取所有(一个或多个)给定 key 的值
     *
     * @param keys  keys
     * @param clazz 返回数据类型
     */
    public <T> List<T> vMultiGet(String[] keys, Class<T> clazz) {
        List<String> values = redisTemplate.opsForValue().multiGet(Arrays.asList(keys));
        return deserializer(values, clazz);
    }

    /**
     * 获取所有(一个或多个)给定 key 的值
     *
     * @param keys keys
     */
    @SuppressWarnings("rawtypes")
    public List<Map> vMultiGet(String[] keys) {
        return vMultiGet(keys, Map.class);
    }

    /**
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)
     *
     * @param key    key
     * @param offset 偏移量
     * @param value  值
     */
    public Boolean vSetBit(String key, Number offset, boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset.longValue(), value);
    }

    /**
     * 用 value 参数覆写给定 key 所储存的字符串值，从偏移量 offset 开始
     *
     * @param key    key
     * @param value  value
     * @param offset 偏移量
     */
    public void vSetRange(String key, Object value, Number offset) {
        redisTemplate.opsForValue().set(key, serializer(value), offset.longValue());
    }

    /**
     * 返回 key 所储存的字符串值的长度
     *
     * @param key key
     */
    public Long vSize(String key) {
        return redisTemplate.opsForValue().size(key);
    }

    /**
     * 同时设置一个或多个 key-value 对
     *
     * @param map 多个 key-value 对
     */
    public void vMultiSet(Map<String, Object> map) {
        redisTemplate.opsForValue().multiSet(serializer(map));
    }

    /**
     * 同时设置一个或多个 key-value 对，当且仅当所有给定 key 都不存在
     *
     * @param map 多个 key-value 对
     */
    public void vMultiSetIfAbsent(Map<String, Object> map) {
        redisTemplate.opsForValue().multiSetIfAbsent(serializer(map));
    }

    /**
     * 将 key 中储存的数字值增 1
     *
     * @param key key
     */
    public Long vIncrement(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 将 key 所储存的值加上给定的增量值（increment）
     *
     * @param key   key
     * @param delta 增量值
     */
    public Long vIncrement(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 将 key 所储存的值加上给定的增量值（increment）
     *
     * @param key   key
     * @param delta 增量值
     */
    public Long vIncrement(String key, Integer delta) {
        return redisTemplate.opsForValue().increment(key, delta.longValue());
    }

    /**
     * 将 key 所储存的值加上给定的增量值（increment）
     *
     * @param key   key
     * @param delta 增量值
     */
    public Double vIncrement(String key, double delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 将 key 中储存的数字值减 1
     *
     * @param key key
     */
    public Long vDecrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * key 所储存的值减去给定的减量值（decrement）
     *
     * @param key   key
     * @param delta 减量值
     */
    public Long vDecrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * key 所储存的值减去给定的减量值（decrement）
     *
     * @param key   key
     * @param delta 减量值
     */
    public Long vDecrement(String key, Integer delta) {
        return redisTemplate.opsForValue().decrement(key, delta.longValue());
    }

    /**
     * key 所储存的值减去给定的减量值（decrement）
     *
     * @param key   key
     * @param delta 减量值
     */
    public Long vDecrement(String key, Double delta) {
        return redisTemplate.opsForValue().decrement(key, delta.longValue());
    }

    /**
     * 如果 key 已经存在并且是一个字符串， APPEND 命令将指定的 value 追加到该 key 原来值（value）的末尾
     *
     * @param key   key
     * @param value value
     */
    public Integer vAppend(String key, String value) {
        return redisTemplate.opsForValue().append(key, value);
    }

    // --------------------------------------------------------------------------------------------
    // Hash 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 删除一个或多个哈希表字段
     *
     * @param key      key
     * @param hashKeys hashKeys
     */
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 删除一个或多个哈希表字段
     *
     * @param key      key
     * @param hashKeys hashKeys
     */
    public Long hDelete(String key, Collection<?> hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys.toArray());
    }

    /**
     * 查看哈希表 key 中，指定的字段是否存在
     *
     * @param key     key
     * @param hashKey hashKey
     */
    public Boolean hHasKey(String key, Object hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * 获取存储在哈希表中指定字段的值
     *
     * @param key     key
     * @param hashKey hashKey
     */
    public Object hGet(String key, Object hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取所有给定字段的值
     *
     * @param key      key
     * @param hashKeys hashKeys
     */
    public List<Object> hMultiGet(String key, Collection<Object> hashKeys) {
        return redisTemplate.opsForHash().multiGet(key, hashKeys);
    }

    /**
     * 获取所有给定字段的值
     *
     * @param key      key
     * @param hashKeys hashKeys
     */
    public List<Object> hMultiGet(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().multiGet(key, Arrays.asList(hashKeys));
    }

    /**
     * 为哈希表 key 中的指定字段的整数值加上增量 increment
     *
     * @param key     key
     * @param hashKey hashKey
     * @param delta   增量
     */
    public Long hIncrement(String key, Object hashKey, long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    /**
     * 为哈希表 key 中的指定字段的整数值加上增量 increment
     *
     * @param key     key
     * @param hashKey hashKey
     * @param delta   增量
     */
    public Long hIncrement(String key, Object hashKey, Integer delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta.longValue());
    }

    /**
     * 为哈希表 key 中的指定字段的整数值加上增量 increment
     *
     * @param key     key
     * @param hashKey hashKey
     * @param delta   增量
     */
    public Double hIncrement(String key, Object hashKey, double delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    /**
     * 获取所有哈希表中的字段
     *
     * @param key key
     */
    public Set<Object> hKeys(String key) {
        return redisTemplate.opsForHash().keys(key);
    }

    /**
     * 返回与hashKey关联的值的长度。如果键或hashKey不存在，则返回0
     *
     * @param key     key
     * @param hashKey hashKey
     */
    public Long hLengthOfValue(String key, Object hashKey) {
        return redisTemplate.opsForHash().lengthOfValue(key, hashKey);
    }

    /**
     * 获取哈希表中字段的数量
     *
     * @param key key
     */
    public Long hSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * 同时将多个 field-value (key-value)对设置到哈希表 key 中
     *
     * @param key key
     * @param m   field-value
     */
    public void hPutAll(String key, Map<?, ?> m) {
        redisTemplate.opsForHash().putAll(key, m);
    }

    /**
     * 将哈希表 key 中的字段 field 的值设为 value
     *
     * @param key     key
     * @param hashKey field
     * @param value   value
     */
    public void hPut(String key, Object hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 只有在字段 field 不存在时，设置哈希表字段的值
     *
     * @param key     key
     * @param hashKey field
     * @param value   字段的值
     */
    public Boolean hPutIfAbsent(String key, Object hashKey, Object value) {
        return redisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
    }

    /**
     * 获取哈希表中所有值
     *
     * @param key key
     */
    public List<Object> hValues(String key) {
        return redisTemplate.opsForHash().values(key);
    }

    /**
     * 将整个散列存储在键上
     *
     * @param key key
     */
    public Map<Object, Object> hEntries(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 迭代哈希表中的键值对
     *
     * @param key      key
     * @param count    数量
     * @param pattern  字段匹配字符串
     * @param callback 数据迭代回调
     */
    public void hScan(String key, Number count, String pattern, ScanCallback<Map.Entry<Object, Object>> callback) throws IOException {
        Assert.notNull(callback, "ScanCallback不能为空");
        ScanOptions scanOptions = ScanOptions.scanOptions().count(count.longValue()).match(pattern).build();
        Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(key, scanOptions);
        cursorForeach(cursor, callback);
    }

    // --------------------------------------------------------------------------------------------
    // List 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 获取列表指定范围内的元素
     *
     * @param key   key
     * @param start start
     * @param end   end
     * @param clazz 返回数据类型
     */
    public <T> List<T> lRange(String key, Number start, Number end, Class<T> clazz) {
        List<String> res = redisTemplate.opsForList().range(key, start.longValue(), end.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 获取列表指定范围内的元素
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    @SuppressWarnings("rawtypes")
    public List<Map> lRange(String key, Number start, Number end) {
        return lRange(key, start, end, Map.class);
    }

    /**
     * 对一个列表进行修剪(trim)，就是说，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    public void lTrim(String key, Number start, Number end) {
        redisTemplate.opsForList().trim(key, start.longValue(), end.longValue());
    }

    /**
     * 获取列表长度
     *
     * @param key key
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 将一个或多个值插入到列表头部
     *
     * @param key   key
     * @param value value
     */
    public Long lLeftPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, serializer(value));
    }

    /**
     * 将一个或多个值插入到列表头部
     *
     * @param key    key
     * @param values values
     */
    public Long lLeftPushAll(String key, Object... values) {
        return redisTemplate.opsForList().leftPushAll(key, serializer(values));
    }

    /**
     * 将一个或多个值插入到列表头部
     *
     * @param key    key
     * @param values values
     */
    public Long lLeftPushAll(String key, Collection<Object> values) {
        return redisTemplate.opsForList().leftPushAll(key, serializer(values));
    }

    /**
     * 将一个值插入到已存在的列表头部
     *
     * @param key   key
     * @param value value
     */
    public Long lLeftPushIfPresent(String key, Object value) {
        return redisTemplate.opsForList().leftPushIfPresent(key, serializer(value));
    }

    /**
     * 将值前置到键值之前
     *
     * @param key   key
     * @param pivot pivot
     * @param value value
     */
    public Long lLeftPush(String key, Object pivot, Object value) {
        return redisTemplate.opsForList().leftPush(key, serializer(pivot), serializer(value));
    }

    /**
     * 在列表中添加一个或多个值
     *
     * @param key   key
     * @param value value
     */
    public Long lRightPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, serializer(value));
    }

    /**
     * 在列表中添加一个或多个值
     *
     * @param key    key
     * @param values values
     */
    public Long lRightPushAll(String key, Object... values) {
        return redisTemplate.opsForList().rightPushAll(key, serializer(values));
    }

    /**
     * 在列表中添加一个或多个值
     *
     * @param key    key
     * @param values values
     */
    public Long lRightPushAll(String key, Collection<Object> values) {
        return redisTemplate.opsForList().rightPushAll(key, serializer(values));
    }

    /**
     * 仅当列表存在时才向键追加值
     *
     * @param key   key
     * @param value value
     */
    public Long lRightPushIfPresent(String key, Object value) {
        return redisTemplate.opsForList().rightPushIfPresent(key, serializer(value));
    }

    /**
     * 在键值之前追加值
     *
     * @param key   key
     * @param pivot pivot
     * @param value value
     */
    public Long lRightPush(String key, Object pivot, Object value) {
        return redisTemplate.opsForList().rightPush(key, serializer(pivot), serializer(value));
    }

    /**
     * 通过索引设置列表元素的值
     *
     * @param key   key
     * @param index 索引
     * @param value value
     */
    public void lSet(String key, Number index, Object value) {
        redisTemplate.opsForList().set(key, index.longValue(), serializer(value));
    }

    /**
     * 移除列表元素，从存储在键上的列表中删除第一次出现的值计数
     *
     * @param key   key
     * @param count count
     * @param value value
     */
    public Long lRemove(String key, Number count, Object value) {
        return redisTemplate.opsForList().remove(key, count.longValue(), value);
    }

    /**
     * 通过索引获取列表中的元素
     *
     * @param key   key
     * @param index 索引
     */
    public Object lIndex(String key, Number index) {
        return redisTemplate.opsForList().index(key, index.longValue());
    }

    /**
     * 移出并获取列表的第一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key key
     */
    public Object lLeftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 移出并获取列表的第一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key     key
     * @param timeout timeout 毫秒
     */
    public Object lLeftPop(String key, Number timeout) {
        return redisTemplate.opsForList().leftPop(key, timeout.longValue(), TimeUnit.MILLISECONDS);
    }

    /**
     * 移出并获取列表的最后一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key key
     */
    public Object lRightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 移出并获取列表的最后一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key     key
     * @param timeout timeout 毫秒
     */
    public Object lRightPop(String key, Number timeout) {
        return redisTemplate.opsForList().rightPop(key, timeout.longValue(), TimeUnit.MILLISECONDS);
    }

    /**
     * 从列表中弹出一个值，将弹出的元素插入到另外一个列表中并返回它； 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param sourceKey      sourceKey
     * @param destinationKey destinationKey
     */
    public Object lRightPopAndLeftPush(String sourceKey, String destinationKey) {
        return redisTemplate.opsForList().rightPopAndLeftPush(sourceKey, destinationKey);
    }

    /**
     * 从列表中弹出一个值，将弹出的元素插入到另外一个列表中并返回它； 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param sourceKey      sourceKey
     * @param destinationKey destinationKey
     * @param timeout        timeout 毫秒
     */
    public Object lRightPopAndLeftPush(String sourceKey, String destinationKey, Number timeout) {
        return redisTemplate.opsForList().rightPopAndLeftPush(sourceKey, destinationKey, timeout.longValue(), TimeUnit.MILLISECONDS);
    }

    // --------------------------------------------------------------------------------------------
    // Set 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 向集合添加一个或多个成员
     *
     * @param key    key
     * @param values values
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, serializer(values).toArray(new String[0]));
    }

    /**
     * 向集合添加一个或多个成员
     *
     * @param key    key
     * @param values values
     */
    public Long sAdd(String key, Collection<Object> values) {
        return redisTemplate.opsForSet().add(key, serializer(values).toArray(new String[0]));
    }

    /**
     * 移除集合中一个或多个成员
     *
     * @param key    key
     * @param values values
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, serializer(values).toArray());
    }

    /**
     * 移除集合中一个或多个成员
     *
     * @param key    key
     * @param values values
     */
    public Long sRemove(String key, Collection<Object> values) {
        return redisTemplate.opsForSet().remove(key, serializer(values).toArray());
    }

    /**
     * 移除并返回集合中的一个随机元素
     *
     * @param key key
     */
    public Object sPop(String key) {
        return redisTemplate.opsForSet().pop(key);
    }

    /**
     * 移除并返回集合中的count个随机元素
     *
     * @param key   key
     * @param count count
     * @param clazz 返回数据类型
     */
    public <T> List<T> sPop(String key, Number count, Class<T> clazz) {
        List<String> res = redisTemplate.opsForSet().pop(key, count.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 将 value 元素从 key 集合移动到 destKey 集合
     *
     * @param key     key
     * @param value   value
     * @param destKey destKey
     */
    public Boolean sMove(String key, Object value, String destKey) {
        return redisTemplate.opsForSet().move(key, serializer(value), destKey);
    }

    /**
     * 获取集合的成员数
     *
     * @param key key
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 判断 member 元素是否是集合 key 的成员
     *
     * @param key    key
     * @param member member 元素
     */
    public Boolean sIsMember(String key, Object member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 返回给定所有集合的交集
     *
     * @param key      key
     * @param otherKey otherKey
     * @param clazz    返回数据类型
     */
    public <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().intersect(key, otherKey);
        return deserializer(res, clazz);
    }

    /**
     * 返回给定所有集合的交集
     *
     * @param key      key
     * @param otherKey otherKey
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sIntersect(String key, String otherKey) {
        return sIntersect(key, otherKey, Map.class);
    }

    /**
     * 返回给定所有集合的交集
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param clazz     返回数据类型
     */
    public <T> Set<T> sIntersect(String key, Collection<String> otherKeys, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().intersect(key, otherKeys);
        return deserializer(res, clazz);
    }

    /**
     * 返回给定所有集合的交集
     *
     * @param key       key
     * @param otherKeys otherKeys
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sIntersect(String key, Collection<String> otherKeys) {
        return sIntersect(key, otherKeys, Map.class);
    }

    /**
     * 返回给定所有集合的交集
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param clazz     返回数据类型
     */
    public <T> Set<T> sIntersect(String key, String[] otherKeys, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().intersect(key, Arrays.asList(otherKeys));
        return deserializer(res, clazz);
    }

    /**
     * 返回给定所有集合的交集
     *
     * @param key       key
     * @param otherKeys otherKeys
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sIntersect(String key, String[] otherKeys) {
        return sIntersect(key, otherKeys, Map.class);
    }

    /**
     * 返回给定所有集合的交集并存储在 destination 中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     */
    public Long sIntersectAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForSet().intersectAndStore(key, otherKey, destKey);
    }

    /**
     * 返回给定所有集合的交集并存储在 destination 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long sIntersectAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForSet().intersectAndStore(key, otherKeys, destKey);
    }

    /**
     * 返回给定所有集合的交集并存储在 destination 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long sIntersectAndStore(String key, String[] otherKeys, String destKey) {
        return redisTemplate.opsForSet().intersectAndStore(key, Arrays.asList(otherKeys), destKey);
    }

    /**
     * 返回所有给定集合的并集
     *
     * @param key      key
     * @param otherKey otherKey
     * @param clazz    返回数据类型
     */
    public <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().union(key, otherKey);
        return deserializer(res, clazz);
    }

    /**
     * 返回所有给定集合的并集
     *
     * @param key      key
     * @param otherKey otherKey
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sUnion(String key, String otherKey) {
        return sUnion(key, otherKey, Map.class);
    }

    /**
     * 返回所有给定集合的并集
     *
     * @param key       key
     * @param otherKeys otherKey
     * @param clazz     返回数据类型
     */
    public <T> Set<T> sUnion(String key, Collection<String> otherKeys, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().union(key, otherKeys);
        return deserializer(res, clazz);
    }

    /**
     * 返回所有给定集合的并集
     *
     * @param key       key
     * @param otherKeys otherKey
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sUnion(String key, Collection<String> otherKeys) {
        return sUnion(key, otherKeys, Map.class);
    }

    /**
     * 返回所有给定集合的并集
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param clazz     返回数据类型
     */
    public <T> Set<T> sUnion(String key, String[] otherKeys, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().union(key, Arrays.asList(otherKeys));
        return deserializer(res, clazz);
    }

    /**
     * 返回所有给定集合的并集
     *
     * @param key       key
     * @param otherKeys otherKeys
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sUnion(String key, String[] otherKeys) {
        return sUnion(key, otherKeys, Map.class);
    }

    /**
     * 所有给定集合的并集存储在 destKey 集合中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     */
    public Long sUnionAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForSet().unionAndStore(key, otherKey, destKey);
    }

    /**
     * 所有给定集合的并集存储在 destKey 集合中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long sUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForSet().unionAndStore(key, otherKeys, destKey);
    }

    /**
     * 所有给定集合的并集存储在 destKey 集合中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long sUnionAndStore(String key, String[] otherKeys, String destKey) {
        return redisTemplate.opsForSet().unionAndStore(key, Arrays.asList(otherKeys), destKey);
    }

    /**
     * 返回给定所有集合的差集
     *
     * @param key      key
     * @param otherKey otherKey
     * @param clazz    返回数据类型
     */
    public <T> Set<T> sDifference(String key, String otherKey, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().difference(key, otherKey);
        return deserializer(res, clazz);
    }

    /**
     * 返回给定所有集合的差集
     *
     * @param key      key
     * @param otherKey otherKey
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sDifference(String key, String otherKey) {
        return sDifference(key, otherKey, Map.class);
    }

    /**
     * 返回给定所有集合的差集
     *
     * @param key      key
     * @param otherKey otherKey
     * @param clazz    返回数据类型
     */
    public <T> Set<T> sDifference(String key, Collection<String> otherKey, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().difference(key, otherKey);
        return deserializer(res, clazz);
    }

    /**
     * 返回给定所有集合的差集
     *
     * @param key      key
     * @param otherKey otherKey
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sDifference(String key, Collection<String> otherKey) {
        return sDifference(key, otherKey, Map.class);
    }

    /**
     * 返回给定所有集合的差集
     *
     * @param key      key
     * @param otherKey otherKey
     * @param clazz    返回数据类型
     */
    public <T> Set<T> sDifference(String key, String[] otherKey, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().difference(key, Arrays.asList(otherKey));
        return deserializer(res, clazz);
    }

    /**
     * 返回给定所有集合的差集
     *
     * @param key      key
     * @param otherKey otherKey
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sDifference(String key, String[] otherKey) {
        return sDifference(key, otherKey, Map.class);
    }

    /**
     * 返回给定所有集合的差集并存储在 destKey 中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     */
    public Long sDifferenceAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForSet().differenceAndStore(key, otherKey, destKey);
    }

    /**
     * 返回给定所有集合的差集并存储在 destKey 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long sDifferenceAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForSet().differenceAndStore(key, otherKeys, destKey);
    }

    /**
     * 返回给定所有集合的差集并存储在 destKey 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long sDifferenceAndStore(String key, String[] otherKeys, String destKey) {
        return redisTemplate.opsForSet().differenceAndStore(key, Arrays.asList(otherKeys), destKey);
    }

    /**
     * 返回集合中的所有成员
     *
     * @param key   key
     * @param clazz 返回数据类型
     */
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().members(key);
        return deserializer(res, clazz);
    }

    /**
     * 返回集合中的所有成员
     *
     * @param key key
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sMembers(String key) {
        return sMembers(key, Map.class);
    }

    /**
     * 返回集合中一个或多个随机数
     *
     * @param key key
     */
    public Object sRandomMember(String key) {
        return redisTemplate.opsForSet().randomMember(key);
    }

    /**
     * 从集合中获取不同的随机元素
     *
     * @param key   key
     * @param count 数量
     * @param clazz 返回数据类型
     */
    public <T> Set<T> sDistinctRandomMembers(String key, Number count, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForSet().distinctRandomMembers(key, count.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 从集合中获取不同的随机元素
     *
     * @param key   key
     * @param count 数量
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> sDistinctRandomMembers(String key, Number count) {
        return sDistinctRandomMembers(key, count, Map.class);
    }

    /**
     * 返回集合中一个或多个随机数
     *
     * @param key   key
     * @param count 数量
     * @param clazz 返回数据类型
     */
    public <T> List<T> sRandomMembers(String key, Number count, Class<T> clazz) {
        List<String> res = redisTemplate.opsForSet().randomMembers(key, count.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 返回集合中一个或多个随机数
     *
     * @param key   key
     * @param count 数量
     */
    @SuppressWarnings("rawtypes")
    public List<Map> sRandomMembers(String key, Number count) {
        return sRandomMembers(key, count, Map.class);
    }

    /**
     * 迭代集合中的元素
     *
     * @param key      key
     * @param count    count
     * @param pattern  pattern
     * @param callback 数据迭代回调函数
     */
    public void sScan(String key, Number count, String pattern, ScanCallback<String> callback) throws IOException {
        Assert.notNull(callback, "ScanCallback不能为空");
        ScanOptions scanOptions = ScanOptions.scanOptions().count(count.longValue()).match(pattern).build();
        Cursor<String> cursor = redisTemplate.opsForSet().scan(key, scanOptions);
        cursorForeach(cursor, callback);
    }

    // --------------------------------------------------------------------------------------------
    // Sorted Set 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 向有序集合添加一个或多个成员，或者更新已存在成员的分数
     *
     * @param key   key
     * @param value value
     * @param score score
     */
    public Boolean zsAdd(String key, Object value, Number score) {
        return redisTemplate.opsForZSet().add(key, serializer(value), score.doubleValue());
    }

    /**
     * 向有序集合添加一个或多个成员，或者更新已存在成员的分数
     *
     * @param key    key
     * @param values values
     */
    public Long zsAdd(String key, Collection<ZSetValue> values) {
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(values.size());
        values.forEach(zSetValue -> {
            ZSetOperations.TypedTuple<String> tuple = getZSetValue(zSetValue);
            tuples.add(tuple);
        });
        return redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * 向有序集合添加一个或多个成员，或者更新已存在成员的分数
     *
     * @param key    key
     * @param values values
     */
    public Long zsAdd(String key, ZSetValue[] values) {
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(values.length);
        for (ZSetValue zSetValue : values) {
            ZSetOperations.TypedTuple<String> tuple = getZSetValue(zSetValue);
            tuples.add(tuple);
        }
        return redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * 移除有序集合中的一个或多个成员
     *
     * @param key    key
     * @param values values
     */
    public Long zsRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * 移除有序集合中的一个或多个成员
     *
     * @param key    key
     * @param values values
     */
    public Long zsRemove(String key, Collection<Object> values) {
        return redisTemplate.opsForZSet().remove(key, values.toArray());
    }

    /**
     * 有序集合中对指定成员的分数加上增量 increment
     *
     * @param key   key
     * @param value value
     * @param delta increment
     */
    public Double zsIncrementScore(String key, Object value, Number delta) {
        return redisTemplate.opsForZSet().incrementScore(key, serializer(value), delta.doubleValue());
    }

    /**
     * 返回有序集合中指定成员的索引
     *
     * @param key key
     * @param o   o
     */
    public Long zsRank(String key, Object o) {
        return redisTemplate.opsForZSet().rank(key, o);
    }

    /**
     * 确定元素的索引值在排序集时得分从高到低
     *
     * @param key key
     * @param o   o
     */
    public Long zsReverseRank(String key, Object o) {
        return redisTemplate.opsForZSet().reverseRank(key, o);
    }

    /**
     * 从已排序集获取开始和结束之间的元素
     *
     * @param key   key
     * @param start start
     * @param end   end
     * @param clazz 返回数据类型
     */
    public <T> Set<T> zsRange(String key, Number start, Number end, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForZSet().range(key, start.longValue(), end.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 从已排序集获取开始和结束之间的元素
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsRange(String key, Number start, Number end) {
        return zsRange(key, start, end, Map.class);
    }

    /**
     * 从已排序集获取开始和结束之间的元素
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    public List<ZSetValue> zsRangeWithScores(String key, Number start, Number end) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet().rangeWithScores(key, start.longValue(), end.longValue());
        return zSetToList(set);
    }

    /**
     * 从排序后的集合中获取得分介于最小值和最大值之间的元素
     *
     * @param key   key
     * @param min   min
     * @param max   max
     * @param clazz 返回数据类型
     */
    public <T> Set<T> zsRangeByScore(String key, Number min, Number max, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForZSet().rangeByScore(key, min.doubleValue(), max.doubleValue());
        return deserializer(res, clazz);
    }

    /**
     * 从排序后的集合中获取得分介于最小值和最大值之间的元素
     *
     * @param key key
     * @param min min
     * @param max max
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsRangeByScore(String key, Number min, Number max) {
        return zsRangeByScore(key, min, max, Map.class);
    }

    /**
     * 从排序后的集合中获取得分介于最小值和最大值之间的元素
     *
     * @param key key
     * @param min min
     * @param max max
     */
    public List<ZSetValue> zsRangeByScoreWithScores(String key, Number min, Number max) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet().rangeByScoreWithScores(key, min.doubleValue(), max.doubleValue());
        return zSetToList(set);
    }

    /**
     * 获取从开始到结束的范围内的元素，其中得分在排序集的最小值和最大值之间
     *
     * @param key    key
     * @param min    min
     * @param max    max
     * @param offset offset
     * @param count  count
     * @param clazz  返回数据类型
     */
    public <T> Set<T> zsRangeByScore(String key, Number min, Number max, Number offset, Number count, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForZSet().rangeByScore(key, min.doubleValue(), max.doubleValue(), offset.longValue(), count.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 获取从开始到结束的范围内的元素，其中得分在排序集的最小值和最大值之间
     *
     * @param key    key
     * @param min    min
     * @param max    max
     * @param offset offset
     * @param count  count
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsRangeByScore(String key, Number min, Number max, Number offset, Number count) {
        return zsRangeByScore(key, min, max, offset, count, Map.class);
    }

    /**
     * 获取从开始到结束的范围内的元素，其中得分在排序集的最小值和最大值之间
     *
     * @param key    key
     * @param min    min
     * @param max    max
     * @param offset offset
     * @param count  count
     */
    public List<ZSetValue> zsRangeByScoreWithScores(String key, Number min, Number max, Number offset, Number count) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet().rangeByScoreWithScores(key, min.doubleValue(), max.doubleValue(), offset.longValue(), count.longValue());
        return zSetToList(set);
    }

    /**
     * 获取范围从开始到结束的元素，从高到低排序的集合
     *
     * @param key   key
     * @param start start
     * @param end   end
     * @param clazz 返回数据类型
     */
    public <T> Set<T> zsReverseRange(String key, Number start, Number end, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForZSet().reverseRange(key, start.longValue(), end.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 获取范围从开始到结束的元素，从高到低排序的集合
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsReverseRange(String key, Number start, Number end) {
        return zsReverseRange(key, start, end, Map.class);
    }

    /**
     * 获取范围从开始到结束的元素，从高到低排序的集合
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    public List<ZSetValue> zsReverseRangeWithScores(String key, Number start, Number end) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet().reverseRangeWithScores(key, start.longValue(), end.longValue());
        return zSetToList(set);
    }

    /**
     * 获取得分介于最小值和最大值之间的元素，从高到低排序
     *
     * @param key   key
     * @param min   min
     * @param max   max
     * @param clazz 返回数据类型
     */
    public <T> Set<T> zsReverseRangeByScore(String key, Number min, Number max, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForZSet().reverseRangeByScore(key, min.doubleValue(), max.doubleValue());
        return deserializer(res, clazz);
    }

    /**
     * 获取得分介于最小值和最大值之间的元素，从高到低排序
     *
     * @param key key
     * @param min min
     * @param max max
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsReverseRangeByScore(String key, Number min, Number max) {
        return zsReverseRangeByScore(key, min, max, Map.class);
    }

    /**
     * 获取得分介于最小值和最大值之间的元素，从高到低排序
     *
     * @param key key
     * @param min min
     * @param max max
     */
    public List<ZSetValue> zsReverseRangeByScoreWithScores(String key, Number min, Number max) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min.doubleValue(), max.doubleValue());
        return zSetToList(set);
    }

    /**
     * 获取从开始到结束的范围内的元素，其中得分在最小和最大之间，排序集高 -> 低
     *
     * @param key    key
     * @param min    min
     * @param max    max
     * @param offset offset
     * @param count  count
     * @param clazz  返回数据类型
     */
    public <T> Set<T> zsReverseRangeByScore(String key, Number min, Number max, Number offset, Number count, Class<T> clazz) {
        Set<String> res = redisTemplate.opsForZSet().reverseRangeByScore(key, min.doubleValue(), max.doubleValue(), offset.longValue(), count.longValue());
        return deserializer(res, clazz);
    }

    /**
     * 获取从开始到结束的范围内的元素，其中得分在最小和最大之间，排序集高 -> 低
     *
     * @param key    key
     * @param min    min
     * @param max    max
     * @param offset offset
     * @param count  count
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsReverseRangeByScore(String key, Number min, Number max, Number offset, Number count) {
        return zsReverseRangeByScore(key, min, max, offset, count, Map.class);
    }

    /**
     * 获取从开始到结束的范围内的元素，其中得分在最小和最大之间，排序集高 -> 低
     *
     * @param key    key
     * @param min    min
     * @param max    max
     * @param offset offset
     * @param count  count
     */
    public List<ZSetValue> zsReverseRangeByScoreWithScores(String key, Number min, Number max, Number offset, Number count) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min.doubleValue(), max.doubleValue(), offset.longValue(), count.longValue());
        return zSetToList(set);
    }

    /**
     * 用最小值和最大值之间的值计算排序集中的元素数
     *
     * @param key key
     * @param min min
     * @param max max
     */
    public Long zsCount(String key, Number min, Number max) {
        return redisTemplate.opsForZSet().count(key, min.doubleValue(), max.doubleValue());
    }

    /**
     * 返回按给定键存储的已排序集的元素数
     *
     * @param key key
     */
    public Long zsSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 获取有序集合的成员数
     *
     * @param key key
     */
    public Long zsZCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    /**
     * 返回有序集中，成员的分数值
     *
     * @param key key
     * @param o   o
     */
    public Double zsScore(String key, Object o) {
        return redisTemplate.opsForZSet().score(key, o);
    }

    /**
     * 从按键排序的集合中删除开始和结束之间范围内的元素
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    public Long zsRemoveRange(String key, Number start, Number end) {
        return redisTemplate.opsForZSet().removeRange(key, start.longValue(), end.longValue());
    }

    /**
     * 从按键排序的集合中删除得分在min和max之间的元素
     *
     * @param key key
     * @param min min
     * @param max max
     */
    public Long zsRemoveRangeByScore(String key, Number min, Number max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min.doubleValue(), max.doubleValue());
    }

    /**
     * 计算给定的一个或多个有序集的并集，并存储在新的 destKey 中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     */
    public Long zsUnionAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
    }

    /**
     * 计算给定的一个或多个有序集的并集，并存储在新的 destKey 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long zsUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, destKey);
    }

    /**
     * 计算给定的一个或多个有序集的并集，并存储在新的 destKey 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long zsUnionAndStore(String key, String[] otherKeys, String destKey) {
        return redisTemplate.opsForZSet().unionAndStore(key, Arrays.asList(otherKeys), destKey);
    }

    /**
     * 计算给定的一个或多个有序集的交集并将结果集存储在新的有序集合 key 中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     */
    public Long zsIntersectAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForZSet().intersectAndStore(key, otherKey, destKey);
    }

    /**
     * 计算给定的一个或多个有序集的交集并将结果集存储在新的有序集合 key 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long zsIntersectAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForZSet().intersectAndStore(key, otherKeys, destKey);
    }

    /**
     * 计算给定的一个或多个有序集的交集并将结果集存储在新的有序集合 key 中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     */
    public Long zsIntersectAndStore(String key, String[] otherKeys, String destKey) {
        return redisTemplate.opsForZSet().intersectAndStore(key, Arrays.asList(otherKeys), destKey);
    }

    /**
     * 迭代有序集合中的元素（包括元素成员和元素分值）
     *
     * @param key      key
     * @param count    count
     * @param pattern  pattern
     * @param callback 数据迭代回调函数
     */
    public void zsScan(String key, Number count, String pattern, ScanCallback<ZSetValue> callback) throws IOException {
        ScanOptions scanOptions = ScanOptions.scanOptions().count(count.longValue()).match(pattern).build();
        Cursor<ZSetOperations.TypedTuple<String>> cursor = redisTemplate.opsForZSet().scan(key, scanOptions);
        while (cursor.hasNext()) {
            ZSetOperations.TypedTuple<String> tuple = cursor.next();
            boolean needBreak;
            try {
                needBreak = callback.next(new ZSetValue(tuple.getValue(), tuple.getScore()));
            } catch (Exception e) {
                needBreak = true;
                log.warn(e.getMessage(), e);
            }
            if (needBreak) {
                cursor.close();
                break;
            }
        }
    }

    /**
     * 通过字典区间返回有序集合的成员
     *
     * @param key       key
     * @param minValue  minValue
     * @param equalsMin equalsMin
     * @param maxValue  maxValue
     * @param equalsMax equalsMax
     */
    public <T> Set<T> zsRangeByLex(String key, Object minValue, boolean equalsMin, Object maxValue, boolean equalsMax, Class<T> clazz) {
        RedisZSetCommands.Range range = newRange(minValue, equalsMin, maxValue, equalsMax);
        Set<String> res = redisTemplate.opsForZSet().rangeByLex(key, range);
        return deserializer(res, clazz);
    }

    /**
     * 通过字典区间返回有序集合的成员
     *
     * @param key       key
     * @param minValue  minValue
     * @param equalsMin equalsMin
     * @param maxValue  maxValue
     * @param equalsMax equalsMax
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsRangeByLex(String key, Object minValue, boolean equalsMin, Object maxValue, boolean equalsMax) {
        return zsRangeByLex(key, minValue, equalsMin, maxValue, equalsMax, Map.class);
    }

    /**
     * 通过字典区间返回有序集合的成员
     *
     * @param key       key
     * @param minValue  minValue
     * @param equalsMin equalsMin
     * @param maxValue  maxValue
     * @param equalsMax equalsMax
     * @param count     count
     * @param offset    offset
     * @param clazz     返回数据类型
     */
    public <T> Set<T> zsRangeByLex(String key, Object minValue, boolean equalsMin, Object maxValue, boolean equalsMax, Number count, Number offset, Class<T> clazz) {
        RedisZSetCommands.Range range = newRange(minValue, equalsMin, maxValue, equalsMax);
        RedisZSetCommands.Limit limit;
        if (count != null || offset != null) {
            limit = RedisZSetCommands.Limit.limit();
            if (count != null) {
                limit.count(count.intValue());
            }
            if (offset != null) {
                limit.offset(offset.intValue());
            }
        } else {
            limit = RedisZSetCommands.Limit.unlimited();
        }
        Set<String> res = redisTemplate.opsForZSet().rangeByLex(key, range, limit);
        return deserializer(res, clazz);
    }

    /**
     * 通过字典区间返回有序集合的成员
     *
     * @param key       key
     * @param minValue  minValue
     * @param equalsMin equalsMin
     * @param maxValue  maxValue
     * @param equalsMax equalsMax
     * @param count     count
     * @param offset    offset
     */
    @SuppressWarnings("rawtypes")
    public Set<Map> zsRangeByLex(String key, Object minValue, boolean equalsMin, Object maxValue, boolean equalsMax, Number count, Number offset) {
        return zsRangeByLex(key, minValue, equalsMin, maxValue, equalsMax, count, offset, Map.class);
    }

    // --------------------------------------------------------------------------------------------
    // HyperLogLog  操作
    // --------------------------------------------------------------------------------------------

    /**
     * 添加指定元素到 HyperLogLog 中
     *
     * @param key    key
     * @param values values
     */
    public Long hyperLogLogAdd(String key, Object... values) {
        return redisTemplate.opsForHyperLogLog().add(key, serializer(values).toArray(new String[0]));
    }

    /**
     * 添加指定元素到 HyperLogLog 中
     *
     * @param key    key
     * @param values values
     */
    public Long hyperLogLogAdd(String key, Collection<Object> values) {
        return redisTemplate.opsForHyperLogLog().add(key, serializer(values).toArray(new String[0]));
    }

    /**
     * 获取键中元素的当前数目
     *
     * @param keys keys
     */
    public Long hyperLogLogSize(String... keys) {
        return redisTemplate.opsForHyperLogLog().size(keys);
    }

    /**
     * 获取键中元素的当前数目
     *
     * @param keys keys
     */
    public Long hyperLogLogSize(Collection<String> keys) {
        return redisTemplate.opsForHyperLogLog().size(keys.toArray(new String[0]));
    }

    /**
     * 将多个 HyperLogLog 合并为一个 HyperLogLog
     *
     * @param destination destination
     * @param sourceKeys  sourceKeys
     */
    public Long hyperLogLogUnion(String destination, String... sourceKeys) {
        return redisTemplate.opsForHyperLogLog().union(destination, sourceKeys);
    }

    /**
     * 将多个 HyperLogLog 合并为一个 HyperLogLog
     *
     * @param destination destination
     * @param sourceKeys  sourceKeys
     */
    public Long hyperLogLogUnion(String destination, Collection<String> sourceKeys) {
        return redisTemplate.opsForHyperLogLog().union(destination, sourceKeys.toArray(new String[]{}));
    }

    /**
     * 删除给定的密钥
     *
     * @param key key
     */
    public void hyperLogLogDelete(String key) {
        redisTemplate.opsForHyperLogLog().delete(key);
    }

    // --------------------------------------------------------------------------------------------
    // Geo 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 将指定成员名的点添加到键上
     *
     * @param key    key
     * @param x      x
     * @param y      y
     * @param member member
     */
    public Long geoAdd(String key, Number x, Number y, Object member) {
        return redisTemplate.opsForGeo().add(key, new Point(x.doubleValue(), y.doubleValue()), serializer(member));
    }

    /**
     * 将指定成员名的点添加到键上
     *
     * @param key        key
     * @param pointValue pointValue
     */
    public Long geoAdd(String key, PointValue pointValue) {
        return redisTemplate.opsForGeo().add(key, getGeoLocation(pointValue));
    }

    /**
     * 将指定成员名的点添加到键上
     *
     * @param key         key
     * @param pointValues pointValues
     */
    public Long geoAdd(String key, Collection<PointValue> pointValues) {
        Map<String, Point> memberCoordinateMap = new HashMap<>(pointValues.size());
        pointValues.forEach(pointValue -> {
            RedisGeoCommands.GeoLocation<String> geoLocation = getGeoLocation(pointValue);
            memberCoordinateMap.put(geoLocation.getName(), geoLocation.getPoint());
        });
        return redisTemplate.opsForGeo().add(key, memberCoordinateMap);
    }

    /**
     * 将指定成员名的点添加到键上
     *
     * @param key         key
     * @param pointValues pointValues
     */
    public Long geoAdd(String key, PointValue[] pointValues) {
        Map<String, Point> memberCoordinateMap = new HashMap<>(pointValues.length);
        for (PointValue pointValue : pointValues) {
            RedisGeoCommands.GeoLocation<String> geoLocation = getGeoLocation(pointValue);
            memberCoordinateMap.put(geoLocation.getName(), geoLocation.getPoint());
        }
        return redisTemplate.opsForGeo().add(key, memberCoordinateMap);
    }

    /**
     * 返回两个给定位置之间的距离
     *
     * @param key     key
     * @param member1 member1
     * @param member2 member2
     */
    public Distance geoDistance(String key, Object member1, Object member2) {
        return redisTemplate.opsForGeo().distance(key, serializer(member1), serializer(member2));
    }

    /**
     * 获取一个或多个成员位置的GeoHash表示
     *
     * @param key     key
     * @param members members
     */
    public List<String> geoHash(String key, Object... members) {
        return redisTemplate.opsForGeo().hash(key, serializer(members).toArray(new String[0]));
    }

    /**
     * 获取一个或多个成员位置的GeoHash表示
     *
     * @param key     key
     * @param members members
     */
    public List<String> geoHash(String key, Collection<Object> members) {
        return redisTemplate.opsForGeo().hash(key, serializer(members).toArray(new String[0]));
    }

    /**
     * 获取一个或多个成员的位置的点表示
     *
     * @param key     key
     * @param members members
     */
    public List<Point> geoPosition(String key, Object... members) {
        return redisTemplate.opsForGeo().position(key, serializer(members).toArray(new String[0]));
    }

    /**
     * 获取一个或多个成员的位置的点表示
     *
     * @param key     key
     * @param members members
     */
    public List<Point> geoPosition(String key, Collection<Object> members) {
        return redisTemplate.opsForGeo().position(key, serializer(members).toArray(new String[0]));
    }

    // --------------------------------------------------------------------------------------------
    // RedisTemplate 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 执行给定的 {@link RedisScript}
     *
     * @param script 要执行的脚本
     * @param keys   任何需要传递给脚本的键
     * @param args   任何需要传递给脚本的参数
     * @return 脚本的返回值，如果 {@link RedisScript#getResultType()} 为 null，则为 null，可能表示一次性状态回复（即“OK”）
     */
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    /**
     * 基于当前的Redis连接配置创建新的RedisTemplate实例(共享底层连接池)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V> RedisTemplate<K, V> createRedisTemplate(Consumer<RedisTemplate<K, V>> customizer) {
        RedisTemplate template = new RedisTemplate();
        template.setConnectionFactory(connectionFactory);
        if (customizer != null) {
            customizer.accept(template);
        }
        template.afterPropertiesSet();
        return template;
    }

    // --------------------------------------------------------------------------------------------
    // 全局限流
    // --------------------------------------------------------------------------------------------

    /**
     * 基于 Lua 脚本的限流实现
     *
     * @param reqId            请求ID
     * @param rateLimitConfigs 限流配置
     * @return 限流结果
     */
    public List<RateLimitState> rateLimit(String reqId, List<RateLimitConfig> rateLimitConfigs) {
        return doRateLimit(reqId, rateLimitConfigs);
    }

    @SuppressWarnings("unchecked")
    private List<RateLimitState> doRateLimit(String reqId, List<RateLimitConfig> rateLimitConfigs) {
        checkConfig(rateLimitConfigs);
        List<RateLimitState> resList = new ArrayList<>(rateLimitConfigs.size());
        List<String> keys = new ArrayList<>(rateLimitConfigs.size() + 1);
        List<String> args = new ArrayList<>(rateLimitConfigs.size() * 2 + 1);
        keys.add(getTimestampKey(reqId, rateLimitConfigs));
        args.add(String.valueOf(Instant.now().getEpochSecond()));
        for (RateLimitConfig rateLimitConfig : rateLimitConfigs) {
            keys.add(getTokensKey(reqId, rateLimitConfig));
            args.add(String.valueOf(rateLimitConfig.getTimes()));
            args.add(String.valueOf(rateLimitConfig.getLimit()));
        }
        List<List<Long>> results = null;
        try {
            results = (List<List<Long>>) redisTemplate.execute(RATE_LIMIT_SCRIPT, keys, args.toArray());
        } catch (Exception e) {
            log.error("执行限流lua脚本失败", e);
            redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                for (String key : keys) {
                    connection.del(RedisSerializer.string().serialize(key));
                }
                return null;
            });
        }
        // if (log.isDebugEnabled()) {
        //     log.debug("[{}] results -> {}", reqId, results);
        // }
        for (int i = 0; i < rateLimitConfigs.size(); i++) {
            assert results != null;
            List<Long> result = results.get(i);
            RateLimitConfig rateLimiterConfig = rateLimitConfigs.get(i);
            RateLimitState rateLimiterRes = new RateLimitState();
            rateLimiterRes.setConfig(rateLimiterConfig);
            rateLimiterRes.setLimited(Objects.equals(result.get(0), 1L));
            rateLimiterRes.setLeft(result.get(1));
            resList.add(rateLimiterRes);
        }
        return resList;
    }

    private void checkConfig(List<RateLimitConfig> rateLimitConfigs) {
        if (rateLimitConfigs == null || rateLimitConfigs.isEmpty()) {
            throw new IllegalArgumentException("参数 rateLimitConfigs 不能是null或空");
        }
        for (RateLimitConfig rateLimitConfig : rateLimitConfigs) {
            if (rateLimitConfig == null) {
                throw new IllegalArgumentException("参数 rateLimitConfigs 不能包含null元素");
            }
            if (rateLimitConfig.getLimit() <= 0 || rateLimitConfig.getTimes() <= 0) {
                throw new IllegalArgumentException("RateLimitConfig 配置 times、limit 必须大于0");
            }
        }
    }

    private String getTimestampKey(String reqId, List<RateLimitConfig> rateLimiterConfigList) {
        StringBuilder sb = new StringBuilder();
        for (RateLimitConfig rateLimiterConfig : rateLimiterConfigList) {
            sb.append(rateLimiterConfig.getTimes()).append("-").append(rateLimiterConfig.getLimit()).append(".");
        }
        // request_rate_limiter.%s.{%s}.timestamp
        return String.format("rrl.%s{%s}.tt", sb, reqId);
    }

    private String getTokensKey(String reqId, RateLimitConfig config) {
        // request_rate_limiter.%s-%s.{%s}.tokens
        return String.format("rrl.%s-%s.{%s}.t", config.getTimes(), config.getLimit(), reqId);
    }

    // --------------------------------------------------------------------------------------------
    // Redisson 分布式锁功能
    // --------------------------------------------------------------------------------------------

    /**
     * 使用基于Redis实现的可重入锁，保证 {@code callback} 回调逻辑串行执行
     *
     * @param lockName 锁名称
     * @param callback 保证串行执行的回调函数
     */
    public <T> T lock(String lockName, Supplier<T> callback) {
        Assert.hasText(lockName, "参数 lockName 不能为空");
        Assert.notNull(callback, "参数 callback 不能为null");
        RLock lock = redisson.getFairLock(lockName);
        try {
            lock.lock();
            return callback.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 尝试获取锁，如果锁获取失败则不执行 {@code callback} <br />
     * 使用基于Redis实现的可重入锁，保证 {@code callback} 回调逻辑串行执行
     *
     * @param lockName 锁名称
     * @param callback 保证串行执行的回调函数
     * @return 二元组对象 {@code TupleTwo<是否获取到Redis锁, callback函数的返回值>}
     */
    public <T> TupleTwo<Boolean, T> tryLock(String lockName, Supplier<T> callback) {
        Assert.hasText(lockName, "参数 lockName 不能为空");
        Assert.notNull(callback, "参数 callback 不能为null");
        RLock lock = redisson.getFairLock(lockName);
        boolean flag = false;
        try {
            flag = lock.tryLock();
            T result = null;
            if (flag) {
                result = callback.get();
            }
            return TupleTwo.creat(flag, result);
        } finally {
            if (flag) {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试获取锁，如果锁获取失败则不执行 {@code callback} <br />
     * 使用基于Redis实现的可重入锁，保证 {@code callback} 回调逻辑串行执行
     *
     * @param lockName 锁名称
     * @param waitTime 等待锁定的最长时间(毫秒)
     * @param callback 保证串行执行的回调函数
     * @return 二元组对象 {@code TupleTwo<是否获取到Redis锁, callback函数的返回值>}
     */
    public <T> TupleTwo<Boolean, T> tryLock(String lockName, long waitTime, Supplier<T> callback) {
        Assert.hasText(lockName, "参数 lockName 不能为空");
        Assert.notNull(callback, "参数 callback 不能为null");
        RLock lock = redisson.getFairLock(lockName);
        boolean flag = false;
        try {
            flag = lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
            T result = null;
            if (flag) {
                result = callback.get();
            }
            return TupleTwo.creat(flag, result);
        } catch (InterruptedException e) {
            log.warn("RLock.tryLock 被中断", e);
            return TupleTwo.creat(false, null);
        } finally {
            if (flag) {
                lock.unlock();
            }
        }
    }

    /**
     * 使用基于Redis实现的可重入锁，可设置最大锁定时间
     *
     * @param lockName    锁名称
     * @param lockMaxTime 最大的锁定时间(毫秒)
     * @param callback    得到锁后需要执行的回调
     */
    public <T> T lockMaxTime(String lockName, long lockMaxTime, Supplier<T> callback) {
        Assert.hasText(lockName, "参数 lockName 不能为空");
        Assert.notNull(callback, "参数 callback 不能为null");
        RLock lock = redisson.getFairLock(lockName);
        try {
            lock.lock(lockMaxTime, TimeUnit.MILLISECONDS);
            return callback.get();
        } finally {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.warn("已经自动释放Redis锁: {}", e.getMessage());
            }
        }
    }

    /**
     * 尝试获取锁，如果锁获取失败则不执行 {@code callback}，可设置最大锁定时间 <br />
     *
     * @param lockName    锁名称
     * @param waitTime    等待锁定的最长时间(毫秒)
     * @param lockMaxTime 最大的锁定时间(毫秒)
     * @param callback    保证串行执行的回调函数
     * @return 二元组对象 {@code TupleTwo<是否获取到Redis锁, callback函数的返回值>}
     */
    public <T> TupleTwo<Boolean, T> tryLockMaxTime(String lockName, long waitTime, long lockMaxTime, Supplier<T> callback) {
        Assert.hasText(lockName, "参数 lockName 不能为空");
        Assert.notNull(callback, "参数 callback 不能为null");
        RLock lock = redisson.getFairLock(lockName);
        try {
            boolean flag = lock.tryLock(waitTime, lockMaxTime, TimeUnit.MILLISECONDS);
            T result = null;
            if (flag) {
                result = callback.get();
            }
            return TupleTwo.creat(flag, result);
        } catch (InterruptedException e) {
            log.warn("RLock.tryLock 被中断", e);
            return TupleTwo.creat(false, null);
        } finally {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.warn("已经自动释放Redis锁: {}", e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // 其它 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 获取 Redis 节点信息，获取失败返回 null
     */
    public RedisInfo getInfo() {
        return RedisUtils.getRedisInfo(redisTemplate);
    }

    /**
     * 获取当前 Redis 连接池状态，未使用连接池返回 null
     */
    public RedisPoolStatus getPoolStatus() {
        return RedisUtils.getPoolStatus(redisTemplate);
    }

    // --------------------------------------------------------------------------------------------
    //  内部函数
    // --------------------------------------------------------------------------------------------

    /**
     * 迭代Cursor数据
     */
    private <T> void cursorForeach(Cursor<T> cursor, ScanCallback<T> callback) throws IOException {
        while (cursor.hasNext()) {
            T entry = cursor.next();
            boolean needBreak;
            try {
                needBreak = callback.next(entry);
            } catch (Exception e) {
                needBreak = true;
                log.warn(e.getMessage(), e);
            }
            if (needBreak) {
                cursor.close();
                break;
            }
        }
    }

    private <T> List<ZSetValue> zSetToList(Set<ZSetOperations.TypedTuple<T>> set) {
        if (set == null) {
            return new ArrayList<>();
        }
        List<ZSetValue> result = new ArrayList<>(set.size());
        for (ZSetOperations.TypedTuple<T> typedTuple : set) {
            result.add(new ZSetValue(typedTuple.getValue(), typedTuple.getScore()));
        }
        return result;
    }

    private RedisZSetCommands.Range newRange(Object minValue, boolean equalsMin, Object maxValue, boolean equalsMax) {
        RedisZSetCommands.Range range = RedisZSetCommands.Range.range();
        if (minValue != null) {
            if (equalsMin) {
                range.gte(minValue);
            } else {
                range.gt(minValue);
            }
        }
        if (maxValue != null) {
            if (equalsMax) {
                range.lte(maxValue);
            } else {
                range.lt(maxValue);
            }
        }
        return range;
    }

    private RedisGeoCommands.GeoLocation<String> getGeoLocation(PointValue pointValue) {
        Assert.notNull(pointValue, "PointValue不能为空");
        return new RedisGeoCommands.GeoLocation<>(serializer(pointValue.getValue()), new Point(pointValue.getX(), pointValue.getY()));
    }

    private ZSetOperations.TypedTuple<String> getZSetValue(ZSetValue zSetValue) {
        Assert.notNull(zSetValue, "ZSetValue不能为空");
        return new DefaultTypedTuple<>(serializer(zSetValue.getValue()), zSetValue.getScore());
    }

    private String serializer(Object value) {
        return jacksonMapper.toJson(value);
    }

    private Map<String, String> serializer(Map<String, Object> values) {
        if (values == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>(values.size());
        values.forEach((k, v) -> map.put(k, jacksonMapper.toJson(v)));
        return map;
    }

    private List<String> serializer(Object... values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(values.length);
        for (Object value : values) {
            list.add(jacksonMapper.toJson(value));
        }
        return list;
    }

    private List<String> serializer(Collection<Object> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(values.size());
        for (Object value : values) {
            list.add(jacksonMapper.toJson(value));
        }
        return list;
    }

    private <T> T deserializer(String value, Class<T> clazz) {
        return jacksonMapper.fromJson(value, clazz);
    }

    private <T> List<T> deserializer(List<String> values, Class<T> clazz) {
        if (values == null) {
            return null;
        }
        List<T> res = new ArrayList<>(values.size());
        for (String value : values) {
            res.add(jacksonMapper.fromJson(value, clazz));
        }
        return res;
    }

    private <T> Set<T> deserializer(Set<String> values, Class<T> clazz) {
        if (values == null) {
            return null;
        }
        Set<T> res = new HashSet<>(values.size());
        for (String value : values) {
            res.add(jacksonMapper.fromJson(value, clazz));
        }
        return res;
    }
}
