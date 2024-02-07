package org.clever.js.api;

import org.clever.js.api.folder.Folder;
import org.clever.js.api.require.Require;

import java.io.Closeable;

/**
 * 脚本引擎实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/14 11:15 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public interface ScriptEngineInstance<E, T> extends Closeable {
    /**
     * 引擎名称
     */
    String getEngineName();

    /**
     * 引擎版本
     */
    String getEngineVersion();

    /**
     * 获取语言版本
     */
    String getLanguageVersion();

    /**
     * 获取脚本引擎上下文
     */
    ScriptEngineContext<E, T> getEngineContext();

    /**
     * script引擎对象
     */
    E getEngine();

    /**
     * 根路径Folder
     */
    Folder getRootPath();

    /**
     * 共享的全局变量(在js代码中使用“global”变量名引用，与ES2020规范的“globalThis”类似但是不是同一个对象)
     */
    T getGlobal();

    /**
     * require用于加载其他模块(在js代码中使用“require”变量名引用)
     */
    Require<T> getRequire();

    /**
     * 获取模块exports包装对象
     *
     * @param id 模块ID(模块路径)
     */
    ScriptObject<T> require(String id) throws Exception;

    /**
     * 清除所有 {@link #require(String)} 缓存
     */
    void clearModuleCache();

    /**
     * 清除指定的 {@link #require(String)} 缓存
     */
    void removeModuleCache(String id);

    /**
     * 把脚本代码包装成函数对象
     *
     * @param code 脚本代码片段(并非完整的函数定义，仅仅是一段脚本代码)
     * @return 脚本函数对象
     */
    ScriptObject<T> wrapFunction(String code);

    /**
     * 清除所有 {@link #wrapFunction(String)} 缓存
     */
    void clearFunctionCache();

    /**
     * 移除指定的 {@link #wrapFunction(String)} 缓存
     *
     * @param code 脚本代码片段
     */
    void removeFunctionCache(String code);
}

//(x)module                               module变量代表当前模块
//(x)exports                              就是module.exports
//require                                 require用于加载其他模块
//global                                  共享的全局变量
