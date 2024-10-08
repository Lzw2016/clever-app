package org.clever.js.nashorn.module;

import org.apache.commons.lang3.StringUtils;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.folder.ReadFileContentException;
import org.clever.js.api.module.AbstractCompileModule;
import org.clever.js.nashorn.utils.ScriptEngineUtils;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

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
    public ScriptObjectMirror compileScriptModule(Folder path) throws Exception {
        final String code = path.getFileContent();
        if (code == null) {
            throw new ReadFileContentException("读取文件内容失败: path=" + path.getFullPath());
        }
        final String moduleScriptCode = getModuleScriptCode(code);
        return (ScriptObjectMirror) engineContext.getEngine().eval(moduleScriptCode);
    }
}
