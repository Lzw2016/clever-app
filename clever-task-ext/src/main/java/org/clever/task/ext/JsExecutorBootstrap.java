package org.clever.task.ext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.AppContextHolder;
import org.clever.core.BannerUtils;
import org.clever.js.api.folder.EmptyFolder;
import org.clever.js.api.module.MemoryCache;
import org.clever.js.api.pool.EngineInstancePool;
import org.clever.js.api.pool.EnginePoolConfig;
import org.clever.js.api.pool.GenericEngineInstancePool;
import org.clever.js.graaljs.GraalScriptEngineInstance;
import org.clever.js.graaljs.pool.GraalSingleEngineFactory;
import org.clever.task.TaskBootstrap;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.job.JsExecutor;
import org.clever.task.ext.config.JsEngineConfig;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/04 17:58 <br/>
 */
@Getter
@Slf4j
public class JsExecutorBootstrap {
    public static JsExecutorBootstrap create(SchedulerConfig schedulerConfig, JsEngineConfig jsEngineConfig) {
        return new JsExecutorBootstrap(schedulerConfig, jsEngineConfig);
    }

    public static JsExecutorBootstrap create(SchedulerConfig schedulerConfig, Environment environment) {
        final JsEngineConfig config = Binder.get(environment).bind(JsEngineConfig.PREFIX, JsEngineConfig.class).orElseGet(JsEngineConfig::new);
        final EnginePoolConfig enginePool = config.getEnginePool();
        // 打印配置日志
        List<String> logs = new ArrayList<>();
        logs.add("timed-task: ");
        logs.add("  js-executor: ");
        logs.add("    enginePool: ");
        logs.add("      lifo                     : " + enginePool.isLifo());
        logs.add("      fairness                 : " + enginePool.isFairness());
        logs.add("      maxIdle                  : " + enginePool.getMaxIdle());
        logs.add("      minIdle                  : " + enginePool.getMinIdle());
        logs.add("      maxTotal                 : " + enginePool.getMaxTotal());
        logs.add("      maxWait                  : " + (enginePool.getMaxWait() != null ? enginePool.getMaxWait().toMillis() + "ms" : ""));
        logs.add("      blockWhenExhausted       : " + enginePool.isBlockWhenExhausted());
        logs.add("      testOnCreate             : " + enginePool.isTestOnCreate());
        logs.add("      testOnBorrow             : " + enginePool.isTestOnBorrow());
        logs.add("      testOnReturn             : " + enginePool.isTestOnReturn());
        logs.add("      testWhileIdle            : " + enginePool.isTestWhileIdle());
        logs.add("      timeBetweenEvictionRuns  : " + (enginePool.getTimeBetweenEvictionRuns() != null ? enginePool.getTimeBetweenEvictionRuns().toMillis() + "ms" : ""));
        logs.add("      numTestsPerEvictionRun   : " + enginePool.getNumTestsPerEvictionRun());
        logs.add("      minEvictableIdleTime     : " + (enginePool.getMinEvictableIdleTime() != null ? enginePool.getMinEvictableIdleTime().toMillis() + "ms" : ""));
        logs.add("      softMinEvictableIdleTime : " + (enginePool.getSoftMinEvictableIdleTime() != null ? enginePool.getSoftMinEvictableIdleTime().toMillis() + "ms" : ""));
        logs.add("      evictionPolicyClassName  : " + enginePool.getEvictionPolicyClassName());
        logs.add("      evictorShutdownTimeout   : " + (enginePool.getEvictorShutdownTimeout() != null ? enginePool.getEvictorShutdownTimeout().toMillis() + "ms" : ""));
        logs.add("      jmxEnabled               : " + enginePool.isJmxEnabled());
        logs.add("      jmxNamePrefix            : " + enginePool.getJmxNamePrefix());
        logs.add("      jmxNameBase              : " + enginePool.getJmxNameBase());
        if (schedulerConfig.isEnable()) {
            BannerUtils.printConfig(log, "定时任务JsExecutor配置", logs.toArray(new String[0]));
        }
        JsExecutorBootstrap jsExecutorBootstrap = new JsExecutorBootstrap(schedulerConfig, config);
        AppContextHolder.registerBean("jsExecutorBootstrap", jsExecutorBootstrap, true);
        return jsExecutorBootstrap;
    }

    private final SchedulerConfig schedulerConfig;
    private final JsEngineConfig jsEngineConfig;
    private final EngineInstancePool<Context, Value> engineInstancePool;

    public JsExecutorBootstrap(SchedulerConfig schedulerConfig, JsEngineConfig jsEngineConfig) {
        this.schedulerConfig = schedulerConfig;
        this.jsEngineConfig = jsEngineConfig;
        if (schedulerConfig.isEnable()) {
            Engine graalvmEngine = Engine.newBuilder()
                .useSystemProperties(true)
                .build();
            GraalScriptEngineInstance.Builder builder = new GraalScriptEngineInstance.Builder(graalvmEngine, EmptyFolder.ROOT);
            builder.setModuleCache(new MemoryCache<>()).setFunctionCache(new MemoryCache<>());
            // builder.registerGlobalVar("", ); // 注入全局对象
            GraalSingleEngineFactory factory = new GraalSingleEngineFactory(builder);
            engineInstancePool = new GenericEngineInstancePool<>(factory, jsEngineConfig.getEnginePool().toGenericObjectPoolConfig());
        } else {
            engineInstancePool = null;
        }
    }

    /**
     * 注册 JsExecutor 到 TaskInstance
     */
    public void init() {
        if (engineInstancePool != null) {
            JsExecutor jsExecutor = new JsExecutor(engineInstancePool);
            TaskBootstrap.JOB_EXECUTORS.add(jsExecutor);
        }
    }
}
