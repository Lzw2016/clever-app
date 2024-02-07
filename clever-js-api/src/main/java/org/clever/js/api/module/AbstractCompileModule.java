package org.clever.js.api.module;

import org.clever.js.api.ScriptEngineContext;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/18 22:55 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public abstract class AbstractCompileModule<E, T> implements CompileModule<T> {
    /**
     * 引擎上下文
     */
    protected final ScriptEngineContext<E, T> engineContext;

    public AbstractCompileModule(ScriptEngineContext<E, T> engineContext) {
        this.engineContext = engineContext;
    }

    public Cache<Module<T>> getCache() {
        return engineContext.getModuleCache();
    }

    /**
     * 获取Script模块代码
     *
     * @param code Script代码
     */
    protected String getModuleScriptCode(String code) {
        return "(function(exports, require, module, __filename, __dirname) {;" + code + "\n});";
    }
}
