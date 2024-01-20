package org.clever.js.javet;

import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.IV8ValueObject;
import lombok.SneakyThrows;
import org.clever.js.api.AbstractScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.MemoryModuleCache;
import org.clever.js.api.module.ModuleCache;
import org.clever.js.api.require.Require;
import org.clever.js.javet.module.V8CompileModule;
import org.clever.js.javet.module.V8Module;
import org.clever.js.javet.require.V8Require;
import org.clever.js.javet.support.V8Logger;
import org.clever.js.javet.utils.ScriptEngineUtils;

import java.util.Collections;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:41 <br/>
 */
public class V8ScriptEngineContext extends AbstractScriptEngineContext<V8Runtime, IV8ValueObject> {

    public V8ScriptEngineContext() {
        super();
    }

    public V8ScriptEngineContext(V8Runtime engine,
                                 Map<String, Object> contextMap,
                                 Folder rootPath,
                                 ModuleCache<IV8ValueObject> moduleCache,
                                 Require<IV8ValueObject> require,
                                 CompileModule<IV8ValueObject> compileModule,
                                 IV8ValueObject global) {
        super(engine, contextMap, rootPath, moduleCache, require, compileModule, global);
    }

    public static class Builder extends AbstractBuilder<V8Runtime, IV8ValueObject> {

        public Builder(Folder rootPath) {
            super(rootPath);
        }

        public static Builder create(Folder rootPath) {
            return new Builder(rootPath);
        }

        /**
         * 创建 ScriptEngineContext
         */
        @SneakyThrows
        public V8ScriptEngineContext build() {
            V8ScriptEngineContext context = new V8ScriptEngineContext();
            // engine
            if (engine == null) {
                engine = V8Host.getNodeInstance().createV8Runtime();
                engine.setLogger(V8Logger.Instance);
                engine.allowEval(true);
            }
            context.engine = engine;
            // contextMap
            if (contextMap == null) {
                contextMap = Collections.emptyMap();
            }
            context.contextMap = contextMap;
            // rootPath
            context.rootPath = rootPath;
            // moduleCache
            if (moduleCache == null) {
                moduleCache = new MemoryModuleCache<>();
            }
            context.moduleCache = moduleCache;
            // require
            if (require == null) {
                V8Module mainModule = V8Module.createMainModule(context);
                require = new V8Require(context, mainModule, rootPath);
            }
            context.require = require;
            // compileModule
            if (compileModule == null) {
                compileModule = new V8CompileModule(context);
            }
            context.compileModule = compileModule;
            // global
            if (global == null) {
                global = ScriptEngineUtils.newObject(engine);
            }
            context.global = global;
            return context;
        }
    }
}
