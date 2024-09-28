package org.clever.data.redis;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppContextHolder;
import org.clever.core.Assert;
import org.clever.core.BannerUtils;
import org.clever.core.SystemClock;
import org.clever.data.redis.config.RedisConfig;
import org.clever.data.redis.config.RedisProperties;
import org.clever.data.redis.support.LettuceClientConfigurationBuilderCustomizer;
import org.clever.data.redis.support.RedissonClientConfigurationCustomizer;
import org.clever.data.redis.util.MergeRedisProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/20 14:09 <br/>
 */
@Slf4j
public class RedisBootstrap {
    public static RedisBootstrap create(RedisConfig redisConfig) {
        return new RedisBootstrap(redisConfig);
    }

    public static RedisBootstrap create(Environment environment) {
        RedisConfig redisConfig = Binder.get(environment).bind(RedisConfig.PREFIX, RedisConfig.class).orElseGet(RedisConfig::new);
        AppContextHolder.registerBean("redisConfig", redisConfig, true);
        return create(redisConfig);
    }

    /**
     * 自定义创建 Redis 数据源 {@code Map<Redis数据源名称, LettuceClientConfigurationBuilderCustomizer>}
     */
    public final static Map<String, LettuceClientConfigurationBuilderCustomizer> REDIS_CUSTOMIZER = new LinkedHashMap<>();
    /**
     * 自定义创建 Redisson 对象 {@code Map<Redis数据源名称, LettuceClientConfigurationBuilderCustomizer>}
     */
    public final static Map<String, RedissonClientConfigurationCustomizer> REDISSON_CUSTOMIZER = new LinkedHashMap<>();

    private volatile boolean initialized = false;
    @Getter
    private final RedisConfig redisConfig;

    public RedisBootstrap(RedisConfig redisConfig) {
        Assert.notNull(redisConfig, "参数 redisConfig 不能为空");
        this.redisConfig = redisConfig;
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        initRedis();
    }

    private void initRedis() {
        final RedisProperties global = Optional.ofNullable(redisConfig.getGlobal()).orElse(new RedisProperties());
        final Map<String, RedisProperties> dataSource = Optional.ofNullable(redisConfig.getDataSource()).orElse(Collections.emptyMap());
        // 合并数据源配置
        dataSource.forEach((name, config) -> MergeRedisProperties.mergeConfig(global, config));
        // 打印配置日志
        List<String> logs = new ArrayList<>();
        logs.add("redis: ");
        logs.add("  enable     : " + redisConfig.isEnable());
        logs.add("  defaultName: " + redisConfig.getDefaultName());
        logs.add("  dataSource : ");
        dataSource.forEach((name, config) -> {
            logs.add("    " + name + ": ");
            logs.add("      mode: " + config.getMode());
            RedisProperties.Standalone standalone = config.getStandalone();
            if (RedisProperties.Mode.Standalone.equals(config.getMode()) && standalone != null) {
                logs.add("      standalone: ");
                logs.add("        host         : " + standalone.getHost());
                logs.add("        port         : " + standalone.getPort());
                logs.add("        database     : " + standalone.getDatabase());
            }
            RedisProperties.Sentinel sentinel = config.getSentinel();
            if (RedisProperties.Mode.Sentinel.equals(config.getMode()) && sentinel != null) {
                logs.add("      sentinel: ");
                logs.add("        master       : " + sentinel.getMaster());
                logs.add("        nodes        : " + StringUtils.join(sentinel.getNodes(), " | "));
                logs.add("        database     : " + sentinel.getDatabase());
            }
            RedisProperties.Cluster cluster = config.getCluster();
            if (RedisProperties.Mode.Cluster.equals(config.getMode()) && cluster != null) {
                logs.add("      cluster: ");
                logs.add("        nodes        : " + StringUtils.join(cluster.getNodes(), " | "));
                logs.add("        maxRedirects : " + cluster.getMaxRedirects());
            }
            RedisProperties.Pool pool = config.getPool();
            if (pool != null) {
                logs.add("      pool: ");
                logs.add("        enabled      : " + pool.isEnabled());
                logs.add("        maxIdle      : " + pool.getMaxIdle());
                logs.add("        maxActive    : " + pool.getMaxActive());
                logs.add("        maxWait      : " + pool.getMaxWait().toMillis() + "ms");
            }
        });
        if (redisConfig.isEnable()) {
            BannerUtils.printConfig(log, "redis数据源配置", logs.toArray(new String[0]));
        }
        if (!redisConfig.isEnable()) {
            return;
        }
        final Map<String, Redis> dataSourceMap = new HashMap<>(dataSource.size());
        // 初始化 Redis
        final long startTime = SystemClock.now();
        dataSource.forEach((name, config) -> {
            if (dataSourceMap.containsKey(name)) {
                throw new RuntimeException("Redis 名称重复: " + name);
            }
            LettuceClientConfigurationBuilderCustomizer redisCustomizer = REDIS_CUSTOMIZER.get(name);
            RedissonClientConfigurationCustomizer redissonCustomizer = REDISSON_CUSTOMIZER.get(name);
            Redis redis = new Redis(name, config, redisCustomizer, redissonCustomizer);
            dataSourceMap.put(name, redis);
            RedisAdmin.addRedis(name, redis);
        });
        // 默认的 Redis
        RedisAdmin.setDefaultRedisName(redisConfig.getDefaultName());
        log.info("默认的 Redis: {}", redisConfig.getDefaultName());
        if (!dataSource.isEmpty()) {
            log.info("redis数据源初始化完成 | 耗时: {}ms", SystemClock.now() - startTime);
        }
        // Redis 对象注入到IOC容器
        for (String redisName : RedisAdmin.allRedisNames()) {
            boolean primary = Objects.equals(redisName, redisConfig.getDefaultName());
            // Redis对象注入到IOC容器
            Redis redis = RedisAdmin.getRedis(redisName);
            AppContextHolder.registerBean(redisName + "Redis", redis, primary);
        }
    }
}
