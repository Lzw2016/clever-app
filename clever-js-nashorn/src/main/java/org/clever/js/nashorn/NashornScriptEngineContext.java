package org.clever.js.nashorn;

import org.clever.js.api.AbstractScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.internal.LoggerConsole;
import org.clever.js.api.module.Cache;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.MemoryCache;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.Require;
import org.clever.js.nashorn.internal.NashornLoggerFactory;
import org.clever.js.nashorn.module.NashornCompileModule;
import org.clever.js.nashorn.module.NashornModule;
import org.clever.js.nashorn.require.NashornRequire;
import org.clever.js.nashorn.support.NashornObjectToString;
import org.clever.js.nashorn.utils.ScriptEngineUtils;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 21:53 <br/>
 */
public class NashornScriptEngineContext extends AbstractScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> {

    public NashornScriptEngineContext(NashornScriptEngine engine,
                                      Map<String, Object> registerGlobalVars,
                                      Folder rootPath,
                                      Cache<Module<ScriptObjectMirror>> moduleCache,
                                      Cache<ScriptObject<ScriptObjectMirror>> functionCache,
                                      Require<ScriptObjectMirror> require,
                                      CompileModule<ScriptObjectMirror> compileModule,
                                      ScriptObjectMirror global) {
        super(engine, registerGlobalVars, rootPath, moduleCache, functionCache, require, compileModule, global);
    }

    public NashornScriptEngineContext(NashornScriptEngine engine,
                                      Map<String, Object> registerGlobalVars,
                                      Folder rootPath,
                                      Cache<Module<ScriptObjectMirror>> moduleCache,
                                      Cache<ScriptObject<ScriptObjectMirror>> functionCache) {
        super(engine, registerGlobalVars, rootPath, moduleCache, functionCache);
    }

    public static class Builder extends AbstractBuilder<NashornScriptEngine, ScriptObjectMirror> {
        private Set<Class<?>> denyAccessClass = new HashSet<>();

        public Builder(Folder rootPath) {
            super(rootPath);
            // 自定义 registerGlobalVars
            LoggerConsole.INSTANCE.setObjectToString(NashornObjectToString.INSTANCE);
            registerGlobalVars.put("console", LoggerConsole.INSTANCE);
            registerGlobalVars.put("print", LoggerConsole.INSTANCE);
            registerGlobalVars.put("LoggerFactory", NashornLoggerFactory.INSTANCE);
        }

        public static Builder create(Folder rootPath) {
            return new Builder(rootPath);
        }

        /**
         * 增加JavaScript不允许访问的Class
         */
        public Builder addDenyAccessClass(Class<?> clazz) {
            if (denyAccessClass != null && clazz != null) {
                denyAccessClass.add(clazz);
            }
            return this;
        }

        /**
         * 设置JavaScript不允许访问的Class
         */
        public Builder setDenyAccessClass(Set<Class<?>> denyAccessClass) {
            this.denyAccessClass = denyAccessClass;
            return this;
        }

        /**
         * 创建 ScriptEngineContext
         */
        public NashornScriptEngineContext build() {
            // engine
            if (engine == null) {
                engine = ScriptEngineUtils.creatEngine(denyAccessClass);
            }
            // registerGlobalVars
            if (registerGlobalVars == null) {
                registerGlobalVars = Collections.emptyMap();
            }
            // moduleCache
            if (moduleCache == null) {
                moduleCache = new MemoryCache<>();
            }
            // functionCache
            if (functionCache == null) {
                functionCache = new MemoryCache<>();
            }
            NashornScriptEngineContext engineContext = new NashornScriptEngineContext(engine, registerGlobalVars, rootPath, moduleCache, functionCache);
            // require
            if (require == null) {
                NashornModule mainModule = NashornModule.createMainModule(engineContext);
                require = new NashornRequire(engineContext, mainModule, rootPath);
            }
            engineContext.require = require;
            // compileModule
            if (compileModule == null) {
                compileModule = new NashornCompileModule(engineContext);
            }
            engineContext.compileModule = compileModule;
            // global
            if (global == null) {
                global = ScriptEngineUtils.newObject();
            }
            engineContext.global = global;
            return engineContext;
        }
    }
}
