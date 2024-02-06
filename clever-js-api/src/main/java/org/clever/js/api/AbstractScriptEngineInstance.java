package org.clever.js.api;

import org.clever.js.api.folder.Folder;
import org.clever.js.api.require.Require;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:27 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public abstract class AbstractScriptEngineInstance<E, T> implements ScriptEngineInstance<E, T> {
    /**
     * 引擎上下文
     */
    protected final ScriptEngineContext<E, T> engineContext;

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
        T scriptObject = engineContext.getRequire().require(id);
        return newScriptObject(scriptObject);
    }

    /**
     * 创建ScriptObject
     */
    protected abstract ScriptObject<T> newScriptObject(T scriptObject);

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
