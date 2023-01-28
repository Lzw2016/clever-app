package org.clever.data.redis.serializer;

import org.clever.core.CollectionFactory;

import java.util.*;

/**
 * 具有各种序列化相关方法的实用程序类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:57 <br/>
 */
public abstract class SerializationUtils {
    static final byte[] EMPTY_ARRAY = new byte[0];

    static boolean isEmpty(byte[] data) {
        return (data == null || data.length == 0);
    }

    @SuppressWarnings("unchecked")
    static <T extends Collection<?>> T deserializeValues(Collection<byte[]> rawValues, Class<T> type, RedisSerializer<?> redisSerializer) {
        // connection in pipeline/multi mode
        if (rawValues == null) {
            return (T) CollectionFactory.createCollection(type, 0);
        }
        Collection<Object> values = (List.class.isAssignableFrom(type) ? new ArrayList<>(rawValues.size()) : new LinkedHashSet<>(rawValues.size()));
        for (byte[] bs : rawValues) {
            values.add(redisSerializer.deserialize(bs));
        }
        return (T) values;
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> deserialize(Set<byte[]> rawValues, RedisSerializer<T> redisSerializer) {
        return deserializeValues(rawValues, Set.class, redisSerializer);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> deserialize(List<byte[]> rawValues, RedisSerializer<T> redisSerializer) {
        return deserializeValues(rawValues, List.class, redisSerializer);
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> deserialize(Collection<byte[]> rawValues, RedisSerializer<T> redisSerializer) {
        return deserializeValues(rawValues, List.class, redisSerializer);
    }

    public static <T> Map<T, T> deserialize(Map<byte[], byte[]> rawValues, RedisSerializer<T> redisSerializer) {
        if (rawValues == null) {
            return Collections.emptyMap();
        }
        Map<T, T> ret = new LinkedHashMap<>(rawValues.size());
        for (Map.Entry<byte[], byte[]> entry : rawValues.entrySet()) {
            ret.put(redisSerializer.deserialize(entry.getKey()), redisSerializer.deserialize(entry.getValue()));
        }
        return ret;
    }

    public static <HK, HV> Map<HK, HV> deserialize(Map<byte[], byte[]> rawValues, RedisSerializer<HK> hashKeySerializer, RedisSerializer<HV> hashValueSerializer) {
        if (rawValues == null) {
            return Collections.emptyMap();
        }
        Map<HK, HV> map = new LinkedHashMap<>(rawValues.size());
        for (Map.Entry<byte[], byte[]> entry : rawValues.entrySet()) {
            // 可能只想反序列化键或值
            HK key = hashKeySerializer != null ? hashKeySerializer.deserialize(entry.getKey()) : (HK) entry.getKey();
            HV value = hashValueSerializer != null ? hashValueSerializer.deserialize(entry.getValue()) : (HV) entry.getValue();
            map.put(key, value);
        }
        return map;
    }
}
