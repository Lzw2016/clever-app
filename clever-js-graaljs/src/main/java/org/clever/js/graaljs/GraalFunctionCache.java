package org.clever.js.graaljs;

import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.ScriptFunctionCache;
import org.clever.js.api.ScriptObject;
import org.clever.util.Assert;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/06 09:18 <br/>
 */
public class GraalFunctionCache extends ScriptFunctionCache<Context, Value> {
    /**
     * @param engineInstance script引擎对象
     * @param expireTime     缓存的过期时间，单位：秒(小于等于0表示不清除)
     * @param maxCapacity    最大缓存容量
     */
    public GraalFunctionCache(ScriptEngineInstance<Context, Value> engineInstance, long expireTime, int maxCapacity) {
        super(engineInstance, expireTime, maxCapacity);
    }

    /**
     * 创建 ScriptObjectCache
     *
     * @param engineInstance script引擎对象
     */
    public GraalFunctionCache(ScriptEngineInstance<Context, Value> engineInstance) {
        super(engineInstance);
    }

    @Override
    protected ScriptObject<Value> createFunction(String funCode) {
        Source source = Source.newBuilder(GraalConstant.Js_Language_Id, funCode, String.format("/__fun_autogenerate_%s.js", FUC_COUNTER.get()))
            .cached(true)
            .buildLiteral();
        Context engine = engineInstance.getEngine();
        Value value;
        try {
            engine.enter();
            value = engine.eval(source);
        } finally {
            engine.leave();
        }
        Assert.isTrue(value != null && value.canExecute(), String.format("脚本代码不可以执行:\n%s\n", funCode));
        return new GraalScriptObject(engineInstance.getEngineContext(), value);
    }
}
