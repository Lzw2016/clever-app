package org.clever.js.graaljs;

import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.AbstractScriptEngineInstance;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.Folder;
import org.clever.js.graaljs.utils.EngineGlobalUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:58 <br/>
 */
@Slf4j
public class GraalScriptEngineInstance extends AbstractScriptEngineInstance<Context, Value> {

    public GraalScriptEngineInstance(ScriptEngineContext<Context, Value> context) {
        super(context);
        Value engineBindings = this.context.getEngine().getBindings(GraalConstant.Js_Language_Id);
        Map<String, Object> contextMap = this.context.getContextMap();
        if (contextMap != null) {
            contextMap.forEach(engineBindings::putMember);
        }
        engineBindings.putMember(GlobalConstant.Engine_Require, this.context.getRequire());
        engineBindings.putMember(GlobalConstant.Engine_Global, this.context.getGlobal());
    }

    @Override
    public String getEngineName() {
        final String engineName = context.getEngine().getEngine().getImplementationName();
        if (GraalConstant.Error_Engine_Name.equalsIgnoreCase(engineName)) {
            log.error("当前GraalJs未使用GraalVM compiler功能，请使用GraalVM compiler功能以提升性能(2 ~ 10倍性能提升)!");
        }
        return engineName;
    }

    @Override
    public String getEngineVersion() {
        return context.getEngine().getEngine().getVersion();
    }

    @Override
    public String getLanguageVersion() {
        return "ECMAScript Version: " + GraalConstant.ECMAScript_Version;
    }

    @Override
    protected ScriptObject<Value> newScriptObject(Value scriptObject) {
        return new GraalScriptObject(context, scriptObject);
    }

    @Override
    public void close() {
        context.getEngine().close(true);
    }

    public static class Builder extends AbstractBuilder<Context, Value> {
        private Consumer<Context.Builder> customContext;
        private Consumer<HostAccess.Builder> customHostAccess;
        private final Engine graalEngine;

        /**
         * @param rootPath 根路径文件夹
         */
        public Builder(Engine graalEngine, Folder rootPath) {
            super(rootPath);
            this.graalEngine = graalEngine;
            // 自定义 contextMap
            EngineGlobalUtils.putGlobalObjects(contextMap);
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
        public GraalScriptEngineInstance build() {
            ScriptEngineContext<Context, Value> context = GraalScriptEngineContext.Builder.create(graalEngine, rootPath)
                .setCustomContext(customContext)
                .setCustomHostAccess(customHostAccess)
                .setEngine(engine)
                .setContextMap(contextMap)
                .setModuleCache(moduleCache)
                .setRequire(require)
                .setCompileModule(compileModule)
                .setGlobal(global)
                .build();
            return new GraalScriptEngineInstance(context);
        }
    }
}
