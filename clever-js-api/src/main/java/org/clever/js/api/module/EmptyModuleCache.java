package org.clever.js.api.module;

/**
 * 空缓存(不使用缓存)
 *
 * @param <T> script引擎对象类型
 */
public class EmptyModuleCache<T> implements ModuleCache<T> {

    @Override
    public Module<T> get(String fullPath) {
        return null;
    }

    @Override
    public void put(String fullPath, Module<T> module) {
    }

    @Override
    public void clear() {
    }

    @Override
    public void remove(String fullPath) {
    }
}
