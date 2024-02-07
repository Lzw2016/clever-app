package org.clever.js.api;

import lombok.Getter;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.Cache;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.Require;
import org.clever.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/26 09:30 <br/>
 *
 * @param <E>  script引擎类型
 * @param <T>  script引擎对象类型
 * @param <EI> build返回的对象类型
 */
@Getter
public abstract class AbstractBuilder<E, T, EI> {
    protected E engine;
    protected Map<String, Object> registerGlobalVars = new HashMap<String, Object>() {{
        putAll(GlobalConstant.DEFAULT_REGISTER_GLOBAL_VARS);
        putAll(GlobalConstant.CUSTOM_REGISTER_GLOBAL_VARS);
    }};
    protected final Folder rootPath;
    protected Cache<Module<T>> moduleCache;
    protected Cache<ScriptObject<T>> functionCache;
    protected Require<T> require;
    protected CompileModule<T> compileModule;
    protected T global;

    /**
     * @param rootPath 根路径文件夹
     */
    public AbstractBuilder(Folder rootPath) {
        Assert.notNull(rootPath, "参数 rootPath 不能为 null");
        this.rootPath = rootPath;
    }

    /**
     * 设置 ScriptEngine
     */
    public AbstractBuilder<E, T, EI> setEngine(E engine) {
        this.engine = engine;
        return this;
    }

    /**
     * 自定义引擎全局对象
     */
    public AbstractBuilder<E, T, EI> setRegisterGlobalVars(Map<String, Object> registerGlobalVars) {
        this.registerGlobalVars = registerGlobalVars;
        return this;
    }

    /**
     * 添加引擎全局对象
     */
    public AbstractBuilder<E, T, EI> registerGlobalVar(String name, Object object) {
        this.registerGlobalVars.put(name, object);
        return this;
    }

    /**
     * 设置模块缓存
     */
    public AbstractBuilder<E, T, EI> setModuleCache(Cache<Module<T>> moduleCache) {
        this.moduleCache = moduleCache;
        return this;
    }

    /**
     * 设置函数缓存
     */
    public AbstractBuilder<E, T, EI> setFunctionCache(Cache<ScriptObject<T>> functionCache) {
        this.functionCache = functionCache;
        return this;
    }

    /**
     * 设置全局require实例(根目录require)
     */
    public AbstractBuilder<E, T, EI> setRequire(Require<T> require) {
        this.require = require;
        return this;
    }

    /**
     * 设置编译模块实现
     */
    public AbstractBuilder<E, T, EI> setCompileModule(CompileModule<T> compileModule) {
        this.compileModule = compileModule;
        return this;
    }

    /**
     * 设置引擎全局变量
     */
    public AbstractBuilder<E, T, EI> setGlobal(T global) {
        this.global = global;
        return this;
    }

    public abstract EI build();
}
