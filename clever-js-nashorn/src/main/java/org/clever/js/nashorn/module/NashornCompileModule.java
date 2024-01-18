package org.clever.js.nashorn.module;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.StringUtils;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.folder.ReadFileContentException;
import org.clever.js.api.module.AbstractCompileModule;
import org.clever.js.nashorn.utils.ScriptEngineUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/18 09:20 <br/>
 */
public class NashornCompileModule extends AbstractCompileModule<NashornScriptEngine, ScriptObjectMirror> {

    public NashornCompileModule(ScriptEngineContext<NashornScriptEngine, ScriptObjectMirror> context) {
        super(context);
    }

    @Override
    public ScriptObjectMirror compileJsonModule(Folder path) {
        final String json = path.getFileContent();
        if (StringUtils.isBlank(json)) {
            throw new ReadFileContentException("读取文件Json内容失败: path=" + path.getFullPath());
        }
        return ScriptEngineUtils.parseJson(json);
    }

    @Override
    public ScriptObjectMirror compileJavaScriptModule(Folder path) throws Exception {
        final String code = path.getFileContent();
        if (code == null) {
            throw new ReadFileContentException("读取文件内容失败: path=" + path.getFullPath());
        }
        final String moduleScriptCode = getModuleScriptCode(code);
        return (ScriptObjectMirror) context.getEngine().eval(moduleScriptCode);
    }
}
