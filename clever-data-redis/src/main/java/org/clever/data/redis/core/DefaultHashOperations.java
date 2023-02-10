package org.clever.data.redis.core;

import org.clever.core.convert.converter.Converter;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.util.Assert;

import java.util.*;
import java.util.Map.Entry;

/**
 * {@link HashOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:28 <br/>
 */
class DefaultHashOperations<K, HK, HV> extends AbstractOperations<K, Object> implements HashOperations<K, HK, HV> {
    @SuppressWarnings("unchecked")
    DefaultHashOperations(RedisTemplate<K, ?> template) {
        super((RedisTemplate<K, Object>) template);
    }

    @Override
    public HV get(K key, Object hashKey) {
        byte[] rawKey = rawKey(key);
        byte[] rawHashKey = rawHashKey(hashKey);
        byte[] rawHashValue = execute(connection -> connection.hGet(rawKey, rawHashKey));
        return rawHashValue != null ? deserializeHashValue(rawHashValue) : null;
    }

    @Override
    public Boolean hasKey(K key, Object hashKey) {
        byte[] rawKey = rawKey(key);
        byte[] rawHashKey = rawHashKey(hashKey);
        return execute(connection -> connection.hExists(rawKey, rawHashKey));
    }

    @Override
    public Long increment(K key, HK hashKey, long delta) {
        byte[] rawKey = rawKey(key);
        byte[] rawHashKey = rawHashKey(hashKey);
        return execute(connection -> connection.hIncrBy(rawKey, rawHashKey, delta));
    }

    @Override
    public Double increment(K key, HK hashKey, double delta) {
        byte[] rawKey = rawKey(key);
        byte[] rawHashKey = rawHashKey(hashKey);
        return execute(connection -> connection.hIncrBy(rawKey, rawHashKey, delta));
    }

    @Override
    public HK randomKey(K key) {
        byte[] rawKey = rawKey(key);
        return deserializeHashKey(execute(connection -> connection.hRandField(rawKey)));
    }

    @Override
    public Entry<HK, HV> randomEntry(K key) {
        byte[] rawKey = rawKey(key);
        Entry<byte[], byte[]> rawEntry = execute(connection -> connection.hRandFieldWithValues(rawKey));
        return rawEntry == null ? null : Converters.entryOf(deserializeHashKey(rawEntry.getKey()), deserializeHashValue(rawEntry.getValue()));
    }

    @Override
    public List<HK> randomKeys(K key, long count) {
        byte[] rawKey = rawKey(key);
        List<byte[]> rawValues = execute(connection -> connection.hRandField(rawKey, count));
        return deserializeHashKeys(rawValues);
    }

    @Override
    public Map<HK, HV> randomEntries(K key, long count) {
        Assert.isTrue(count > 0, "Count must not be negative");
        byte[] rawKey = rawKey(key);
        List<Entry<byte[], byte[]>> rawEntries = execute(connection -> connection.hRandFieldWithValues(rawKey, count));
        if (rawEntries == null) {
            return null;
        }
        Map<byte[], byte[]> rawMap = new LinkedHashMap<>(rawEntries.size());
        rawEntries.forEach(entry -> rawMap.put(entry.getKey(), entry.getValue()));
        return deserializeHashMap(rawMap);
    }

    @Override
    public Set<HK> keys(K key) {
        byte[] rawKey = rawKey(key);
        Set<byte[]> rawValues = execute(connection -> connection.hKeys(rawKey));
        return rawValues != null ? deserializeHashKeys(rawValues) : Collections.emptySet();
    }

    @Override
    public Long size(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.hLen(rawKey));
    }

    @Override
    public Long lengthOfValue(K key, HK hashKey) {
        byte[] rawKey = rawKey(key);
        byte[] rawHashKey = rawHashKey(hashKey);
        return execute(connection -> connection.hStrLen(rawKey, rawHashKey));
    }

    @Override
    public void putAll(K key, Map<? extends HK, ? extends HV> m) {
        if (m.isEmpty()) {
            return;
        }
        byte[] rawKey = rawKey(key);
        Map<byte[], byte[]> hashes = new LinkedHashMap<>(m.size());
        for (Map.Entry<? extends HK, ? extends HV> entry : m.entrySet()) {
            hashes.put(rawHashKey(entry.getKey()), rawHashValue(entry.getValue()));
        }
        execute(connection -> {
            connection.hMSet(rawKey, hashes);
            return null;
        });
    }

    @Override
    public List<HV> multiGet(K key, Collection<HK> fields) {
        if (fields.isEmpty()) {
            return Collections.emptyList();
        }
        byte[] rawKey = rawKey(key);
        byte[][] rawHashKeys = new byte[fields.size()][];
        int counter = 0;
        for (HK hashKey : fields) {
            rawHashKeys[counter++] = rawHashKey(hashKey);
        }
        List<byte[]> rawValues = execute(connection -> connection.hMGet(rawKey, rawHashKeys));
        return deserializeHashValues(rawValues);
    }

    @Override
    public void put(K key, HK hashKey, HV value) {
        byte[] rawKey = rawKey(key);
        byte[] rawHashKey = rawHashKey(hashKey);
        byte[] rawHashValue = rawHashValue(value);
        execute(connection -> {
            connection.hSet(rawKey, rawHashKey, rawHashValue);
            return null;
        });
    }

    @Override
    public Boolean putIfAbsent(K key, HK hashKey, HV value) {
        byte[] rawKey = rawKey(key);
        byte[] rawHashKey = rawHashKey(hashKey);
        byte[] rawHashValue = rawHashValue(value);
        return execute(connection -> connection.hSetNX(rawKey, rawHashKey, rawHashValue));
    }

    @Override
    public List<HV> values(K key) {
        byte[] rawKey = rawKey(key);
        List<byte[]> rawValues = execute(connection -> connection.hVals(rawKey));
        return rawValues != null ? deserializeHashValues(rawValues) : Collections.emptyList();
    }

    @Override
    public Long delete(K key, Object... hashKeys) {
        byte[] rawKey = rawKey(key);
        byte[][] rawHashKeys = rawHashKeys(hashKeys);
        return execute(connection -> connection.hDel(rawKey, rawHashKeys));
    }

    @Override
    public Map<HK, HV> entries(K key) {
        byte[] rawKey = rawKey(key);
        Map<byte[], byte[]> entries = execute(connection -> connection.hGetAll(rawKey));
        return entries != null ? deserializeHashMap(entries) : Collections.emptyMap();
    }

    @Override
    public Cursor<Entry<HK, HV>> scan(K key, ScanOptions options) {
        byte[] rawKey = rawKey(key);
        return template.executeWithStickyConnection((RedisCallback<Cursor<Entry<HK, HV>>>) connection -> new ConvertingCursor<>(
                connection.hScan(rawKey, options),
                (Converter<Entry<byte[], byte[]>, Entry<HK, HV>>) source -> Converters.entryOf(deserializeHashKey(source.getKey()), deserializeHashValue(source.getValue()))
        ));
    }
}
