package org.clever.js.graaljs;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.ScriptObject;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.clever.js.graaljs.utils.InteropScriptToJavaUtils;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/30 20:44 <br/>
 */
@Slf4j
public class JavaInteropTest {
    private final Folder rootFolder = FileSystemFolder.createRootPath(new File("../clever-js-api/src/test/resources/graaljs").getAbsolutePath());

    private ScriptEngineInstance<?, ?> engineInstance;

    @BeforeEach
    public void before1() {
        // clever-js-graaljs
        log.info("### rootFolder -> {}", rootFolder);
        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        engineInstance = GraalScriptEngineInstance.Builder.create(engine, rootFolder)
            .setCustomHostAccess(builder -> {
                builder.targetTypeMapping(
                    Value.class,
                    TestBean.class,
                    Value::hasMembers,
                    value -> {
                        TestBean bean = new TestBean();
                        // noinspection unchecked
                        BeanCopyUtils.toBean(
                            (Map<String, Object>) InteropScriptToJavaUtils.INSTANCE.deepToJavaObject(value),
                            bean
                        );
                        return bean;
                    }
                );
            })
            .build();
    }

    @AfterEach
    public void after() throws IOException {
        engineInstance.close();
    }

    @Test
    public void t01() throws Exception {
        ScriptObject<?> scriptObject = engineInstance.require("/java-interop01");
        log.info("# -------> {}", scriptObject);
    }

    @Test
    public void t02() throws Exception {
        ScriptObject<?> scriptObject = engineInstance.require("/java-interop02");
        log.info("# -------> {}", scriptObject);
    }

    @Test
    public void t03() throws Exception {
        ScriptObject<?> scriptObject = engineInstance.require("/java-interop03");
        log.info("# -------> {}", scriptObject);
    }
}
