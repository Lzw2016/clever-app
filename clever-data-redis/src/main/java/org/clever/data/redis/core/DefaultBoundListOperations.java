package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;
import org.clever.data.redis.connection.RedisListCommands.Direction;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link BoundListOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:29 <br/>
 */
class DefaultBoundListOperations<K, V> extends DefaultBoundKeyOperations<K> implements BoundListOperations<K, V> {
    private final ListOperations<K, V> ops;

    /**
     * 构造一个新的 <code>DefaultBoundListOperations<code> 实例
     */
    DefaultBoundListOperations(K key, RedisOperations<K, V> operations) {
        super(key, operations);
        this.ops = operations.opsForList();
    }

    @Override
    public RedisOperations<K, V> getOperations() {
        return ops.getOperations();
    }

    @Override
    public V index(long index) {
        return ops.index(getKey(), index);
    }

    @Override
    public Long indexOf(V value) {
        return ops.indexOf(getKey(), value);
    }

    @Override
    public Long lastIndexOf(V value) {
        return ops.lastIndexOf(getKey(), value);
    }

    @Override
    public V leftPop() {
        return ops.leftPop(getKey());
    }

    @Override
    public List<V> leftPop(long count) {
        return ops.leftPop(getKey(), count);
    }

    @Override
    public V leftPop(long timeout, TimeUnit unit) {
        return ops.leftPop(getKey(), timeout, unit);
    }

    @Override
    public Long leftPush(V value) {
        return ops.leftPush(getKey(), value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long leftPushAll(V... values) {
        return ops.leftPushAll(getKey(), values);
    }

    @Override
    public Long leftPushIfPresent(V value) {
        return ops.leftPushIfPresent(getKey(), value);
    }

    @Override
    public Long leftPush(V pivot, V value) {
        return ops.leftPush(getKey(), pivot, value);
    }

    @Override
    public Long size() {
        return ops.size(getKey());
    }

    @Override
    public List<V> range(long start, long end) {
        return ops.range(getKey(), start, end);
    }

    @Override
    public Long remove(long i, Object value) {
        return ops.remove(getKey(), i, value);
    }

    @Override
    public V rightPop() {
        return ops.rightPop(getKey());
    }

    @Override
    public List<V> rightPop(long count) {
        return ops.rightPop(getKey(), count);
    }

    @Override
    public V rightPop(long timeout, TimeUnit unit) {
        return ops.rightPop(getKey(), timeout, unit);
    }

    @Override
    public Long rightPushIfPresent(V value) {
        return ops.rightPushIfPresent(getKey(), value);
    }

    @Override
    public Long rightPush(V value) {
        return ops.rightPush(getKey(), value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long rightPushAll(V... values) {
        return ops.rightPushAll(getKey(), values);
    }

    @Override
    public Long rightPush(V pivot, V value) {
        return ops.rightPush(getKey(), pivot, value);
    }

    @Override
    public V move(Direction from, K destinationKey, Direction to) {
        return ops.move(getKey(), from, destinationKey, to);
    }

    @Override
    public V move(Direction from, K destinationKey, Direction to, Duration timeout) {
        return ops.move(getKey(), from, destinationKey, to, timeout);
    }

    @Override
    public V move(Direction from, K destinationKey, Direction to, long timeout, TimeUnit unit) {
        return ops.move(getKey(), from, destinationKey, to, timeout, unit);
    }

    @Override
    public void trim(long start, long end) {
        ops.trim(getKey(), start, end);
    }

    @Override
    public void set(long index, V value) {
        ops.set(getKey(), index, value);
    }

    @Override
    public DataType getType() {
        return DataType.LIST;
    }
}
