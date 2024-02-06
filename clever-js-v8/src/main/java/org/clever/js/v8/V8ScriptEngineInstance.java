package org.clever.js.v8;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.utils.JavetResourceUtils;
import com.caoccao.javet.values.reference.IV8ValueObject;
import com.caoccao.javet.values.reference.V8ValueGlobalObject;
import lombok.SneakyThrows;
import org.clever.js.api.AbstractScriptEngineInstance;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.Folder;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:40 <br/>
 */
public class V8ScriptEngineInstance extends AbstractScriptEngineInstance<V8Runtime, IV8ValueObject> {

    @SneakyThrows
    public V8ScriptEngineInstance(ScriptEngineContext<V8Runtime, IV8ValueObject> context) {
        super(context);
        V8ValueGlobalObject globalObject = this.engineContext.getEngine().getGlobalObject();
        Map<String, Object> registerGlobalVars = this.engineContext.getRegisterGlobalVars();
        if (registerGlobalVars != null) {
            for (Map.Entry<String, Object> entry : registerGlobalVars.entrySet()) {
                // TODO registerJavaMethod
                globalObject.set(entry.getKey(), entry.getValue());
            }
        }
        // TODO registerJavaMethod
        globalObject.set(GlobalConstant.ENGINE_REQUIRE, this.engineContext.getRequire());
        globalObject.set(GlobalConstant.ENGINE_GLOBAL, this.engineContext.getGlobal());
    }

    @Override
    public String getEngineName() {
        return "V8";
    }

    @Override
    public String getEngineVersion() {
        return "V8 Version: " + engineContext.getEngine().getVersion();
    }

    @Override
    public String getLanguageVersion() {
        return "unknown";
    }

    @Override
    protected ScriptObject<IV8ValueObject> newScriptObject(IV8ValueObject scriptObject) {
        return new V8ScriptObject(engineContext, scriptObject);
    }

    @Override
    public void close() {
        V8Runtime v8runtime = engineContext.getEngine();
        JavetResourceUtils.safeClose(v8runtime);
    }

    public static class Builder extends AbstractBuilder<V8Runtime, IV8ValueObject> {
        /**
         * @param rootPath 根路径文件夹
         */
        public Builder(Folder rootPath) {
            super(rootPath);
        }

        public static Builder create(Folder rootPath) {
            return new Builder(rootPath);
        }

        /**
         * 创建 ScriptEngineContext
         */
        public V8ScriptEngineInstance build() {
            ScriptEngineContext<V8Runtime, IV8ValueObject> context = V8ScriptEngineContext.Builder.create(rootPath)
                .setEngine(engine)
                .setRegisterGlobalVars(registerGlobalVars)
                .setModuleCache(moduleCache)
                .setRequire(require)
                .setCompileModule(compileModule)
                .setGlobal(global)
                .build();
            return new V8ScriptEngineInstance(context);
        }
    }
}
