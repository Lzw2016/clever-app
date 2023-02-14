package org.clever.data.redis.util;

import org.clever.data.redis.config.RedisProperties;

import java.util.Objects;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-06 22:05 <br/>
 */
public class MergeRedisProperties {
    private static final RedisProperties DEF = new RedisProperties();
    private static final RedisProperties.Standalone DEF_STANDALONE = DEF.getStandalone();
    private static final RedisProperties.Sentinel DEF_SENTINEL = DEF.getSentinel();
    private static final RedisProperties.Cluster DEF_CLUSTER = DEF.getCluster();
    private static final RedisProperties.Cluster.Refresh DEF_CLUSTER_REFRESH = DEF.getCluster().getRefresh();
    private static final RedisProperties.Pool DEF_POOL = DEF.getPool();

    public static RedisProperties mergeConfig(RedisProperties source, RedisProperties target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getMode(), DEF.getMode())) {
            target.setMode(source.getMode());
        }
        target.setStandalone(mergeStandalone(source.getStandalone(), target.getStandalone()));
        target.setSentinel(mergeSentinel(source.getSentinel(), target.getSentinel()));
        target.setCluster(mergeCluster(source.getCluster(), target.getCluster()));
        if (Objects.equals(target.isSsl(), DEF.isSsl())) {
            target.setSsl(source.isSsl());
        }
        if (Objects.equals(target.getReadTimeout(), DEF.getReadTimeout())) {
            target.setReadTimeout(source.getReadTimeout());
        }
        if (Objects.equals(target.getConnectTimeout(), DEF.getConnectTimeout())) {
            target.setConnectTimeout(source.getConnectTimeout());
        }
        if (Objects.equals(target.getShutdownTimeout(), DEF.getShutdownTimeout())) {
            target.setShutdownTimeout(source.getShutdownTimeout());
        }
        if (Objects.equals(target.getClientName(), DEF.getClientName())) {
            target.setClientName(source.getClientName());
        }
        target.setPool(mergePool(source.getPool(), target.getPool()));
        return target;
    }

    public static RedisProperties.Standalone mergeStandalone(RedisProperties.Standalone source, RedisProperties.Standalone target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getHost(), DEF_STANDALONE.getHost())) {
            target.setHost(source.getHost());
        }
        if (Objects.equals(target.getPort(), DEF_STANDALONE.getPort())) {
            target.setPort(source.getPort());
        }
        if (Objects.equals(target.getDatabase(), DEF_STANDALONE.getDatabase())) {
            target.setDatabase(source.getDatabase());
        }
        if (Objects.equals(target.getUsername(), DEF_STANDALONE.getUsername())) {
            target.setUsername(source.getUsername());
        }
        if (Objects.equals(target.getPassword(), DEF_STANDALONE.getPassword())) {
            target.setPassword(source.getPassword());
        }
        return target;
    }

    public static RedisProperties.Sentinel mergeSentinel(RedisProperties.Sentinel source, RedisProperties.Sentinel target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getMaster(), DEF_SENTINEL.getMaster())) {
            target.setMaster(source.getMaster());
        }
        if (Objects.equals(target.getNodes(), DEF_SENTINEL.getNodes())) {
            target.setNodes(source.getNodes());
        }
        if (Objects.equals(target.getDatabase(), DEF_SENTINEL.getDatabase())) {
            target.setDatabase(source.getDatabase());
        }
        if (Objects.equals(target.getUsername(), DEF_SENTINEL.getUsername())) {
            target.setUsername(source.getUsername());
        }
        if (Objects.equals(target.getPassword(), DEF_SENTINEL.getPassword())) {
            target.setPassword(source.getPassword());
        }
        return target;
    }

    public static RedisProperties.Cluster mergeCluster(RedisProperties.Cluster source, RedisProperties.Cluster target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getNodes(), DEF_CLUSTER.getNodes())) {
            target.setNodes(source.getNodes());
        }
        if (Objects.equals(target.getMaxRedirects(), DEF_CLUSTER.getMaxRedirects())) {
            target.setMaxRedirects(source.getMaxRedirects());
        }
        if (Objects.equals(target.getUsername(), DEF_CLUSTER.getUsername())) {
            target.setUsername(source.getUsername());
        }
        if (Objects.equals(target.getPassword(), DEF_CLUSTER.getPassword())) {
            target.setPassword(source.getPassword());
        }
        target.setRefresh(mergeRefresh(source.getRefresh(), target.getRefresh()));
        return target;
    }

    public static RedisProperties.Cluster.Refresh mergeRefresh(RedisProperties.Cluster.Refresh source, RedisProperties.Cluster.Refresh target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.isDynamicRefreshSources(), DEF_CLUSTER_REFRESH.isDynamicRefreshSources())) {
            target.setDynamicRefreshSources(source.isDynamicRefreshSources());
        }
        if (Objects.equals(target.getPeriod(), DEF_CLUSTER_REFRESH.getPeriod())) {
            target.setPeriod(source.getPeriod());
        }
        if (Objects.equals(target.isAdaptive(), DEF_CLUSTER_REFRESH.isAdaptive())) {
            target.setAdaptive(source.isAdaptive());
        }
        return target;
    }

    private static RedisProperties.Pool mergePool(RedisProperties.Pool source, RedisProperties.Pool target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.isEnabled(), DEF_POOL.isEnabled())) {
            target.setEnabled(source.isEnabled());
        }
        if (Objects.equals(target.getMaxIdle(), DEF_POOL.getMaxIdle())) {
            target.setMaxIdle(source.getMaxIdle());
        }
        if (Objects.equals(target.getMinIdle(), DEF_POOL.getMinIdle())) {
            target.setMinIdle(source.getMinIdle());
        }
        if (Objects.equals(target.getMaxActive(), DEF_POOL.getMaxActive())) {
            target.setMaxActive(source.getMaxActive());
        }
        if (Objects.equals(target.getMaxWait(), DEF_POOL.getMaxWait())) {
            target.setMaxWait(source.getMaxWait());
        }
        if (Objects.equals(target.getTimeBetweenEvictionRuns(), DEF_POOL.getTimeBetweenEvictionRuns())) {
            target.setTimeBetweenEvictionRuns(source.getTimeBetweenEvictionRuns());
        }
        return target;
    }
}
