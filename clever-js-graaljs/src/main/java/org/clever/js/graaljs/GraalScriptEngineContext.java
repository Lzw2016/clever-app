package org.clever.js.graaljs;

import org.clever.js.api.AbstractScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.CompileModule;
import org.clever.js.api.module.MemoryModuleCache;
import org.clever.js.api.module.ModuleCache;
import org.clever.js.api.require.Require;
import org.clever.js.graaljs.module.GraalCompileModule;
import org.clever.js.graaljs.module.GraalModule;
import org.clever.js.graaljs.require.GraalRequire;
import org.clever.js.graaljs.utils.EngineGlobalUtils;
import org.clever.js.graaljs.utils.ScriptEngineUtils;
import org.clever.util.Assert;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:58 <br/>
 */
public class GraalScriptEngineContext extends AbstractScriptEngineContext<Context, Value> {
    public GraalScriptEngineContext() {
        super();
    }

    public GraalScriptEngineContext(
            Context engine,
            Map<String, Object> contextMap,
            Folder rootPath,
            ModuleCache<Value> moduleCache,
            Require<Value> require,
            CompileModule<Value> compileModule,
            Value global) {
        super(engine, contextMap, rootPath, moduleCache, require, compileModule, global);
    }

    public static class Builder extends AbstractBuilder<Context, Value> {
        private Set<Class<?>> denyAccessClass = new HashSet<>();
        private final Engine graalvmEngine;

        public Builder(Engine graalvmEngine, Folder rootPath) {
            super(rootPath);
            this.graalvmEngine = graalvmEngine;
            // 自定义 contextMap
            EngineGlobalUtils.putGlobalObjects(contextMap);
        }

        public static Builder create(Engine graalvmEngine, Folder rootPath) {
            return new Builder(graalvmEngine, rootPath);
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
        public GraalScriptEngineContext build() {
            GraalScriptEngineContext context = new GraalScriptEngineContext();
            // engine
            if (engine == null) {
                Assert.notNull(graalvmEngine, "参数graalvmEngine或者engine不能为空");
                engine = ScriptEngineUtils.creatEngine(graalvmEngine, denyAccessClass);
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
                GraalModule mainModule = GraalModule.createMainModule(context);
                require = new GraalRequire(context, mainModule, rootPath);
            }
            context.require = require;
            // compileModule
            if (compileModule == null) {
                compileModule = new GraalCompileModule(context);
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
