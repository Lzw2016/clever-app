package org.clever.js.graaljs;

import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/24 17:45 <br/>
 */
@Slf4j
public class MultiThreadTest {
    private final Folder rootFolder = FileSystemFolder.createRootPath(new File("src/test/resources").getAbsolutePath());
    private ScriptEngineInstance<?, ?> engineInstance;

    @BeforeEach
    public void before() {
        // clever-js-graaljs
        log.info("### rootFolder -> {}", rootFolder);
        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        engineInstance = GraalScriptEngineInstance.Builder.create(engine, rootFolder).build();
    }

    @AfterEach
    public void after() throws IOException {
        engineInstance.close();
    }

    @Test
    public void t01() throws Exception {
        engineInstance.require("/pool2-test").callMember("t01");

        Thread thread = new Thread(() -> {
            try {
                engineInstance.require("/pool2-test").callMember("t01");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        thread.join();
    }
}
