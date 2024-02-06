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
    protected final E engine;
    /**
     * 自定义引擎全局对象
     */
    protected final Map<String, Object> registerGlobalVars = new ConcurrentHashMap<>();
    /**
     * 根路径文件夹
     */
    protected final Folder rootPath;
    /**
     * 模块缓存
     */
    protected final ModuleCache<T> moduleCache;
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

    public AbstractScriptEngineContext(E engine,
                                       Map<String, Object> registerGlobalVars,
                                       Folder rootPath,
                                       ModuleCache<T> moduleCache,
                                       Require<T> require,
                                       CompileModule<T> compileModule,
                                       T global) {
        Assert.notNull(engine, "参数 engine 不能为  null");
        Assert.notNull(rootPath, "参数 rootPath 不能为  null");
        Assert.notNull(moduleCache, "参数 moduleCache 不能为  null");
        Assert.notNull(require, "参数 require 不能为 null");
        Assert.notNull(compileModule, "参数 compileModule 不能为 null");
        Assert.notNull(global, "参数 global 不能为 null");
        this.engine = engine;
        if (registerGlobalVars != null) {
            this.registerGlobalVars.putAll(registerGlobalVars);
        }
        this.rootPath = rootPath;
        this.moduleCache = moduleCache;
        this.require = require;
        this.compileModule = compileModule;
        this.global = global;
    }

    protected AbstractScriptEngineContext(E engine,
                                          Map<String, Object> registerGlobalVars,
                                          Folder rootPath,
                                          ModuleCache<T> moduleCache) {
        Assert.notNull(engine, "参数 engine 不能为  null");
        Assert.notNull(rootPath, "参数 rootPath 不能为  null");
        Assert.notNull(moduleCache, "参数 moduleCache 不能为  null");
        this.engine = engine;
        if (registerGlobalVars != null) {
            this.registerGlobalVars.putAll(registerGlobalVars);
        }
        this.rootPath = rootPath;
        this.moduleCache = moduleCache;
    }

    @Override
    public E getEngine() {
        return engine;
    }

    @Override
    public Map<String, Object> getRegisterGlobalVars() {
        return registerGlobalVars;
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
