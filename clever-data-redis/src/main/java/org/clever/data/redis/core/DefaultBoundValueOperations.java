package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 17:18 <br/>
 */
class DefaultBoundValueOperations<K, V> extends DefaultBoundKeyOperations<K> implements BoundValueOperations<K, V> {
    private final ValueOperations<K, V> ops;

    /**
     * 构造一个新的 {@link DefaultBoundValueOperations} 实例
     */
    DefaultBoundValueOperations(K key, RedisOperations<K, V> operations) {
        super(key, operations);
        this.ops = operations.opsForValue();
    }

    @Override
    public V get() {
        return ops.get(getKey());
    }

    @Override
    public V getAndDelete() {
        return ops.getAndDelete(getKey());
    }

    @Override
    public V getAndExpire(long timeout, TimeUnit unit) {
        return ops.getAndExpire(getKey(), timeout, unit);
    }

    @Override
    public V getAndExpire(Duration timeout) {
        return ops.getAndExpire(getKey(), timeout);
    }

    @Override
    public V getAndPersist() {
        return ops.getAndPersist(getKey());
    }

    @Override
    public V getAndSet(V value) {
        return ops.getAndSet(getKey(), value);
    }

    @Override
    public Long increment() {
        return ops.increment(getKey());
    }

    @Override
    public Long increment(long delta) {
        return ops.increment(getKey(), delta);
    }

    @Override
    public Double increment(double delta) {
        return ops.increment(getKey(), delta);
    }

    @Override
    public Long decrement() {
        return ops.decrement(getKey());
    }

    @Override
    public Long decrement(long delta) {
        return ops.decrement(getKey(), delta);
    }

    @Override
    public Integer append(String value) {
        return ops.append(getKey(), value);
    }

    @Override
    public String get(long start, long end) {
        return ops.get(getKey(), start, end);
    }

    @Override
    public void set(V value, long timeout, TimeUnit unit) {
        ops.set(getKey(), value, timeout, unit);
    }

    @Override
    public void set(V value) {
        ops.set(getKey(), value);
    }

    @Override
    public Boolean setIfAbsent(V value) {
        return ops.setIfAbsent(getKey(), value);
    }

    @Override
    public Boolean setIfAbsent(V value, long timeout, TimeUnit unit) {
        return ops.setIfAbsent(getKey(), value, timeout, unit);
    }

    @Override
    public Boolean setIfPresent(V value) {
        return ops.setIfPresent(getKey(), value);
    }

    @Override
    public Boolean setIfPresent(V value, long timeout, TimeUnit unit) {
        return ops.setIfPresent(getKey(), value, timeout, unit);
    }

    @Override
    public void set(V value, long offset) {
        ops.set(getKey(), value, offset);
    }

    @Override
    public Long size() {
        return ops.size(getKey());
    }

    @Override
    public RedisOperations<K, V> getOperations() {
        return ops.getOperations();
    }

    @Override
    public DataType getType() {
        return DataType.STRING;
    }
}
