package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link BoundSetOperations} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:30 <br/>
 */
class DefaultBoundSetOperations<K, V> extends DefaultBoundKeyOperations<K> implements BoundSetOperations<K, V> {
    private final SetOperations<K, V> ops;

    /**
     * 构造一个新的 <code>DefaultBoundSetOperations</code> 实例
     */
    DefaultBoundSetOperations(K key, RedisOperations<K, V> operations) {
        super(key, operations);
        this.ops = operations.opsForSet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long add(V... values) {
        return ops.add(getKey(), values);
    }

    @Override
    public Set<V> diff(K key) {
        return ops.difference(getKey(), key);
    }

    @Override
    public Set<V> diff(Collection<K> keys) {
        return ops.difference(getKey(), keys);
    }

    @Override
    public void diffAndStore(K key, K destKey) {
        ops.differenceAndStore(getKey(), key, destKey);
    }

    @Override
    public void diffAndStore(Collection<K> keys, K destKey) {
        ops.differenceAndStore(getKey(), keys, destKey);
    }

    @Override
    public RedisOperations<K, V> getOperations() {
        return ops.getOperations();
    }

    @Override
    public Set<V> intersect(K key) {
        return ops.intersect(getKey(), key);
    }

    @Override
    public Set<V> intersect(Collection<K> keys) {
        return ops.intersect(getKey(), keys);
    }

    @Override
    public void intersectAndStore(K key, K destKey) {
        ops.intersectAndStore(getKey(), key, destKey);
    }

    @Override
    public void intersectAndStore(Collection<K> keys, K destKey) {
        ops.intersectAndStore(getKey(), keys, destKey);
    }

    @Override
    public Boolean isMember(Object o) {
        return ops.isMember(getKey(), o);
    }

    @Override
    public Map<Object, Boolean> isMember(Object... objects) {
        return ops.isMember(getKey(), objects);
    }

    @Override
    public Set<V> members() {
        return ops.members(getKey());
    }

    @Override
    public Boolean move(K destKey, V value) {
        return ops.move(getKey(), value, destKey);
    }

    @Override
    public V randomMember() {
        return ops.randomMember(getKey());
    }

    @Override
    public Set<V> distinctRandomMembers(long count) {
        return ops.distinctRandomMembers(getKey(), count);
    }

    @Override
    public List<V> randomMembers(long count) {
        return ops.randomMembers(getKey(), count);
    }

    @Override
    public Long remove(Object... values) {
        return ops.remove(getKey(), values);
    }

    @Override
    public V pop() {
        return ops.pop(getKey());
    }

    @Override
    public Long size() {
        return ops.size(getKey());
    }

    @Override
    public Set<V> union(K key) {
        return ops.union(getKey(), key);
    }

    @Override
    public Set<V> union(Collection<K> keys) {
        return ops.union(getKey(), keys);
    }

    @Override
    public void unionAndStore(K key, K destKey) {
        ops.unionAndStore(getKey(), key, destKey);
    }

    @Override
    public void unionAndStore(Collection<K> keys, K destKey) {
        ops.unionAndStore(getKey(), keys, destKey);
    }

    @Override
    public DataType getType() {
        return DataType.SET;
    }

    @Override
    public Cursor<V> scan(ScanOptions options) {
        return ops.scan(getKey(), options);
    }
}
