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
import org.clever.js.nashorn.support.NashornObjectToString;

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
        Bindings engineBindings = this.engineContext.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
        Map<String, Object> registerGlobalVars = this.engineContext.getRegisterGlobalVars();
        if (registerGlobalVars != null) {
            engineBindings.putAll(registerGlobalVars);
        }
        engineBindings.put(GlobalConstant.Engine_Require, this.engineContext.getRequire());
        engineBindings.put(GlobalConstant.Engine_Global, this.engineContext.getGlobal());
    }

    @Override
    public String getEngineName() {
        return engineContext.getEngine().getFactory().getEngineName();
    }

    @Override
    public String getEngineVersion() {
        return engineContext.getEngine().getFactory().getEngineVersion();
    }

    @Override
    public String getLanguageVersion() {
        return engineContext.getEngine().getFactory().getLanguageVersion();
    }

    @Override
    protected ScriptObject<ScriptObjectMirror> newScriptObject(ScriptObjectMirror scriptObject) {
        return new NashornScriptObject(engineContext, scriptObject);
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
            // 自定义 registerGlobalVars
            LoggerConsole.Instance.setObjectToString(NashornObjectToString.Instance);
            registerGlobalVars.put("console", LoggerConsole.Instance);
            registerGlobalVars.put("print", LoggerConsole.Instance);
            registerGlobalVars.put("LoggerFactory", NashornLoggerFactory.Instance);
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
                    .setRegisterGlobalVars(registerGlobalVars)
                    .setModuleCache(moduleCache)
                    .setRequire(require)
                    .setCompileModule(compileModule)
                    .setGlobal(global)
                    .build();
            return new NashornScriptEngineInstance(context);
        }
    }
}
