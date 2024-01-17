package org.clever.js.graaljs;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.folder.FileSystemFolder;
import org.clever.js.api.folder.Folder;
import org.clever.js.graaljs.utils.ScriptEngineUtils;
import org.clever.util.Assert;
import org.graalvm.polyglot.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
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
        GenericObjectPoolConfig<ScriptEngineInstance<Context, Value>> config = new GenericObjectPoolConfig<>();
        config.setMaxWait(Duration.ofMillis(-1));
        config.setMaxTotal(8);
        config.setMinIdle(2);
        // 创建对象工厂
        Folder rootFolder = FileSystemFolder.createRootPath(new File("src/test/resources").getAbsolutePath());
        Engine engine = Engine.newBuilder()
                .useSystemProperties(true)
                .build();
        PooledObjectFactory<ScriptEngineInstance<Context, Value>> factory = new ScriptEngineInstanceFactory(rootFolder, engine);
        // 创建对象池
        pool = new GenericObjectPool<>(factory, config);
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

@Slf4j
class ScriptEngineInstanceFactory extends BasePooledObjectFactory<ScriptEngineInstance<Context, Value>> {
    private final Folder rootFolder;
    private final Engine engine;
    private final HostAccess hostAccess;

    ScriptEngineInstanceFactory(Folder rootFolder, Engine engine) {
        this.rootFolder = rootFolder;
        this.engine = engine;
        // 沙箱环境控制 - 定义JavaScript可以访问的Class(使用黑名单机制)
        HostAccess.Builder hostAccessBuilder = HostAccess.newBuilder();
        hostAccessBuilder.allowArrayAccess(true);
        hostAccessBuilder.allowListAccess(true);
        hostAccessBuilder.allowPublicAccess(true);
        hostAccessBuilder.allowAllImplementations(true);
        // 计划支持 https://github.com/graalvm/graaljs/issues/143
        // hostAccessBuilder.allowMapAccess(true);
        Set<Class<?>> denyAccessClass = new HashSet<>(GlobalConstant.Default_Deny_Access_Class);
        ScriptEngineUtils.addDenyAccess(hostAccessBuilder, denyAccessClass);
        this.hostAccess = hostAccessBuilder.build();
    }

    private Context creatEngine(Engine engine) {
        Assert.notNull(engine, "参数engine不能为空");
        Context.Builder contextBuilder = Context.newBuilder(GraalConstant.Js_Language_Id)
                .engine(engine)
                .options(ScriptEngineUtils.Context_Default_Options)
                // 不允许使用实验特性
                .allowExperimentalOptions(false)
                // 不允许多语言访问
                .allowPolyglotAccess(PolyglotAccess.NONE)
                // 默认允许所有行为
                .allowAllAccess(true)
                // 不允许JavaScript创建进程
                .allowCreateProcess(false)
                // 不允许JavaScript创建线程
                .allowCreateThread(false)
                // 不允许JavaScript访问环境变量
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                // 不允许JavaScript对主机的IO操作
                .allowIO(false)
                // 不允许JavaScript访问本机接口
                .allowNativeAccess(false)
                // 不允许JavaScript加载Class
                .allowHostClassLoading(false)
                // 定义JavaScript可以加载的Class
                // .allowHostClassLookup()
                // 定义JavaScript可以访问的Class
                // .allowHostAccess(HostAccess.ALL)
                // 限制JavaScript的资源使用(CPU)
                // .resourceLimits()
                ;
        // 沙箱环境控制 - 定义JavaScript可以访问的Class(使用黑名单机制)
        contextBuilder.allowHostAccess(hostAccess);
        // 沙箱环境控制 - 限制JavaScript的资源使用
        // ResourceLimits resourceLimits = ResourceLimits.newBuilder().statementLimit()
        return contextBuilder.build();
    }

    // 创建一个新的对象
    @Override
    public ScriptEngineInstance<Context, Value> create() {
        ScriptEngineInstance<Context, Value> instance = GraalScriptEngineInstance.Builder.create(engine, rootFolder)
                .setEngine(creatEngine(engine))
                .build();
        log.info("# create");
        // instance.getContext().getEngine().leave();
        return instance;
    }

    // 封装为池化对象
    @Override
    public PooledObject<ScriptEngineInstance<Context, Value>> wrap(ScriptEngineInstance<Context, Value> scriptEngineInstance) {
        return new DefaultPooledObject<>(scriptEngineInstance);
    }

    // 使用完返还对象
    @Override
    public void passivateObject(PooledObject<ScriptEngineInstance<Context, Value>> pooledObject) {
        log.info("# passivateObject");
    }

    // 销毁对象
    @Override
    public void destroyObject(PooledObject<ScriptEngineInstance<Context, Value>> pooledObject) throws Exception {
        pooledObject.getObject().close();
        log.info("# destroyObject");
    }
}
