package org.clever.js.api;


import org.clever.util.Assert;

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
    protected final ScriptEngineContext<E, T> context;
    /**
     * Script引擎对应的对象值
     */
    protected final T original;

    public AbstractScriptObject(ScriptEngineContext<E, T> context, T original) {
        Assert.notNull(context, "参数context不能为空");
        Assert.notNull(original, "参数original不能为空");
        this.context = context;
        this.original = original;
    }

    @Override
    public T originalValue() {
        return original;
    }
}
