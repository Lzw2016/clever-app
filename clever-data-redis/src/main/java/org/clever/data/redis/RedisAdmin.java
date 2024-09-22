package org.clever.data.redis;

import org.clever.core.AppShutdownHook;
import org.clever.core.Assert;
import org.clever.core.OrderIncrement;
import org.clever.data.redis.support.RedisInfo;
import org.clever.data.redis.support.RedisPoolStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/13 09:45 <br/>
 */
public class RedisAdmin {
    /**
     * 默认的Redis
     */
    private static String DEFAULT_REDIS_NAME;
    /**
     * Redis集合 {@code ConcurrentMap<redisName, Redis>}
     */
    private static final ConcurrentMap<String, Redis> REDIS_MAP = new ConcurrentHashMap<>();

    static {
        AppShutdownHook.addShutdownHook(RedisAdmin::closeAllRedis, OrderIncrement.MAX, "关闭Redis连接池");
    }

    /**
     * 关闭所有的 Redis
     */
    public static void closeAllRedis() {
        for (Redis redis : REDIS_MAP.values()) {
            try {
                if (!redis.isClosed()) {
                    redis.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 设置默认数据源
     *
     * @param defaultRedisName 默认数据源名称
     */
    public static void setDefaultRedisName(String defaultRedisName) {
        Assert.hasText(defaultRedisName, "参数defaultRedisName不能为空");
        DEFAULT_REDIS_NAME = defaultRedisName;
    }

    /**
     * 默认数据源名称
     */
    public static String getDefaultRedisName() {
        return DEFAULT_REDIS_NAME;
    }

    /**
     * 新增数据源
     *
     * @param redisName 数据源名称
     * @param redis     数据源
     */
    public static void addRedis(String redisName, Redis redis) {
        Assert.hasText(redisName, "参数redisName不能为空");
        Assert.isTrue(redis != null, "参数redis不能为空");
        REDIS_MAP.put(redisName, redis);
    }

    /**
     * 获取数据源
     *
     * @param redisName 数据源名称
     */
    public static Redis getRedis(String redisName) {
        return REDIS_MAP.get(redisName);
    }

    /**
     * 获取默认的数据源
     */
    public static Redis getRedis() {
        return getRedis(DEFAULT_REDIS_NAME);
    }

    /**
     * 获取所有数据源名称
     */
    public static Set<String> allRedisNames() {
        return REDIS_MAP.keySet();
    }

    /**
     * 获取数据源信息
     *
     * @param redisName 数据源名称
     */
    public static RedisInfo getInfo(String redisName) {
        Redis redis = getRedis(redisName);
        return redis == null ? null : redis.getInfo();
    }

    /**
     * 获取所有数据源信息
     */
    public static Map<String, RedisInfo> allInfos() {
        Map<String, RedisInfo> map = new HashMap<>(REDIS_MAP.size());
        for (Map.Entry<String, Redis> entry : REDIS_MAP.entrySet()) {
            String name = entry.getKey();
            map.put(name, getInfo(name));
        }
        return map;
    }

    /**
     * 获取数据源状态
     *
     * @param redisName 数据源名称
     */
    public static RedisPoolStatus getStatus(String redisName) {
        Redis redis = getRedis(redisName);
        return redis == null ? null : redis.getPoolStatus();
    }

    /**
     * 获取数据源状态
     */
    public static Map<String, RedisPoolStatus> allStatus() {
        Map<String, RedisPoolStatus> map = new HashMap<>(REDIS_MAP.size());
        for (Map.Entry<String, Redis> entry : REDIS_MAP.entrySet()) {
            String name = entry.getKey();
            map.put(name, getStatus(name));
        }
        return map;
    }
}
