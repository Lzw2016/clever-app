package org.clever.js.v8;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.callback.JavetCallbackContext;
import com.caoccao.javet.values.reference.IV8ValueObject;
import com.caoccao.javet.values.reference.V8ValueFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/22 21:57 <br/>
 */
@Slf4j
public class V8ScriptEngineInstanceTest {
    public static class JPrint {
        public Object print(Object... parameters) {
            StringBuilder sb = new StringBuilder();
            for (Object parameter : parameters) {
                sb.append(parameter);
            }
            log.info("### JPrint --> {}", sb);
            return null;
        }
    }

    @SneakyThrows
    public void registerPrint(V8Runtime v8) {
        JavetCallbackContext call = new JavetCallbackContext(
            "print",
            new JPrint(),
            JPrint.class.getMethod("print", Object[].class)
        );
        V8ValueFunction print = v8.createV8ValueFunction(call);
        v8.getGlobalObject().set("print", print);
    }

    @Test
    public void t01() throws Exception {
        String basePath = new File("../clever-js-api/src/test/resources/javascript").getAbsolutePath();
        log.info("### basePath   -> {}", basePath);
        Folder rootFolder = FileSystemFolder.createRootPath(basePath);
        log.info("### rootFolder -> {}", rootFolder);

        ScriptEngineInstance<V8Runtime, IV8ValueObject> engineInstance = V8ScriptEngineInstance.Builder.create(rootFolder).build();
        V8Runtime v8 = engineInstance.getEngineContext().getEngine();
        registerPrint(v8);

        log.info("### getEngineName      -> {}", engineInstance.getEngineName());
        log.info("### getEngineVersion   -> {}", engineInstance.getEngineVersion());
        log.info("### getLanguageVersion -> {}", engineInstance.getLanguageVersion());

        ScriptObject<?> scriptObject = engineInstance.require("/01基本使用/01.js");
        log.info("### a1    -> {}", scriptObject.getMember("a1"));
        log.info("### a2    -> {}", scriptObject.getMember("a2"));
        log.info("### fuc   -> {}", scriptObject.getMember("fuc"));

        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());
        log.info("### hasMember        -> {}", scriptObject.hasMember("a2"));
        log.info("### hasMember        -> {}", scriptObject.hasMember("a3"));
        scriptObject.setMember("a3", "a333");
        log.info("### hasMember        -> {}", scriptObject.hasMember("a3"));
        log.info("### getMembers       -> {}", scriptObject.getMembers());
        log.info("### size             -> {}", scriptObject.size());
        log.info("### callMember       -> {}", scriptObject.callMember("fuc", 1, 2));

        engineInstance.close();
    }

    @Test
    public void t02() throws Exception {
        String basePath = new File("../clever-js-api/src/test/resources/javascript").getAbsolutePath();
        log.info("### basePath   -> {}", basePath);
        Folder rootFolder = FileSystemFolder.createRootPath(basePath);
        log.info("### rootFolder -> {}", rootFolder);

        ScriptEngineInstance<V8Runtime, IV8ValueObject> engineInstance = V8ScriptEngineInstance.Builder.create(rootFolder).build();
        V8Runtime v8 = engineInstance.getEngineContext().getEngine();
        registerPrint(v8);

        ScriptObject<?> scriptObject = engineInstance.require("/01基本使用/02.js");
        log.info("### a1    -> {}", scriptObject.getMember("a1"));
        log.info("### a2    -> {}", scriptObject.getMember("a2"));
        log.info("### fuc   -> {}", scriptObject.getMember("fuc"));

        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());
        log.info("### hasMember        -> {}", scriptObject.hasMember("a2"));
        log.info("### hasMember        -> {}", scriptObject.hasMember("a3"));
        scriptObject.setMember("a3", "a333333");
        log.info("### hasMember        -> {}", scriptObject.hasMember("a3"));
        log.info("### getMembers       -> {}", scriptObject.getMembers());
        log.info("### size             -> {}", scriptObject.size());
        log.info("### callMember       -> {}", scriptObject.callMember("fuc", 1, 2));

        scriptObject = engineInstance.require("/01基本使用/03.js");
        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());

        engineInstance.close();
    }

    @Test
    public void t03() throws Exception {
        String basePath = new File("../clever-js-api/src/test/resources/javascript").getAbsolutePath();
        log.info("### basePath   -> {}", basePath);
        Folder rootFolder = FileSystemFolder.createRootPath(basePath);
        log.info("### rootFolder -> {}", rootFolder);

        ScriptEngineInstance<V8Runtime, IV8ValueObject> engineInstance = V8ScriptEngineInstance.Builder.create(rootFolder).build();
        V8Runtime v8 = engineInstance.getEngineContext().getEngine();
        registerPrint(v8);

        ScriptObject<?> scriptObject = engineInstance.require("/02循环依赖/main");
        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());

        engineInstance.close();

        // 参考 http://nodejs.cn/api/modules.html#modules_the_module_wrapper
        // main 开始
        // a 开始
        // b 开始
        // 在 b 中，a.done = false
        // b 结束
        // 在 a 中，b.done = true
        // a 结束
        // 在 main 中，a.done=true，b.done=true
    }

    @Test
    public void t04() throws Exception {
        String basePath = new File("../clever-js-api/src/test/resources/javascript").getAbsolutePath();
        log.info("### basePath   -> {}", basePath);
        Folder rootFolder = FileSystemFolder.createRootPath(basePath);
        log.info("### rootFolder -> {}", rootFolder);

        ScriptEngineInstance<V8Runtime, IV8ValueObject> engineInstance = V8ScriptEngineInstance.Builder.create(rootFolder).build();
        V8Runtime v8 = engineInstance.getEngineContext().getEngine();
        registerPrint(v8);

        ScriptObject<?> scriptObject = engineInstance.require("/03使用npm包/src/01");
        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());
        log.info("### callMember       -> {}", scriptObject.callMember("trim", "   abc  "));

        scriptObject = engineInstance.require("/03使用npm包/src/01");
        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());
        log.info("### callMember       -> {}", scriptObject.callMember("trim", "   abc  "));

        engineInstance.close();
    }
}
