package org.clever.js.api.module;

import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 内存缓存
 *
 * @param <T> script引擎对象类型
 */
@Slf4j
public class MemoryCache<T> implements Cache<T> {
    public static final int DEFAULT_MAX_CAPACITY = 4096;
    /**
     * 缓存
     */
    private final com.google.common.cache.Cache<String, T> modulesCache;

    /**
     * @param expireTime  缓存的过期时间，单位：秒(小于等于0表示不清除)
     * @param maxCapacity 最大缓存容量
     */
    public MemoryCache(long expireTime, int maxCapacity) {
        if (maxCapacity < 0) {
            maxCapacity = DEFAULT_MAX_CAPACITY;
        }
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .initialCapacity(32)
            .maximumSize(maxCapacity);
        if (expireTime >= 0) {
            cacheBuilder.removalListener(notification -> {
                Object string = notification.getKey();
                log.debug("ModuleCache 移除缓存 -> {} | 原因: {}", string, notification.getCause());
                Object value = notification.getValue();
                if (value instanceof Module) {
                    Module<?> module = (Module<?>) value;
                    module.triggerOnRemove();
                }
            }).expireAfterWrite(expireTime, TimeUnit.SECONDS);
        }
        this.modulesCache = cacheBuilder.build();
    }

    public MemoryCache() {
        this(-1, DEFAULT_MAX_CAPACITY);
    }

    @Override
    public T get(String key) {
        return modulesCache.getIfPresent(key);
    }

    @Override
    public void put(String key, T obj) {
        modulesCache.put(key, obj);
    }

    @Override
    public void clear() {
        modulesCache.invalidateAll();
    }

    @Override
    public void remove(String key) {
        modulesCache.invalidate(key);
    }
}
