package org.clever.data.redis.core;

import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.connection.RedisListCommands.Direction;
import org.clever.data.redis.connection.RedisListCommands.Position;
import org.clever.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link ListOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:49 <br/>
 */
class DefaultListOperations<K, V> extends AbstractOperations<K, V> implements ListOperations<K, V> {
    DefaultListOperations(RedisTemplate<K, V> template) {
        super(template);
    }

    @Override
    public V index(K key, long index) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.lIndex(rawKey, index);
            }
        });
    }

    @Override
    public Long indexOf(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.lPos(rawKey, rawValue));
    }

    @Override
    public Long lastIndexOf(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> {
            List<Long> indexes = connection.lPos(rawKey, rawValue, -1, null);
            return CollectionUtils.firstElement(indexes);
        });
    }

    @Override
    public V leftPop(K key) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.lPop(rawKey);
            }
        });
    }

    @Override
    public List<V> leftPop(K key, long count) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> deserializeValues(connection.lPop(rawKey, count)));
    }

    @Override
    public V leftPop(K key, long timeout, TimeUnit unit) {
        int tm = (int) TimeoutUtils.toSeconds(timeout, unit);
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                List<byte[]> lPop = connection.bLPop(tm, rawKey);
                return (CollectionUtils.isEmpty(lPop) ? null : lPop.get(1));
            }
        });
    }

    @Override
    public Long leftPush(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.lPush(rawKey, rawValue));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long leftPushAll(K key, V... values) {
        byte[] rawKey = rawKey(key);
        // noinspection ConfusingArgumentToVarargsMethod
        byte[][] rawValues = rawValues(values);
        return execute(connection -> connection.lPush(rawKey, rawValues));
    }

    @Override
    public Long leftPushAll(K key, Collection<V> values) {
        byte[] rawKey = rawKey(key);
        byte[][] rawValues = rawValues(values);
        return execute(connection -> connection.lPush(rawKey, rawValues));
    }

    @Override
    public Long leftPushIfPresent(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.lPushX(rawKey, rawValue));
    }

    @Override
    public Long leftPush(K key, V pivot, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawPivot = rawValue(pivot);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.lInsert(rawKey, Position.BEFORE, rawPivot, rawValue));
    }

    @Override
    public Long size(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.lLen(rawKey));
    }

    @Override
    public List<V> range(K key, long start, long end) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> deserializeValues(connection.lRange(rawKey, start, end)));
    }

    @Override
    public Long remove(K key, long count, Object value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.lRem(rawKey, count, rawValue));
    }

    @Override
    public V rightPop(K key) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.rPop(rawKey);
            }
        });
    }

    @Override
    public List<V> rightPop(K key, long count) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> deserializeValues(connection.rPop(rawKey, count)));
    }

    @Override
    public V rightPop(K key, long timeout, TimeUnit unit) {
        int tm = (int) TimeoutUtils.toSeconds(timeout, unit);
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                List<byte[]> bRPop = connection.bRPop(tm, rawKey);
                return (CollectionUtils.isEmpty(bRPop) ? null : bRPop.get(1));
            }
        });
    }

    @Override
    public Long rightPush(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.rPush(rawKey, rawValue));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long rightPushAll(K key, V... values) {
        byte[] rawKey = rawKey(key);
        // noinspection ConfusingArgumentToVarargsMethod
        byte[][] rawValues = rawValues(values);
        return execute(connection -> connection.rPush(rawKey, rawValues));
    }

    @Override
    public Long rightPushAll(K key, Collection<V> values) {
        byte[] rawKey = rawKey(key);
        byte[][] rawValues = rawValues(values);
        return execute(connection -> connection.rPush(rawKey, rawValues));
    }

    @Override
    public Long rightPushIfPresent(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.rPushX(rawKey, rawValue));
    }

    @Override
    public Long rightPush(K key, V pivot, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawPivot = rawValue(pivot);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.lInsert(rawKey, Position.AFTER, rawPivot, rawValue));
    }

    @Override
    public V rightPopAndLeftPush(K sourceKey, K destinationKey) {
        byte[] rawDestKey = rawKey(destinationKey);
        return execute(new ValueDeserializingRedisCallback(sourceKey) {
            @Override
            protected byte[] inRedis(byte[] rawSourceKey, RedisConnection connection) {
                return connection.rPopLPush(rawSourceKey, rawDestKey);
            }
        });
    }

    @Override
    public V rightPopAndLeftPush(K sourceKey, K destinationKey, long timeout, TimeUnit unit) {
        int tm = (int) TimeoutUtils.toSeconds(timeout, unit);
        byte[] rawDestKey = rawKey(destinationKey);
        return execute(new ValueDeserializingRedisCallback(sourceKey) {
            @Override
            protected byte[] inRedis(byte[] rawSourceKey, RedisConnection connection) {
                return connection.bRPopLPush(tm, rawSourceKey, rawDestKey);
            }
        });
    }

    @Override
    public V move(K sourceKey, Direction from, K destinationKey, Direction to) {
        byte[] rawDestKey = rawKey(destinationKey);
        return execute(new ValueDeserializingRedisCallback(sourceKey) {
            @Override
            protected byte[] inRedis(byte[] rawSourceKey, RedisConnection connection) {
                return connection.lMove(rawSourceKey, rawDestKey, from, to);
            }
        });
    }

    @Override
    public V move(K sourceKey, Direction from, K destinationKey, Direction to, long timeout, TimeUnit unit) {
        byte[] rawDestKey = rawKey(destinationKey);
        return execute(new ValueDeserializingRedisCallback(sourceKey) {
            @Override
            protected byte[] inRedis(byte[] rawSourceKey, RedisConnection connection) {
                return connection.bLMove(rawSourceKey, rawDestKey, from, to, TimeoutUtils.toDoubleSeconds(timeout, unit));
            }
        });
    }

    @Override
    public void set(K key, long index, V value) {
        byte[] rawValue = rawValue(value);
        execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                connection.lSet(rawKey, index, rawValue);
                return null;
            }
        });
    }

    @Override
    public void trim(K key, long start, long end) {
        execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                connection.lTrim(rawKey, start, end);
                return null;
            }
        });
    }
}
