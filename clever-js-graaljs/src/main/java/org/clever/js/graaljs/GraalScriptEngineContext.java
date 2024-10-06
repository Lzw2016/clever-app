package org.clever.js.graaljs;

import org.clever.core.Assert;
import org.clever.js.api.AbstractScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.Cache;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.MemoryCache;
import org.clever.js.api.module.Module;
import org.clever.js.api.require.Require;
import org.clever.js.graaljs.module.GraalCompileModule;
import org.clever.js.graaljs.module.GraalModule;
import org.clever.js.graaljs.require.GraalRequire;
import org.clever.js.graaljs.utils.EngineGlobalUtils;
import org.clever.js.graaljs.utils.ScriptEngineUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:58 <br/>
 */
public class GraalScriptEngineContext extends AbstractScriptEngineContext<Context, Value> {

    public GraalScriptEngineContext(Context engine,
                                    Map<String, Object> registerGlobalVars,
                                    Folder rootPath,
                                    Cache<Module<Value>> moduleCache,
                                    Cache<ScriptObject<Value>> functionCache,
                                    Require<Value> require,
                                    CompileModule<Value> compileModule,
                                    Value global) {
        super(engine, registerGlobalVars, rootPath, moduleCache, functionCache, require, compileModule, global);
    }

    protected GraalScriptEngineContext(Context engine,
                                       Map<String, Object> registerGlobalVars,
                                       Folder rootPath,
                                       Cache<Module<Value>> moduleCache,
                                       Cache<ScriptObject<Value>> functionCache) {
        super(engine, registerGlobalVars, rootPath, moduleCache, functionCache);
    }

    public static class Builder extends AbstractBuilder<Context, Value> {
        private Consumer<Context.Builder> customContext;
        private Consumer<HostAccess.Builder> customHostAccess;
        private final Engine graalvmEngine;

        public Builder(Engine graalvmEngine, Folder rootPath) {
            super(rootPath);
            this.graalvmEngine = graalvmEngine;
            // 自定义 registerGlobalVars
            EngineGlobalUtils.putGlobalObjects(registerGlobalVars);
        }

        public static Builder create(Engine graalvmEngine, Folder rootPath) {
            return new Builder(graalvmEngine, rootPath);
        }

        /**
         * 设置自定义 Context 逻辑
         */
        public Builder setCustomContext(Consumer<Context.Builder> customContext) {
            this.customContext = customContext;
            return this;
        }

        /**
         * 设置自定义 HostAccess 逻辑
         */
        public Builder setCustomHostAccess(Consumer<HostAccess.Builder> customHostAccess) {
            this.customHostAccess = customHostAccess;
            return this;
        }

        /**
         * 创建 ScriptEngineContext
         */
        public GraalScriptEngineContext build() {
            // engine
            if (engine == null) {
                Assert.notNull(graalvmEngine, "参数graalvmEngine或者engine不能为空");
                engine = ScriptEngineUtils.creatEngine(graalvmEngine, customContext, customHostAccess);
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
            GraalScriptEngineContext engineContext = new GraalScriptEngineContext(engine, registerGlobalVars, rootPath, moduleCache, functionCache);
            // require
            if (require == null) {
                GraalModule mainModule = GraalModule.createMainModule(engineContext);
                require = new GraalRequire(engineContext, mainModule, rootPath);
            }
            engineContext.require = require;
            // compileModule
            if (compileModule == null) {
                compileModule = new GraalCompileModule(engineContext);
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
