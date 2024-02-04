package org.clever.task;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.core.env.Environment;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.pool.EngineInstancePool;
import org.clever.js.api.pool.GenericEngineInstancePool;
import org.clever.js.graaljs.pool.GraalSingleEngineFactory;
import org.clever.task.core.job.JsJobExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.time.Duration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/04 17:58 <br/>
 */
public class JsExecutorBootstrap {
    // TODO 通过配置实现 JsJobExecutor

    public static JsExecutorBootstrap create(Environment environment) {
        return new JsExecutorBootstrap();
    }

    private final EngineInstancePool<Context, Value> engineInstancePool;

    public JsExecutorBootstrap() {
        GenericObjectPoolConfig<ScriptEngineInstance<Context, Value>> config = new GenericObjectPoolConfig<>();
        config.setMaxWait(Duration.ofSeconds(30));
        config.setMaxTotal(8);
        config.setMinIdle(1);
        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        // Folder rootFolder = FileSystemFolder.createRootPath("/not_exists_path");
        Folder rootFolder = FileSystemFolder.createRootPath("D:\\");
        GraalSingleEngineFactory factory = new GraalSingleEngineFactory(rootFolder, engine);
        engineInstancePool = new GenericEngineInstancePool<>(factory, config);
    }

    public void init() {
        TaskBootstrap.JOB_EXECUTORS.add(new JsJobExecutor(engineInstancePool));
    }
}
