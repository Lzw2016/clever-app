//package org.clever.js.javet.require;
//
//import com.eclipsesource.v8.*;
//import org.clever.js.api.ScriptEngineContext;
//import org.clever.js.api.folder.Folder;
//import org.clever.js.api.module.Module;
//import org.clever.js.api.require.AbstractRequire;
//import org.clever.js.api.require.Require;
//import org.clever.js.j2v8.module.J2V8Module;
//import org.clever.js.j2v8.utils.ScriptEngineUtils;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/07/22 12:41 <br/>
// */
//public class J2V8Require extends AbstractRequire<V8, V8Object> implements JavaCallback {
//
//    public J2V8Require(
//            ScriptEngineContext<V8, V8Object> context,
//            Module<V8Object> currentModule,
//            Folder currentModuleFolder) {
//        super(context, currentModule, currentModuleFolder);
//    }
//
//    protected J2V8Require(ScriptEngineContext<V8, V8Object> context, Folder currentModuleFolder) {
//        super(context, currentModuleFolder);
//    }
//
//    @Override
//    protected V8Object newScriptObject() {
//        return ScriptEngineUtils.newObject(context.getEngine());
//    }
//
//    @Override
//    protected AbstractRequire<V8, V8Object> newRequire(ScriptEngineContext<V8, V8Object> context, Folder currentModuleFolder) {
//        return new J2V8Require(context, currentModuleFolder);
//    }
//
//    @Override
//    protected Module<V8Object> newModule(
//            ScriptEngineContext<V8, V8Object> context,
//            String id,
//            String filename,
//            V8Object exports,
//            Module<V8Object> parent,
//            Require<V8Object> require) {
//        return new J2V8Module(context, id, filename, exports, parent, require);
//    }
//
//    @Override
//    protected void moduleFunctionCall(
//            V8Object function,
//            V8Object that,
//            V8Object exports,
//            Require<V8Object> require,
//            V8Object module,
//            String filename,
//            String dirname) {
//        if (!(function instanceof V8Function)) {
//            throw new UnsupportedOperationException("不支持的操作");
//        }
//        V8Array parameters = new V8Array(context.getEngine());
//        parameters.push(exports);
//        V8Function requireFunction = new V8Function(context.getEngine(), (J2V8Require) require);
//        parameters.push(requireFunction);
//        parameters.push(module);
//        parameters.push(filename);
//        parameters.push(dirname);
//        ((V8Function) function).call(that, parameters);
//        // parameters.release();
//        // requireFunction.release();
//    }
//
//    @Override
//    public Object invoke(V8Object receiver, V8Array parameters) {
//        return this.require(parameters.getString(0));
//    }
//}
