package org.clever.js.api.module;

/**
 * 空缓存(不使用缓存)
 *
 * @param <T> script引擎对象类型
 */
public class EmptyCache<T> implements Cache<T> {
    @Override
    public T get(String key) {
        return null;
    }

    @Override
    public void put(String key, T obj) {
    }

    @Override
    public void clear() {
    }

    @Override
    public void remove(String key) {
    }
}
