package org.clever.js.api.module;

/**
 * 脚本模块缓存
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/14 11:10 <br/>
 *
 * @param <T> script引擎对象类型
 */
public interface ModuleCache<T> {

    /**
     * 从缓存中获取脚本模块，不存在就返回null
     *
     * @param fullPath 模块逻辑绝对路径
     */
    Module<T> get(String fullPath);

    /**
     * 缓存脚本模块
     *
     * @param fullPath       模块逻辑绝对路径
     * @param module 脚本模块
     */
    void put(String fullPath, Module<T> module);

    /**
     * 清空脚本模块缓存
     */
    void clear();

    /**
     * 删除脚本模块缓存
     *
     * @param fullPath 模块逻辑绝对路径
     */
    void remove(String fullPath);
}
