package org.clever.js.javet.module;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.IV8ValueObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.module.AbstractModule;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.Require;
import org.clever.js.javet.V8ScriptObject;
import org.clever.js.javet.utils.ScriptEngineUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:41 <br/>
 */
@Slf4j
public class V8Module extends AbstractModule<V8Runtime, IV8ValueObject> {

    public V8Module(ScriptEngineContext<V8Runtime, IV8ValueObject> context,
                    String id,
                    String filename,
                    IV8ValueObject exports,
                    Module<IV8ValueObject> parent,
                    Require<IV8ValueObject> require) {
        super(context, id, filename, exports, parent, require);
        // Assert.isTrue(require instanceof JavaCallback, "参数require必须实现JavaCallback接口");
    }

    protected V8Module(ScriptEngineContext<V8Runtime, IV8ValueObject> context) {
        super(context);
    }

    /**
     * 创建主模块(根模块)
     */
    public static V8Module createMainModule(ScriptEngineContext<V8Runtime, IV8ValueObject> context) {
        return new V8Module(context);
    }

    @SneakyThrows
    @Override
    protected void initModule(IV8ValueObject exports) {
        this.module.set(GlobalConstant.Module_Id, this.id);
        this.module.set(GlobalConstant.Module_Filename, this.filename);
        this.module.set(GlobalConstant.Module_Loaded, this.loaded);
        if (this.parent != null) {
            this.module.set(GlobalConstant.Module_Parent, this.parent.getModule());
        }
        // TODO  Module_Paths
        this.module.set(GlobalConstant.Module_Paths, ScriptEngineUtils.newArray(context.getEngine()));
        // TODO  Module_Children
        this.module.set(GlobalConstant.Module_Children, ScriptEngineUtils.newArray(context.getEngine()));
        this.module.set(GlobalConstant.Module_Exports, exports);
        // Assert.isTrue(this.require instanceof JavaCallback, "参数require必须实现JavaCallback接口");
        // this.module.registerJavaMethod((JavaCallback) this.require, GlobalConstant.Module_Require);
    }

    @Override
    protected IV8ValueObject newScriptObject() {
        return ScriptEngineUtils.newObject(context.getEngine());
    }

    @SneakyThrows
    @Override
    public IV8ValueObject getExports() {
        return this.module.get(GlobalConstant.Module_Exports);
    }

    @Override
    public ScriptObject<IV8ValueObject> getExportsWrapper() {
        return new V8ScriptObject(context, getExports());
    }

    @SneakyThrows
    @Override
    protected void doTriggerOnLoaded() {
        this.module.set(GlobalConstant.Module_Loaded, true);
        // TODO triggerOnLoaded
    }

    @Override
    protected void doTriggerOnRemove() {
        // TODO triggerOnRemove
    }
}
