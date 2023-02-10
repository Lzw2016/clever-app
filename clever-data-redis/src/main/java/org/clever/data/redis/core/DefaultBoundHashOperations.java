package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Default implementation for {@link HashOperations}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:25 <br/>
 */
class DefaultBoundHashOperations<H, HK, HV> extends DefaultBoundKeyOperations<H> implements BoundHashOperations<H, HK, HV> {
    private final HashOperations<H, HK, HV> ops;

    /**
     * 构造一个新的<code>DefaultBoundHashOperations</code>实例
     */
    DefaultBoundHashOperations(H key, RedisOperations<H, ?> operations) {
        super(key, operations);
        this.ops = operations.opsForHash();
    }

    @Override
    public Long delete(Object... keys) {
        return ops.delete(getKey(), keys);
    }

    @Override
    public HV get(Object key) {
        return ops.get(getKey(), key);
    }

    @Override
    public List<HV> multiGet(Collection<HK> hashKeys) {
        return ops.multiGet(getKey(), hashKeys);
    }

    @Override
    public RedisOperations<H, ?> getOperations() {
        return ops.getOperations();
    }

    @Override
    public Boolean hasKey(Object key) {
        return ops.hasKey(getKey(), key);
    }

    @Override
    public Long increment(HK key, long delta) {
        return ops.increment(getKey(), key, delta);
    }

    @Override
    public Double increment(HK key, double delta) {
        return ops.increment(getKey(), key, delta);
    }

    @Override
    public HK randomKey() {
        return ops.randomKey(getKey());
    }

    @Override
    public Entry<HK, HV> randomEntry() {
        return ops.randomEntry(getKey());
    }

    @Override
    public List<HK> randomKeys(long count) {
        return ops.randomKeys(getKey(), count);
    }

    @Override
    public Map<HK, HV> randomEntries(long count) {
        return ops.randomEntries(getKey(), count);
    }

    @Override
    public Set<HK> keys() {
        return ops.keys(getKey());
    }

    @Override
    public Long lengthOfValue(HK hashKey) {
        return ops.lengthOfValue(getKey(), hashKey);
    }

    @Override
    public Long size() {
        return ops.size(getKey());
    }

    @Override
    public void putAll(Map<? extends HK, ? extends HV> m) {
        ops.putAll(getKey(), m);
    }

    @Override
    public void put(HK key, HV value) {
        ops.put(getKey(), key, value);
    }

    @Override
    public Boolean putIfAbsent(HK key, HV value) {
        return ops.putIfAbsent(getKey(), key, value);
    }

    @Override
    public List<HV> values() {
        return ops.values(getKey());
    }

    @Override
    public Map<HK, HV> entries() {
        return ops.entries(getKey());
    }

    @Override
    public DataType getType() {
        return DataType.HASH;
    }

    @Override
    public Cursor<Entry<HK, HV>> scan(ScanOptions options) {
        return ops.scan(getKey(), options);
    }
}
