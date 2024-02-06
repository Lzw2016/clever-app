package org.clever.js.v8.module;


import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.IV8ValueObject;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.folder.ReadFileContentException;
import org.clever.js.api.module.AbstractCompileModule;
import org.clever.js.v8.utils.ScriptEngineUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 12:41 <br/>
 */
public class V8CompileModule extends AbstractCompileModule<V8Runtime, IV8ValueObject> {

    public V8CompileModule(ScriptEngineContext<V8Runtime, IV8ValueObject> context) {
        super(context);
    }

    @Override
    public IV8ValueObject compileJsonModule(Folder path) {
        final String json = path.getFileContent();
        if (StringUtils.isBlank(json)) {
            throw new ReadFileContentException("读取文件Json内容失败: path=" + path.getFullPath());
        }
        return ScriptEngineUtils.parseJson(engineContext.getEngine(), json);
    }

    @SneakyThrows
    @Override
    public IV8ValueObject compileScriptModule(Folder path) {
        final String code = path.getFileContent();
        if (code == null) {
            throw new ReadFileContentException("读取文件内容失败: path=" + path.getFullPath());
        }
        final String moduleScriptCode = getModuleScriptCode(code);
        return engineContext.getEngine().getExecutor(moduleScriptCode).execute();
    }
}
