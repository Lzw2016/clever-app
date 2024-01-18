package org.clever.js.graaljs;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.pool.GenericEngineInstancePool;
import org.clever.js.graaljs.pool.GraalSingleEngineFactory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/24 19:33 <br/>
 */
@Slf4j
public class GraalSingleEngineFactoryTest {

    private GenericEngineInstancePool<Context, Value> pool;
    private GraalSingleEngineFactory factory;

    @BeforeEach
    public void init() {
        // 创建对象池配置
        GenericObjectPoolConfig<ScriptEngineInstance<Context, Value>> config = new GenericObjectPoolConfig<>();
        config.setMaxWait(Duration.ofMillis(-1));
        config.setMaxTotal(8);
        config.setMinIdle(2);
        // 创建对象工厂
        Folder rootFolder = FileSystemFolder.createRootPath(new File("../clever-js-api/src/test/resources").getAbsolutePath());
        Engine engine = Engine.newBuilder()
            .useSystemProperties(true)
            .build();
        factory = new GraalSingleEngineFactory(rootFolder, engine);
        // 创建对象池
        pool = new GenericEngineInstancePool<>(factory, config);
    }

    @AfterEach
    public void close() {
        pool.close();
        factory.close();
    }

    public void usePool() {
        ScriptEngineInstance<Context, Value> instance = null;
        try {
            // 从池中获取对象
            instance = pool.borrowObject();
            // 使用对象
            Context engine = instance.getContext().getEngine();
            try {
                engine.enter();
                instance.require("/pool2-test").callMember("t01");
            } finally {
                if (engine != null) {
                    engine.leave();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (instance != null) {
                try {
                    // 出现错误将对象置为失效
                    pool.invalidateObject(instance);
                    // 避免 invalidate 之后再 return 抛异常
                    instance = null;
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        } finally {
            try {
                if (null != instance) {
                    // 使用完后必须 returnObject
                    pool.returnObject(instance);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @SneakyThrows
    @Test
    public void t01() {
        final int threadCount = 10000;
        final Semaphore semaphore = new Semaphore(threadCount);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(256, 256, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(102400));
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            semaphore.acquire();
            executor.execute(() -> {
                usePool();
                semaphore.release();
                // log.info("# --> release");
            });
        }
        semaphore.acquire(threadCount);
        final long endTime = System.currentTimeMillis();
        log.info("# --> 耗时: {}ms", endTime - startTime);
    }
}
