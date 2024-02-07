package org.clever.js.api;

import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.Cache;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.Require;

import java.util.Map;

/**
 * 脚本引擎上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/14 11:15 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public interface ScriptEngineContext<E, T> {
    /**
     * 获取script引擎实例
     */
    E getEngine();

    /**
     * 获取向Script引擎注册的全局对象集合
     */
    Map<String, Object> getRegisterGlobalVars();

    /**
     * 根路径文件夹
     */
    Folder getRootPath();

    /**
     * 模块缓存
     */
    Cache<Module<T>> getModuleCache();

    /**
     * 函数对象缓存
     */
    Cache<ScriptObject<T>> getFunctionCache();

    /**
     * 全局require实例(根目录require)
     */
    Require<T> getRequire();

    /**
     * 编译ScriptModule实现
     */
    CompileModule<T> getCompileModule();

    /**
     * 引擎全局变量
     */
    T getGlobal();

    // ...其他Context对象
}
