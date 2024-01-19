//package org.clever.js.javet.module;
//
//import com.eclipsesource.v8.V8;
//import com.eclipsesource.v8.V8Object;
//import org.apache.commons.lang3.StringUtils;
//import org.clever.js.api.ScriptEngineContext;
//import org.clever.js.api.folder.Folder;
//import org.clever.js.api.folder.ReadFileContentException;
//import org.clever.js.api.module.AbstractCompileModule;
//import org.clever.js.j2v8.utils.ScriptEngineUtils;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/07/22 12:41 <br/>
// */
//public class J2V8CompileModule extends AbstractCompileModule<V8, V8Object> {
//
//    public J2V8CompileModule(ScriptEngineContext<V8, V8Object> context) {
//        super(context);
//    }
//
//    @Override
//    public V8Object compileJsonModule(Folder path) {
//        final String json = path.getFileContent();
//        if (StringUtils.isBlank(json)) {
//            throw new ReadFileContentException("读取文件Json内容失败: path=" + path.getFullPath());
//        }
//        return ScriptEngineUtils.parseJson(context.getEngine(), json);
//    }
//
//    @Override
//    public V8Object compileJavaScriptModule(Folder path) {
//        final String code = path.getFileContent();
//        if (code == null) {
//            throw new ReadFileContentException("读取文件内容失败: path=" + path.getFullPath());
//        }
//        final String moduleScriptCode = getModuleScriptCode(code);
//        return context.getEngine().executeObjectScript(moduleScriptCode, path.getFullPath(), -1);
//    }
//}
