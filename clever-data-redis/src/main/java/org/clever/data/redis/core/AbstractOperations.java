package org.clever.data.redis.core;

import org.clever.data.geo.GeoResults;
import org.clever.data.redis.connection.DefaultTuple;
import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.clever.data.redis.connection.RedisZSetCommands.Tuple;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.data.redis.core.ZSetOperations.TypedTuple;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.data.redis.serializer.SerializationUtils;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;

import java.util.*;

/**
 * 各种 RedisTemplate XXXOperations 实现使用的内部基类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:48 <br/>
 */
abstract class AbstractOperations<K, V> {
    // 模板内部方法的实用方法
    abstract class ValueDeserializingRedisCallback implements RedisCallback<V> {
        private final Object key;

        public ValueDeserializingRedisCallback(Object key) {
            this.key = key;
        }

        public final V doInRedis(RedisConnection connection) {
            byte[] result = inRedis(rawKey(key), connection);
            return deserializeValue(result);
        }

        protected abstract byte[] inRedis(byte[] rawKey, RedisConnection connection);
    }

    final RedisTemplate<K, V> template;

    AbstractOperations(RedisTemplate<K, V> template) {
        this.template = template;
    }

    @SuppressWarnings("rawtypes")
    RedisSerializer keySerializer() {
        return template.getKeySerializer();
    }

    @SuppressWarnings("rawtypes")
    RedisSerializer valueSerializer() {
        return template.getValueSerializer();
    }

    @SuppressWarnings("rawtypes")
    RedisSerializer hashKeySerializer() {
        return template.getHashKeySerializer();
    }

    @SuppressWarnings("rawtypes")
    RedisSerializer hashValueSerializer() {
        return template.getHashValueSerializer();
    }

    @SuppressWarnings("rawtypes")
    RedisSerializer stringSerializer() {
        return template.getStringSerializer();
    }

    <T> T execute(RedisCallback<T> callback) {
        return template.execute(callback, true);
    }

    public RedisOperations<K, V> getOperations() {
        return template;
    }

    @SuppressWarnings("unchecked")
    byte[] rawKey(Object key) {
        Assert.notNull(key, "non null key required");
        if (keySerializer() == null && key instanceof byte[]) {
            return (byte[]) key;
        }
        return keySerializer().serialize(key);
    }

    @SuppressWarnings("unchecked")
    byte[] rawString(String key) {
        return stringSerializer().serialize(key);
    }

    @SuppressWarnings("unchecked")
    byte[] rawValue(Object value) {
        if (valueSerializer() == null && value instanceof byte[]) {
            return (byte[]) value;
        }
        return valueSerializer().serialize(value);
    }

    byte[][] rawValues(Object... values) {
        byte[][] rawValues = new byte[values.length][];
        int i = 0;
        for (Object value : values) {
            rawValues[i++] = rawValue(value);
        }
        return rawValues;
    }

    /**
     * @param values 不能是 {@literal empty} 也不能包含 {@literal null} 值
     */
    byte[][] rawValues(Collection<V> values) {
        Assert.notEmpty(values, "Values must not be 'null' or empty.");
        Assert.noNullElements(values.toArray(), "Values must not contain 'null' value.");
        byte[][] rawValues = new byte[values.size()][];
        int i = 0;
        for (V value : values) {
            rawValues[i++] = rawValue(value);
        }
        return rawValues;
    }

    @SuppressWarnings("unchecked")
    <HK> byte[] rawHashKey(HK hashKey) {
        Assert.notNull(hashKey, "non null hash key required");
        if (hashKeySerializer() == null && hashKey instanceof byte[]) {
            return (byte[]) hashKey;
        }
        return hashKeySerializer().serialize(hashKey);
    }

    @SuppressWarnings("unchecked")
    <HK> byte[][] rawHashKeys(HK... hashKeys) {
        byte[][] rawHashKeys = new byte[hashKeys.length][];
        int i = 0;
        for (HK hashKey : hashKeys) {
            rawHashKeys[i++] = rawHashKey(hashKey);
        }
        return rawHashKeys;
    }

    @SuppressWarnings("unchecked")
    <HV> byte[] rawHashValue(HV value) {
        if (hashValueSerializer() == null && value instanceof byte[]) {
            return (byte[]) value;
        }
        return hashValueSerializer().serialize(value);
    }

    byte[][] rawKeys(K key, K otherKey) {
        byte[][] rawKeys = new byte[2][];
        rawKeys[0] = rawKey(key);
        rawKeys[1] = rawKey(key);
        return rawKeys;
    }

    byte[][] rawKeys(Collection<K> keys) {
        return rawKeys(null, keys);
    }

    byte[][] rawKeys(K key, Collection<K> keys) {
        byte[][] rawKeys = new byte[keys.size() + (key != null ? 1 : 0)][];
        int i = 0;
        if (key != null) {
            rawKeys[i++] = rawKey(key);
        }
        for (K k : keys) {
            rawKeys[i++] = rawKey(k);
        }
        return rawKeys;
    }

    @SuppressWarnings("unchecked")
    Set<V> deserializeValues(Set<byte[]> rawValues) {
        if (valueSerializer() == null) {
            return (Set<V>) rawValues;
        }
        return SerializationUtils.deserialize(rawValues, valueSerializer());
    }

    Set<TypedTuple<V>> deserializeTupleValues(Set<Tuple> rawValues) {
        if (rawValues == null) {
            return null;
        }
        Set<TypedTuple<V>> set = new LinkedHashSet<>(rawValues.size());
        for (Tuple rawValue : rawValues) {
            set.add(deserializeTuple(rawValue));
        }
        return set;
    }

    List<TypedTuple<V>> deserializeTupleValues(List<Tuple> rawValues) {
        if (rawValues == null) {
            return null;
        }
        List<TypedTuple<V>> set = new ArrayList<>(rawValues.size());
        for (Tuple rawValue : rawValues) {
            set.add(deserializeTuple(rawValue));
        }
        return set;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    TypedTuple<V> deserializeTuple(Tuple tuple) {
        if (tuple == null) {
            return null;
        }
        Object value = tuple.getValue();
        if (valueSerializer() != null) {
            value = valueSerializer().deserialize(tuple.getValue());
        }
        return new DefaultTypedTuple(value, tuple.getScore());
    }

    @SuppressWarnings("unchecked")
    Set<Tuple> rawTupleValues(Set<TypedTuple<V>> values) {
        if (values == null) {
            return null;
        }
        Set<Tuple> rawTuples = new LinkedHashSet<>(values.size());
        for (TypedTuple<V> value : values) {
            byte[] rawValue;
            if (valueSerializer() == null && value.getValue() instanceof byte[]) {
                rawValue = (byte[]) value.getValue();
            } else {
                rawValue = valueSerializer().serialize(value.getValue());
            }
            rawTuples.add(new DefaultTuple(rawValue, value.getScore()));
        }
        return rawTuples;
    }

    @SuppressWarnings("unchecked")
    List<V> deserializeValues(List<byte[]> rawValues) {
        if (valueSerializer() == null) {
            return (List<V>) rawValues;
        }
        return SerializationUtils.deserialize(rawValues, valueSerializer());
    }

    @SuppressWarnings("unchecked")
    <T> Set<T> deserializeHashKeys(Set<byte[]> rawKeys) {
        if (hashKeySerializer() == null) {
            return (Set<T>) rawKeys;
        }
        return SerializationUtils.deserialize(rawKeys, hashKeySerializer());
    }

    @SuppressWarnings("unchecked")
    <T> List<T> deserializeHashKeys(List<byte[]> rawKeys) {
        if (hashKeySerializer() == null) {
            return (List<T>) rawKeys;
        }
        return SerializationUtils.deserialize(rawKeys, hashKeySerializer());
    }

    @SuppressWarnings("unchecked")
    <T> List<T> deserializeHashValues(List<byte[]> rawValues) {
        if (hashValueSerializer() == null) {
            return (List<T>) rawValues;
        }
        return SerializationUtils.deserialize(rawValues, hashValueSerializer());
    }

    @SuppressWarnings("unchecked")
    <HK, HV> Map<HK, HV> deserializeHashMap(Map<byte[], byte[]> entries) {
        // 管道连接/多模式
        if (entries == null) {
            return null;
        }
        Map<HK, HV> map = new LinkedHashMap<>(entries.size());
        for (Map.Entry<byte[], byte[]> entry : entries.entrySet()) {
            map.put((HK) deserializeHashKey(entry.getKey()), (HV) deserializeHashValue(entry.getValue()));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    K deserializeKey(byte[] value) {
        if (keySerializer() == null) {
            return (K) value;
        }
        return (K) keySerializer().deserialize(value);
    }

    Set<K> deserializeKeys(Set<byte[]> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptySet();
        }
        Set<K> result = new LinkedHashSet<>(keys.size());
        for (byte[] key : keys) {
            result.add(deserializeKey(key));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    V deserializeValue(byte[] value) {
        if (valueSerializer() == null) {
            return (V) value;
        }
        return (V) valueSerializer().deserialize(value);
    }

    String deserializeString(byte[] value) {
        return (String) stringSerializer().deserialize(value);
    }

    @SuppressWarnings({"unchecked"})
    <HK> HK deserializeHashKey(byte[] value) {
        if (hashKeySerializer() == null) {
            return (HK) value;
        }
        return (HK) hashKeySerializer().deserialize(value);
    }

    @SuppressWarnings("unchecked")
    <HV> HV deserializeHashValue(byte[] value) {
        if (hashValueSerializer() == null) {
            return (HV) value;
        }
        return (HV) hashValueSerializer().deserialize(value);
    }

    /**
     * 反序列化 {@link GeoResults} 的 {@link GeoLocation}
     *
     * @param source 可以是 {@literal null}。
     * @return 转换或 {@literal null}
     */
    @SuppressWarnings("unchecked")
    GeoResults<GeoLocation<V>> deserializeGeoResults(GeoResults<GeoLocation<byte[]>> source) {
        if (source == null) {
            return null;
        }
        if (valueSerializer() == null) {
            return (GeoResults<GeoLocation<V>>) (Object) source;
        }
        return Converters.deserializingGeoResultsConverter((RedisSerializer<V>) valueSerializer()).convert(source);
    }
}
