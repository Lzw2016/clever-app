package org.clever.data.redis.core;

import org.clever.dao.DataAccessException;
import org.clever.data.redis.connection.BitFieldSubCommands;
import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.connection.RedisStringCommands.SetOption;
import org.clever.data.redis.core.types.Expiration;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * {@link ValueOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:47 <br/>
 */
class DefaultValueOperations<K, V> extends AbstractOperations<K, V> implements ValueOperations<K, V> {
    DefaultValueOperations(RedisTemplate<K, V> template) {
        super(template);
    }

    @Override
    public V get(Object key) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.get(rawKey);
            }
        });
    }

    @Override
    public V getAndDelete(K key) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.getDel(rawKey);
            }
        });
    }

    @Override
    public V getAndExpire(K key, long timeout, TimeUnit unit) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.getEx(rawKey, Expiration.from(timeout, unit));
            }
        });
    }

    @Override
    public V getAndExpire(K key, Duration timeout) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.getEx(rawKey, Expiration.from(timeout));
            }
        });
    }

    @Override
    public V getAndPersist(K key) {
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.getEx(rawKey, Expiration.persistent());
            }
        });
    }

    @Override
    public V getAndSet(K key, V newValue) {
        byte[] rawValue = rawValue(newValue);
        return execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                return connection.getSet(rawKey, rawValue);
            }
        });
    }

    @Override
    public Long increment(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.incr(rawKey));
    }

    @Override
    public Long increment(K key, long delta) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.incrBy(rawKey, delta));
    }

    @Override
    public Double increment(K key, double delta) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.incrBy(rawKey, delta));
    }

    @Override
    public Long decrement(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.decr(rawKey));
    }

    @Override
    public Long decrement(K key, long delta) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.decrBy(rawKey, delta));
    }

    @Override
    public Integer append(K key, String value) {
        byte[] rawKey = rawKey(key);
        byte[] rawString = rawString(value);
        return execute(connection -> {
            Long result = connection.append(rawKey, rawString);
            return (result != null) ? result.intValue() : null;
        });
    }

    @Override
    public String get(K key, long start, long end) {
        byte[] rawKey = rawKey(key);
        byte[] rawReturn = execute(connection -> connection.getRange(rawKey, start, end));
        return deserializeString(rawReturn);
    }

    @Override
    public List<V> multiGet(Collection<K> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        byte[][] rawKeys = new byte[keys.size()][];
        int counter = 0;
        for (K hashKey : keys) {
            rawKeys[counter++] = rawKey(hashKey);
        }
        List<byte[]> rawValues = execute(connection -> connection.mGet(rawKeys));
        return deserializeValues(rawValues);
    }

    @Override
    public void multiSet(Map<? extends K, ? extends V> m) {
        if (m.isEmpty()) {
            return;
        }
        Map<byte[], byte[]> rawKeys = new LinkedHashMap<>(m.size());
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            rawKeys.put(rawKey(entry.getKey()), rawValue(entry.getValue()));
        }
        execute(connection -> {
            connection.mSet(rawKeys);
            return null;
        });
    }

    @Override
    public Boolean multiSetIfAbsent(Map<? extends K, ? extends V> m) {
        if (m.isEmpty()) {
            return true;
        }
        Map<byte[], byte[]> rawKeys = new LinkedHashMap<>(m.size());
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            rawKeys.put(rawKey(entry.getKey()), rawValue(entry.getValue()));
        }
        return execute(connection -> connection.mSetNX(rawKeys));
    }

    @Override
    public void set(K key, V value) {
        byte[] rawValue = rawValue(value);
        execute(new ValueDeserializingRedisCallback(key) {
            @Override
            protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
                connection.set(rawKey, rawValue);
                return null;
            }
        });
    }

    @Override
    public void set(K key, V value, long timeout, TimeUnit unit) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                potentiallyUsePsetEx(connection);
                return null;
            }

            public void potentiallyUsePsetEx(RedisConnection connection) {
                if (!TimeUnit.MILLISECONDS.equals(unit) || !failsafeInvokePsetEx(connection)) {
                    connection.setEx(rawKey, TimeoutUtils.toSeconds(timeout, unit), rawValue);
                }
            }

            private boolean failsafeInvokePsetEx(RedisConnection connection) {
                boolean failed = false;
                try {
                    connection.pSetEx(rawKey, timeout, rawValue);
                } catch (UnsupportedOperationException e) {
                    // 如果连接不支持 pSetEx，则返回 false 以允许回退到其他操作
                    failed = true;
                }
                return !failed;
            }
        });
    }

    @Override
    public Boolean setIfAbsent(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.setNX(rawKey, rawValue));
    }

    @Override
    public Boolean setIfAbsent(K key, V value, long timeout, TimeUnit unit) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        Expiration expiration = Expiration.from(timeout, unit);
        return execute(connection -> connection.set(rawKey, rawValue, expiration, SetOption.ifAbsent()));
    }

    @Override
    public Boolean setIfPresent(K key, V value) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        return execute(connection -> connection.set(rawKey, rawValue, Expiration.persistent(), SetOption.ifPresent()));
    }

    @Override
    public Boolean setIfPresent(K key, V value, long timeout, TimeUnit unit) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        Expiration expiration = Expiration.from(timeout, unit);
        return execute(connection -> connection.set(rawKey, rawValue, expiration, SetOption.ifPresent()));
    }

    @Override
    public void set(K key, V value, long offset) {
        byte[] rawKey = rawKey(key);
        byte[] rawValue = rawValue(value);
        execute(connection -> {
            connection.setRange(rawKey, rawValue, offset);
            return null;
        });
    }

    @Override
    public Long size(K key) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.strLen(rawKey));
    }

    @Override
    public Boolean setBit(K key, long offset, boolean value) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.setBit(rawKey, offset, value));
    }

    @Override
    public Boolean getBit(K key, long offset) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.getBit(rawKey, offset));
    }

    @Override
    public List<Long> bitField(K key, final BitFieldSubCommands subCommands) {
        byte[] rawKey = rawKey(key);
        return execute(connection -> connection.bitField(rawKey, subCommands));
    }
}
