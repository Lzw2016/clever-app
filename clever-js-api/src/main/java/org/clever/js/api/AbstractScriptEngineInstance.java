package org.clever.js.api;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.Assert;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.Cache;
import org.clever.js.api.require.Require;
import org.clever.js.api.utils.ScriptCodeUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:27 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
@Slf4j
public abstract class AbstractScriptEngineInstance<E, T> implements ScriptEngineInstance<E, T> {
    /**
     * 引擎上下文
     */
    protected final ScriptEngineContext<E, T> engineContext;

    /**
     * @param engineContext 引擎上下文
     */
    public AbstractScriptEngineInstance(ScriptEngineContext<E, T> engineContext) {
        Assert.notNull(engineContext, "参数 engineContext 不能为 null");
        this.engineContext = engineContext;
    }

    @Override
    public ScriptEngineContext<E, T> getEngineContext() {
        return engineContext;
    }

    @Override
    public E getEngine() {
        return engineContext.getEngine();
    }

    @Override
    public Folder getRootPath() {
        return engineContext.getRootPath();
    }

    @Override
    public T getGlobal() {
        return engineContext.getGlobal();
    }

    @Override
    public Require<T> getRequire() {
        return engineContext.getRequire();
    }

    @Override
    public ScriptObject<T> require(String id) throws Exception {
        T original = engineContext.getRequire().require(id);
        return wrapScriptObject(original);
    }

    @Override
    public void clearModuleCache() {
        engineContext.getModuleCache().clear();
    }

    @Override
    public void removeModuleCache(String id) {
        engineContext.getModuleCache().remove(id);
    }

    @Override
    public ScriptObject<T> wrapFunction(String code) {
        Assert.isNotBlank(code, "参数 code 不能为空");
        final Cache<ScriptObject<T>> functionCache = engineContext.getFunctionCache();
        ScriptObject<T> function = functionCache.get(code);
        if (function != null) {
            return function;
        }
        synchronized (functionCache) {
            // 二次确认
            function = functionCache.get(code);
            if (function != null) {
                return function;
            }
            String compressCode = ScriptCodeUtils.compressCode(code, true);
            String funCode = ScriptCodeUtils.wrapFunction(compressCode);
            function = createFunction(funCode);
            functionCache.put(code, function);
        }
        return function;
    }

    @Override
    public void clearFunctionCache() {
        engineContext.getFunctionCache().clear();
    }

    @Override
    public void removeFunctionCache(String code) {
        engineContext.getFunctionCache().remove(code);
    }

    @Override
    public void close() {
        clearModuleCache();
        clearFunctionCache();
    }

    /**
     * 根据 “script引擎对象” 包装成 ScriptObject
     */
    protected abstract ScriptObject<T> wrapScriptObject(T original);

    /**
     * 根据“完整的函数定义代码”创建脚本函数对象
     *
     * @param funCode 完整的函数定义代码
     */
    protected abstract ScriptObject<T> createFunction(String funCode);

    /**
     * ScriptEngineInstance 构建器
     *
     * @param <E> script引擎类型
     * @param <T> script引擎对象类型
     */
    public static abstract class AbstractBuilder<E, T> extends org.clever.js.api.AbstractBuilder<E, T, ScriptEngineInstance<E, T>> {
        /**
         * @param rootPath 根路径文件夹
         */
        public AbstractBuilder(Folder rootPath) {
            super(rootPath);
        }
    }
}
