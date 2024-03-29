package org.clever.js.graaljs;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.pool.EnginePoolConfig;
import org.clever.js.graaljs.pool.GraalSingleEngineFactory;
import org.graalvm.polyglot.Context;
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
 * 创建时间：2020/08/24 17:01 <br/>
 */
@Slf4j
public class EngineInstancePool2Test {
    private ObjectPool<ScriptEngineInstance<Context, Value>> pool;

    @BeforeEach
    public void init() {
        // 创建对象池配置
        EnginePoolConfig config = new EnginePoolConfig();
        config.setMaxWait(Duration.ofMillis(-1));
        config.setMaxTotal(8);
        config.setMinIdle(2);
        // 创建对象工厂
        Folder rootFolder = FileSystemFolder.createRootPath(new File("../clever-js-api/src/test/resources").getAbsolutePath());
        GraalSingleEngineFactory factory = GraalSingleEngineFactory.create(rootFolder);
        // 创建对象池
        pool = new GenericObjectPool<>(factory, config.toGenericObjectPoolConfig());
    }

    @AfterEach
    public void close() {
        pool.close();
    }

    public void usePool() {
        ScriptEngineInstance<Context, Value> instance = null;
        try {
            // 从池中获取对象
            instance = pool.borrowObject();
            // 使用对象
            Context engine = instance.getEngineContext().getEngine();
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
