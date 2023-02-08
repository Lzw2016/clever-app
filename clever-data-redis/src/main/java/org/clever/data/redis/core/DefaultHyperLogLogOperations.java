package org.clever.data.redis.core;

import java.util.Arrays;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:55 <br/>
 */
class DefaultHyperLogLogOperations<K, V> extends AbstractOperations<K, V> implements HyperLogLogOperations<K, V> {
    DefaultHyperLogLogOperations(RedisTemplate<K, V> template) {
        super(template);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Long add(K key, V... values) {
        byte[] rawKey = rawKey(key);
        // noinspection ConfusingArgumentToVarargsMethod
        byte[][] rawValues = rawValues(values);
        return execute(connection -> connection.pfAdd(rawKey, rawValues));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long size(K... keys) {
        byte[][] rawKeys = rawKeys(Arrays.asList(keys));
        return execute(connection -> connection.pfCount(rawKeys));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long union(K destination, K... sourceKeys) {
        byte[] rawDestinationKey = rawKey(destination);
        byte[][] rawSourceKeys = rawKeys(Arrays.asList(sourceKeys));
        return execute(connection -> {
            connection.pfMerge(rawDestinationKey, rawSourceKeys);
            return connection.pfCount(rawDestinationKey);
        });
    }

    @Override
    public void delete(K key) {
        template.delete(key);
    }
}
