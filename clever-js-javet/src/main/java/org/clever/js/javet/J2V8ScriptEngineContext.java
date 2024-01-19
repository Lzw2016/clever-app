//package org.clever.js.javet;
//
//import com.eclipsesource.v8.V8;
//import com.eclipsesource.v8.V8Object;
//import org.clever.js.api.AbstractScriptEngineContext;
//import org.clever.js.api.folder.Folder;
//import org.clever.js.api.module.CompileModule;
//import org.clever.js.api.module.MemoryModuleCache;
//import org.clever.js.api.module.ModuleCache;
//import org.clever.js.api.require.Require;
//import org.clever.js.j2v8.module.J2V8CompileModule;
//import org.clever.js.j2v8.module.J2V8Module;
//import org.clever.js.j2v8.require.J2V8Require;
//import org.clever.js.j2v8.utils.ScriptEngineUtils;
//
//import java.util.Collections;
//import java.util.Map;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/07/22 12:41 <br/>
// */
//public class J2V8ScriptEngineContext extends AbstractScriptEngineContext<V8, V8Object> {
//
//    public J2V8ScriptEngineContext() {
//        super();
//    }
//
//    public J2V8ScriptEngineContext(
//            V8 engine,
//            Map<String, Object> contextMap,
//            Folder rootPath,
//            ModuleCache<V8Object> moduleCache,
//            Require<V8Object> require,
//            CompileModule<V8Object> compileModule,
//            V8Object global) {
//        super(engine, contextMap, rootPath, moduleCache, require, compileModule, global);
//    }
//
//    public static class Builder extends AbstractBuilder<V8, V8Object> {
//
//        public Builder(Folder rootPath) {
//            super(rootPath);
//        }
//
//        public static Builder create(Folder rootPath) {
//            return new Builder(rootPath);
//        }
//
//        /**
//         * 创建 ScriptEngineContext
//         */
//        public J2V8ScriptEngineContext build() {
//            J2V8ScriptEngineContext context = new J2V8ScriptEngineContext();
//            // engine
//            if (engine == null) {
//                engine = V8.createV8Runtime();
//            }
//            context.engine = engine;
//            // contextMap
//            if (contextMap == null) {
//                contextMap = Collections.emptyMap();
//            }
//            context.contextMap = contextMap;
//            // rootPath
//            context.rootPath = rootPath;
//            // moduleCache
//            if (moduleCache == null) {
//                moduleCache = new MemoryModuleCache<>();
//            }
//            context.moduleCache = moduleCache;
//            // require
//            if (require == null) {
//                J2V8Module mainModule = J2V8Module.createMainModule(context);
//                require = new J2V8Require(context, mainModule, rootPath);
//            }
//            context.require = require;
//            // compileModule
//            if (compileModule == null) {
//                compileModule = new J2V8CompileModule(context);
//            }
//            context.compileModule = compileModule;
//            // global
//            if (global == null) {
//                global = ScriptEngineUtils.newObject(engine);
//            }
//            context.global = global;
//            return context;
//        }
//    }
//}
