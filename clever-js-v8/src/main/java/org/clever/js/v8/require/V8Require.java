package org.clever.js.v8.require;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.callback.JavetCallbackContext;
import com.caoccao.javet.values.reference.IV8ValueFunction;
import com.caoccao.javet.values.reference.IV8ValueObject;
import com.caoccao.javet.values.reference.V8ValueFunction;
import lombok.SneakyThrows;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.AbstractRequire;
import org.clever.js.api.require.Require;
import org.clever.js.v8.module.V8Module;
import org.clever.js.v8.utils.ScriptEngineUtils;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:41 <br/>
 */
public class V8Require extends AbstractRequire<V8Runtime, IV8ValueObject> {

    public V8Require(ScriptEngineContext<V8Runtime, IV8ValueObject> context,
                     Module<IV8ValueObject> currentModule,
                     Folder currentModuleFolder) {
        super(context, currentModule, currentModuleFolder);
    }

    protected V8Require(ScriptEngineContext<V8Runtime, IV8ValueObject> context, Folder currentModuleFolder) {
        super(context, currentModuleFolder);
    }

    @Override
    protected IV8ValueObject newScriptObject() {
        return ScriptEngineUtils.newObject(engineContext.getEngine());
    }

    @Override
    protected AbstractRequire<V8Runtime, IV8ValueObject> newRequire(ScriptEngineContext<V8Runtime, IV8ValueObject> engineContext, Folder currentModuleFolder) {
        return new V8Require(engineContext, currentModuleFolder);
    }

    @Override
    protected Module<IV8ValueObject> newModule(ScriptEngineContext<V8Runtime, IV8ValueObject> engineContext,
                                               String id,
                                               String filename,
                                               IV8ValueObject exports,
                                               Module<IV8ValueObject> parent,
                                               Require<IV8ValueObject> require) {
        return new V8Module(engineContext, id, filename, exports, parent, require);
    }

    @SneakyThrows
    @Override
    protected void moduleFunctionCall(IV8ValueObject function,
                                      IV8ValueObject that,
                                      IV8ValueObject exports,
                                      Require<IV8ValueObject> require,
                                      IV8ValueObject module,
                                      String filename,
                                      String dirname) {
        Assert.isTrue(function instanceof IV8ValueFunction, "参数function必须是一个可执行函数ScriptObject");
        JavetCallbackContext callback = new JavetCallbackContext(
            "require",
            require,
            require.getClass().getMethod("require", String.class)
        );
        V8ValueFunction requireFun = engineContext.getEngine().createV8ValueFunction(callback);
        ((IV8ValueFunction) function).callVoid(function, exports, requireFun, module, filename, dirname);
    }
}
