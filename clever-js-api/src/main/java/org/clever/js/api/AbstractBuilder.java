package org.clever.js.api;

import lombok.Getter;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.ModuleCache;
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
    protected Map<String, Object> contextMap = new HashMap<String, Object>() {{
        putAll(GlobalConstant.Default_Context_Map);
        putAll(GlobalConstant.Custom_Context_Map);
    }};
    protected final Folder rootPath;
    protected ModuleCache<T> moduleCache;
    protected Require<T> require;
    protected CompileModule<T> compileModule;
    protected T global;

    /**
     * @param rootPath 根路径文件夹
     */
    public AbstractBuilder(Folder rootPath) {
        Assert.notNull(rootPath, "参数rootPath不能为空");
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
    public AbstractBuilder<E, T, EI> setContextMap(Map<String, Object> contextMap) {
        this.contextMap = contextMap;
        return this;
    }

    /**
     * 添加引擎全局对象
     */
    public AbstractBuilder<E, T, EI> putContextMap(String name, Object object) {
        this.contextMap.put(name, object);
        return this;
    }

    /**
     * 设置模块缓存
     */
    public AbstractBuilder<E, T, EI> setModuleCache(ModuleCache<T> moduleCache) {
        this.moduleCache = moduleCache;
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
