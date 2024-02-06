package org.clever.js.v8;

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
import org.clever.js.v8.module.V8CompileModule;
import org.clever.js.v8.module.V8Module;
import org.clever.js.v8.require.V8Require;
import org.clever.js.v8.support.V8Logger;
import org.clever.js.v8.utils.ScriptEngineUtils;

import java.util.Collections;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:41 <br/>
 */
public class V8ScriptEngineContext extends AbstractScriptEngineContext<V8Runtime, IV8ValueObject> {

    public V8ScriptEngineContext(V8Runtime engine,
                                 Map<String, Object> registerGlobalVars,
                                 Folder rootPath,
                                 ModuleCache<IV8ValueObject> moduleCache,
                                 Require<IV8ValueObject> require,
                                 CompileModule<IV8ValueObject> compileModule,
                                 IV8ValueObject global) {
        super(engine, registerGlobalVars, rootPath, moduleCache, require, compileModule, global);
    }

    protected V8ScriptEngineContext(V8Runtime engine,
                                    Map<String, Object> registerGlobalVars,
                                    Folder rootPath,
                                    ModuleCache<IV8ValueObject> moduleCache) {
        super(engine, registerGlobalVars, rootPath, moduleCache);
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
            // engine
            if (engine == null) {
                engine = V8Host.getNodeInstance().createV8Runtime();
                engine.setLogger(V8Logger.Instance);
                engine.allowEval(true);
            }
            // registerGlobalVars
            if (registerGlobalVars == null) {
                registerGlobalVars = Collections.emptyMap();
            }
            // moduleCache
            if (moduleCache == null) {
                moduleCache = new MemoryModuleCache<>();
            }
            V8ScriptEngineContext engineContext = new V8ScriptEngineContext(engine, registerGlobalVars, rootPath, moduleCache);
            // require
            if (require == null) {
                V8Module mainModule = V8Module.createMainModule(engineContext);
                require = new V8Require(engineContext, mainModule, rootPath);
            }
            engineContext.require = require;
            // compileModule
            if (compileModule == null) {
                compileModule = new V8CompileModule(engineContext);
            }
            engineContext.compileModule = compileModule;
            // global
            if (global == null) {
                global = ScriptEngineUtils.newObject(engine);
            }
            engineContext.global = global;
            return engineContext;
        }
    }
}
