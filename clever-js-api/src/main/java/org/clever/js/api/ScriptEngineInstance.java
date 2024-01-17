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
    ScriptEngineContext<E, T> getContext();

    /**
     * 根路径Folder
     */
    Folder getRootPath();

    /**
     * 共享的全局变量
     */
    T getGlobal();

    /**
     * require用于加载其他模块
     */
    Require<T> getRequire();

    /**
     * 获取模块exports包装对象
     *
     * @param id 模块ID(模块路径)
     */
    ScriptObject<T> require(String id) throws Exception;
}

//(x)module                               module变量代表当前模块
//(x)exports                              就是module.exports
//require                                 require用于加载其他模块
//global                                  共享的全局变量
