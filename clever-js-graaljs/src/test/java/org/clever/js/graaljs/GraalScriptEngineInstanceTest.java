package org.clever.js.graaljs;

import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/21 10:05 <br/>
 */
@Slf4j
public class GraalScriptEngineInstanceTest {

    @Test
    public void t01() throws Exception {
        String basePath = new File("../clever-js-api/src/test/resources/javascript").getAbsolutePath();
        log.info("### basePath   -> {}", basePath);
        Folder rootFolder = FileSystemFolder.createRootPath(basePath);
        log.info("### rootFolder -> {}", rootFolder);

        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        ScriptEngineInstance<Context, Value> engineInstance = GraalScriptEngineInstance.Builder.create(engine, rootFolder).build();
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

        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        ScriptEngineInstance<?, ?> engineInstance = GraalScriptEngineInstance.Builder.create(engine, rootFolder).build();
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

        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        ScriptEngineInstance<?, ?> engineInstance = GraalScriptEngineInstance.Builder.create(engine, rootFolder).build();
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

        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        ScriptEngineInstance<?, ?> engineInstance = GraalScriptEngineInstance.Builder.create(engine, rootFolder).build();
        ScriptObject<?> scriptObject = engineInstance.require("/03使用npm包/src/01");
        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());
        log.info("### callMember       -> {}", scriptObject.callMember("trim", "   abc  "));

        scriptObject = engineInstance.require("/03使用npm包/src/01");
        log.info("### getMemberNames   -> {}", scriptObject.getMemberNames());
        log.info("### callMember       -> {}", scriptObject.callMember("trim", "   abc  "));
        engineInstance.close();
    }
}
