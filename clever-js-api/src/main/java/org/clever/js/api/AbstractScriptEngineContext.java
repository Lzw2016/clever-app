package org.clever.js.api;

import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.ModuleCache;
import org.clever.js.api.require.Require;
import org.clever.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:08 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public abstract class AbstractScriptEngineContext<E, T> implements ScriptEngineContext<E, T> {
    /**
     * NashornScriptEngine
     */
    protected E engine;
    /**
     * 自定义引擎全局对象
     */
    protected Map<String, Object> contextMap = new ConcurrentHashMap<>();
    /**
     * 根路径文件夹
     */
    protected Folder rootPath;
    /**
     * 模块缓存
     */
    protected ModuleCache<T> moduleCache;
    /**
     * 全局require实例(根目录require)
     */
    protected Require<T> require;
    /**
     * 编译模块实现
     */
    protected CompileModule<T> compileModule;
    /**
     * 引擎全局变量
     */
    protected T global;

    protected AbstractScriptEngineContext() {
    }

    public AbstractScriptEngineContext(
            E engine,
            Map<String, Object> contextMap,
            Folder rootPath,
            ModuleCache<T> moduleCache,
            Require<T> require,
            CompileModule<T> compileModule,
            T global) {
        Assert.notNull(engine, "参数engine不能为空");
        Assert.notNull(rootPath, "参数rootPath不能为空");
        Assert.notNull(moduleCache, "参数moduleCache不能为空");
        Assert.notNull(require, "参数require不能为空");
        Assert.notNull(compileModule, "参数compileModule不能为空");
        Assert.notNull(global, "参数global不能为空");
        this.engine = engine;
        if (contextMap != null) {
            this.contextMap.putAll(contextMap);
        }
        this.rootPath = rootPath;
        this.moduleCache = moduleCache;
        this.require = require;
        this.compileModule = compileModule;
        this.global = global;
    }

    @Override
    public E getEngine() {
        return engine;
    }

    @Override
    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    @Override
    public Folder getRootPath() {
        return rootPath;
    }

    @Override
    public ModuleCache<T> getModuleCache() {
        return moduleCache;
    }

    @Override
    public Require<T> getRequire() {
        return require;
    }

    @Override
    public CompileModule<T> getCompileModule() {
        return compileModule;
    }

    @Override
    public T getGlobal() {
        return global;
    }

    /**
     * ScriptEngineContext 构建器
     *
     * @param <E> script引擎类型
     * @param <T> script引擎对象类型
     */
    public static abstract class AbstractBuilder<E, T> extends org.clever.js.api.AbstractBuilder<E, T, ScriptEngineContext<E, T>> {
        /**
         * @param rootPath 根路径文件夹
         */
        public AbstractBuilder(Folder rootPath) {
            super(rootPath);
        }
    }
}
