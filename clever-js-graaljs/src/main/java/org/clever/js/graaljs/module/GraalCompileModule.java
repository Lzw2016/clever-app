package org.clever.js.graaljs.module;

import org.apache.commons.lang3.StringUtils;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.folder.ReadFileContentException;
import org.clever.js.api.module.AbstractCompileModule;
import org.clever.js.graaljs.GraalConstant;
import org.clever.js.graaljs.utils.ScriptEngineUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/20 21:57 <br/>
 */
public class GraalCompileModule extends AbstractCompileModule<Context, Value> {

    public GraalCompileModule(ScriptEngineContext<Context, Value> engineContext) {
        super(engineContext);
    }

    @Override
    public Value compileJsonModule(Folder path) {
        final String json = path.getFileContent();
        if (StringUtils.isBlank(json)) {
            throw new ReadFileContentException("读取文件Json内容失败: path=" + path.getFullPath());
        }
        return ScriptEngineUtils.parseJson(engineContext.getEngine(), json);
    }

    @Override
    public Value compileScriptModule(Folder path) {
        final String code = path.getFileContent();
        if (code == null) {
            throw new ReadFileContentException("读取文件内容失败: path=" + path.getFullPath());
        }
        final String moduleScriptCode = getModuleScriptCode(code);
        Source source = Source.newBuilder(GraalConstant.Js_Language_Id, moduleScriptCode, path.getFullPath()).cached(true).buildLiteral();
        Context engine = engineContext.getEngine();
        Value modelFunction;
        try {
            engine.enter();
            modelFunction = engineContext.getEngine().eval(source);
        } finally {
            if (engine != null) {
                engine.leave();
            }
        }
        return modelFunction;
    }
}
