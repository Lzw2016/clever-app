package org.clever.js.api;


import org.clever.core.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:22 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public abstract class AbstractScriptObject<E, T> implements ScriptObject<T> {
    /**
     * 引擎上下文
     */
    protected final ScriptEngineContext<E, T> engineContext;
    /**
     * Script引擎对应的对象值
     */
    protected final T original;

    public AbstractScriptObject(ScriptEngineContext<E, T> engineContext, T original) {
        Assert.notNull(engineContext, "参数 engineContext 不能为 null");
        Assert.notNull(original, "参数 original 不能为空");
        this.engineContext = engineContext;
        this.original = original;
    }

    @Override
    public T originalValue() {
        return original;
    }
}
