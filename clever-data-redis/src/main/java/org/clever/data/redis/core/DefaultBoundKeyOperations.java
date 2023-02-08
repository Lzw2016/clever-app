package org.clever.data.redis.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 默认 {@link BoundKeyOperations} 实现。供内部使用
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:18 <br/>
 */
abstract class DefaultBoundKeyOperations<K> implements BoundKeyOperations<K> {
    private K key;
    private final RedisOperations<K, ?> ops;

    DefaultBoundKeyOperations(K key, RedisOperations<K, ?> operations) {
        this.key = key;
        this.ops = operations;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public Boolean expire(long timeout, TimeUnit unit) {
        return ops.expire(key, timeout, unit);
    }

    @Override
    public Boolean expireAt(Date date) {
        return ops.expireAt(key, date);
    }

    @Override
    public Long getExpire() {
        return ops.getExpire(key);
    }

    @Override
    public Boolean persist() {
        return ops.persist(key);
    }

    @Override
    public void rename(K newKey) {
        if (ops.hasKey(key)) {
            ops.rename(key, newKey);
        }
        key = newKey;
    }
}
