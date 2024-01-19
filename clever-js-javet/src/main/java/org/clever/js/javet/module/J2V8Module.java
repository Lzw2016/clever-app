//package org.clever.js.javet.module;
//
//import com.eclipsesource.v8.JavaCallback;
//import com.eclipsesource.v8.V8;
//import com.eclipsesource.v8.V8Object;
//import lombok.extern.slf4j.Slf4j;
//import org.clever.js.api.GlobalConstant;
//import org.clever.js.api.ScriptEngineContext;
//import org.clever.js.api.ScriptObject;
//import org.clever.js.api.module.AbstractModule;
//import org.clever.js.api.module.Module;
//import org.clever.js.api.require.Require;
//import org.clever.js.api.utils.Assert;
//import org.clever.js.j2v8.J2V8ScriptObject;
//import org.clever.js.j2v8.utils.ScriptEngineUtils;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/07/22 12:41 <br/>
// */
//@Slf4j
//public class J2V8Module extends AbstractModule<V8, V8Object> {
//
//    public J2V8Module(
//            ScriptEngineContext<V8, V8Object> context,
//            String id,
//            String filename,
//            V8Object exports,
//            Module<V8Object> parent,
//            Require<V8Object> require) {
//        super(context, id, filename, exports, parent, require);
//        Assert.isTrue(require instanceof JavaCallback, "参数require必须实现JavaCallback接口");
//    }
//
//    protected J2V8Module(ScriptEngineContext<V8, V8Object> context) {
//        super(context);
//    }
//
//    /**
//     * 创建主模块(根模块)
//     */
//    public static J2V8Module createMainModule(ScriptEngineContext<V8, V8Object> context) {
//        return new J2V8Module(context);
//    }
//
//    @Override
//    protected void initModule(V8Object exports) {
//        this.module.add(GlobalConstant.Module_Id, this.id);
//        this.module.add(GlobalConstant.Module_Filename, this.filename);
//        this.module.add(GlobalConstant.Module_Loaded, this.loaded);
//        if (this.parent != null) {
//            this.module.add(GlobalConstant.Module_Parent, this.parent.getModule());
//        }
//        // TODO  Module_Paths
//        this.module.add(GlobalConstant.Module_Paths, ScriptEngineUtils.newArray(context.getEngine()));
//        // TODO  Module_Children
//        this.module.add(GlobalConstant.Module_Children, ScriptEngineUtils.newArray(context.getEngine()));
//        this.module.add(GlobalConstant.Module_Exports, exports);
//        // Assert.isTrue(this.require instanceof JavaCallback, "参数require必须实现JavaCallback接口");
//        // this.module.registerJavaMethod((JavaCallback) this.require, GlobalConstant.Module_Require);
//    }
//
//    @Override
//    protected V8Object newScriptObject() {
//        return ScriptEngineUtils.newObject(context.getEngine());
//    }
//
//    @Override
//    public V8Object getExports() {
//        return this.module.getObject(GlobalConstant.Module_Exports);
//    }
//
//    @Override
//    public ScriptObject<V8Object> getExportsWrapper() {
//        return new J2V8ScriptObject(context, getExports());
//    }
//
//    @Override
//    protected void doTriggerOnLoaded() {
//        this.module.add(GlobalConstant.Module_Loaded, true);
//        // TODO triggerOnLoaded
//    }
//
//    @Override
//    protected void doTriggerOnRemove() {
//        // TODO triggerOnRemove
//    }
//}
