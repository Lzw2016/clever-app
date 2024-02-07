package org.clever.js.api.module;

/**
 * 缓存实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/14 11:10 <br/>
 *
 * @param <T> script引擎对象类型
 */
public interface Cache<T> {
    /**
     * 从缓存中获取对象，不存在就返回null
     *
     * @param key 缓存key
     */
    T get(String key);

    /**
     * 缓存对象
     *
     * @param key 缓存key
     * @param obj 缓存对象
     */
    void put(String key, T obj);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 删除指定缓存对象
     *
     * @param key 缓存key
     */
    void remove(String key);
}
