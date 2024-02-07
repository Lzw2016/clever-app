package org.clever.js.graaljs;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.AbstractScriptEngineInstance;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.Folder;
import org.clever.js.graaljs.utils.EngineGlobalUtils;
import org.clever.util.Assert;
import org.graalvm.polyglot.*;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:58 <br/>
 */
@Slf4j
public class GraalScriptEngineInstance extends AbstractScriptEngineInstance<Context, Value> {

    public GraalScriptEngineInstance(ScriptEngineContext<Context, Value> engineContext) {
        super(engineContext);
        init();
    }

    protected void init() {
        Value engineBindings = this.engineContext.getEngine().getBindings(GraalConstant.JS_LANGUAGE_ID);
        Map<String, Object> registerGlobalVars = this.engineContext.getRegisterGlobalVars();
        if (registerGlobalVars != null) {
            registerGlobalVars.forEach(engineBindings::putMember);
        }
        engineBindings.putMember(GlobalConstant.ENGINE_REQUIRE, this.engineContext.getRequire());
        engineBindings.putMember(GlobalConstant.ENGINE_GLOBAL, this.engineContext.getGlobal());
    }

    @Override
    public String getEngineName() {
        final String engineName = engineContext.getEngine().getEngine().getImplementationName();
        if (GraalConstant.ERROR_ENGINE_NAME.equalsIgnoreCase(engineName)) {
            log.error("当前GraalJs未使用GraalVM compiler功能，请使用GraalVM compiler功能以提升性能(2 ~ 10倍性能提升)!");
        }
        return engineName;
    }

    @Override
    public String getEngineVersion() {
        return engineContext.getEngine().getEngine().getVersion();
    }

    @Override
    public String getLanguageVersion() {
        return "ECMAScript Version: " + GraalConstant.ECMASCRIPT_VERSION;
    }

    @Override
    protected ScriptObject<Value> wrapScriptObject(Value original) {
        return new GraalScriptObject(engineContext, original);
    }

    @Override
    protected ScriptObject<Value> createFunction(String funCode) {
        Source source = Source.newBuilder(GraalConstant.JS_LANGUAGE_ID, funCode, String.format("/__fun_autogenerate_%s.js", FUC_COUNTER.get()))
            .cached(true)
            .buildLiteral();
        Context engine = getEngine();
        Value value;
        try {
            engine.enter();
            value = engine.eval(source);
        } finally {
            engine.leave();
        }
        Assert.isTrue(value != null && value.canExecute(), String.format("脚本代码不可以执行:\n%s\n", funCode));
        return new GraalScriptObject(engineContext, value);
    }

    @Override
    public void close() {
        super.close();
        engineContext.getEngine().close(true);
    }

    @Getter
    public static class Builder extends AbstractBuilder<Context, Value> {
        private Consumer<Context.Builder> customContext;
        private Consumer<HostAccess.Builder> customHostAccess;
        private final Engine graalvmEngine;

        /**
         * @param rootPath 根路径文件夹
         */
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
        public GraalScriptEngineInstance build() {
            ScriptEngineContext<Context, Value> context = GraalScriptEngineContext.Builder.create(graalvmEngine, rootPath)
                .setCustomContext(customContext)
                .setCustomHostAccess(customHostAccess)
                .setEngine(engine)
                .setRegisterGlobalVars(registerGlobalVars)
                .setModuleCache(moduleCache)
                .setRequire(require)
                .setCompileModule(compileModule)
                .setGlobal(global)
                .build();
            return new GraalScriptEngineInstance(context);
        }
    }
}
