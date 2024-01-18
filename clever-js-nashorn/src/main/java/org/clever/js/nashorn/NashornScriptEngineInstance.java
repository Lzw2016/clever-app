package org.clever.js.nashorn;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.clever.js.api.AbstractScriptEngineInstance;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.internal.LoggerConsole;
import org.clever.js.nashorn.internal.NashornLoggerFactory;
import org.clever.js.nashorn.internal.support.NashornObjectToString;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 21:25 <br/>
 */
public class NashornScriptEngineInstance extends AbstractScriptEngineInstance<NashornScriptEngine, ScriptObjectMirror> {

    public NashornScriptEngineInstance(ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context) {
        super(context);
        Bindings engineBindings = this.context.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
        Map<String, Object> contextMap = this.context.getContextMap();
        if (contextMap != null) {
            engineBindings.putAll(contextMap);
        }
        engineBindings.put(GlobalConstant.Engine_Require, this.context.getRequire());
        engineBindings.put(GlobalConstant.Engine_Global, this.context.getGlobal());
    }

    @Override
    public String getEngineName() {
        return context.getEngine().getFactory().getEngineName();
    }

    @Override
    public String getEngineVersion() {
        return context.getEngine().getFactory().getEngineVersion();
    }

    @Override
    public String getLanguageVersion() {
        return context.getEngine().getFactory().getLanguageVersion();
    }

    @Override
    protected ScriptObject<ScriptObjectMirror> newScriptObject(ScriptObjectMirror scriptObject) {
        return new NashornScriptObject(context, scriptObject);
    }

    @Override
    public void close() {
    }

    public static class Builder extends AbstractBuilder<NashornScriptEngine, ScriptObjectMirror> {
        private Set<Class<?>> denyAccessClass = new HashSet<>();

        /**
         * @param rootPath 根路径文件夹
         */
        public Builder(Folder rootPath) {
            super(rootPath);
            // 自定义 contextMap
            LoggerConsole.Instance.setObjectToString(NashornObjectToString.Instance);
            contextMap.put("console", LoggerConsole.Instance);
            contextMap.put("print", LoggerConsole.Instance);
            contextMap.put("LoggerFactory", NashornLoggerFactory.Instance);
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
        public NashornScriptEngineInstance build() {
            ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context = NashornScriptEngineContext.Builder.create(rootPath)
                    .setDenyAccessClass(denyAccessClass)
                    .setEngine(engine)
                    .setContextMap(contextMap)
                    .setModuleCache(moduleCache)
                    .setRequire(require)
                    .setCompileModule(compileModule)
                    .setGlobal(global)
                    .build();
            return new NashornScriptEngineInstance(context);
        }
    }
}
