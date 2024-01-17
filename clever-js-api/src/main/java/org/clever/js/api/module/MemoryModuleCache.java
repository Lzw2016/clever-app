package org.clever.js.api.module;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Module 内存缓存
 *
 * @param <T> script引擎对象类型
 */
@Slf4j
public class MemoryModuleCache<T> implements ModuleCache<T> {
    public static final int Default_Initial_Capacity = 512;
    /**
     * 缓存
     */
    @JsonIgnore
    private final Cache<String, Module<T>> modulesCache;

    /**
     * @param clearTimeInterval 定时清除缓存的时间间隔，单位：秒(小于0表示不清除)
     * @param initialCapacity   初始缓存容量
     */
    public MemoryModuleCache(long clearTimeInterval, int initialCapacity) {
        if (initialCapacity < 0) {
            initialCapacity = Default_Initial_Capacity;
        }
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder().initialCapacity(initialCapacity);
        if (clearTimeInterval >= 0) {
            cacheBuilder.removalListener(notification -> {
                Object string = notification.getKey();
                log.debug("ModuleCache 移除缓存 -> {} | 原因: {}", string, notification.getCause());
                Object value = notification.getValue();
                if (value instanceof Module) {
                    Module<?> module = (Module<?>) value;
                    module.triggerOnRemove();
                }
            }).expireAfterWrite(clearTimeInterval, TimeUnit.SECONDS);
        }
        this.modulesCache = cacheBuilder.build();
    }

    public MemoryModuleCache() {
        this(-1, Default_Initial_Capacity);
    }

    @Override
    public Module<T> get(String fullPath) {
        return modulesCache.getIfPresent(fullPath);
    }

    @Override
    public void put(String fullPath, Module<T> module) {
        modulesCache.put(fullPath, module);
    }

    @Override
    public void clear() {
        modulesCache.invalidateAll();
    }

    @Override
    public void remove(String fullPath) {
        modulesCache.invalidate(fullPath);
    }
}
