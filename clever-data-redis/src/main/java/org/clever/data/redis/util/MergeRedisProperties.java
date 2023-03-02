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
    private static final RedisProperties.RedissonConfig DEF_REDISSON = DEF.getRedisson();
    private static final RedisProperties.RedissonConfig.SingleConfig DEF_REDISSON_SINGLE = DEF_REDISSON.getSingle();
    private static final RedisProperties.RedissonConfig.SlaveConfig DEF_REDISSON_SLAVE = DEF_REDISSON.getSlave();
    private static final RedisProperties.RedissonConfig.ClusterConfig DEF_REDISSON_CLUSTER = DEF_REDISSON.getCluster();
    private static final RedisProperties.RedissonConfig.SentinelConfig DEF_REDISSON_SENTINEL = DEF_REDISSON.getSentinel();

    /**
     * 合并RedisProperties配置，会修改target配置属性
     *
     * @param source 源配置
     * @param target 目标配置
     */
    public static void mergeConfig(RedisProperties source, RedisProperties target) {
        if (source == null) {
            return;
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
        target.setRedisson(mergeRedisson(source.getRedisson(), target.getRedisson()));
    }

    private static RedisProperties.Standalone mergeStandalone(RedisProperties.Standalone source, RedisProperties.Standalone target) {
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

    private static RedisProperties.Sentinel mergeSentinel(RedisProperties.Sentinel source, RedisProperties.Sentinel target) {
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

    private static RedisProperties.Cluster mergeCluster(RedisProperties.Cluster source, RedisProperties.Cluster target) {
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

    private static RedisProperties.Cluster.Refresh mergeRefresh(RedisProperties.Cluster.Refresh source, RedisProperties.Cluster.Refresh target) {
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

    private static RedisProperties.RedissonConfig mergeRedisson(RedisProperties.RedissonConfig source, RedisProperties.RedissonConfig target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getThreads(), DEF_REDISSON.getThreads())) {
            target.setThreads(source.getThreads());
        }
        if (Objects.equals(target.getNettyThreads(), DEF_REDISSON.getNettyThreads())) {
            target.setNettyThreads(source.getNettyThreads());
        }
        if (Objects.equals(target.getRedissonReferenceEnabled(), DEF_REDISSON.getRedissonReferenceEnabled())) {
            target.setRedissonReferenceEnabled(source.getRedissonReferenceEnabled());
        }
        if (Objects.equals(target.getTransportMode(), DEF_REDISSON.getTransportMode())) {
            target.setTransportMode(source.getTransportMode());
        }
        if (Objects.equals(target.getLockWatchdogTimeout(), DEF_REDISSON.getLockWatchdogTimeout())) {
            target.setLockWatchdogTimeout(source.getLockWatchdogTimeout());
        }
        if (Objects.equals(target.getCheckLockSyncedSlaves(), DEF_REDISSON.getCheckLockSyncedSlaves())) {
            target.setCheckLockSyncedSlaves(source.getCheckLockSyncedSlaves());
        }
        if (Objects.equals(target.getReliableTopicWatchdogTimeout(), DEF_REDISSON.getReliableTopicWatchdogTimeout())) {
            target.setReliableTopicWatchdogTimeout(source.getReliableTopicWatchdogTimeout());
        }
        if (Objects.equals(target.getKeepPubSubOrder(), DEF_REDISSON.getKeepPubSubOrder())) {
            target.setKeepPubSubOrder(source.getKeepPubSubOrder());
        }
        if (Objects.equals(target.getUseScriptCache(), DEF_REDISSON.getUseScriptCache())) {
            target.setUseScriptCache(source.getUseScriptCache());
        }
        if (Objects.equals(target.getMinCleanUpDelay(), DEF_REDISSON.getMinCleanUpDelay())) {
            target.setMinCleanUpDelay(source.getMinCleanUpDelay());
        }
        if (Objects.equals(target.getMaxCleanUpDelay(), DEF_REDISSON.getMaxCleanUpDelay())) {
            target.setMaxCleanUpDelay(source.getMaxCleanUpDelay());
        }
        if (Objects.equals(target.getCleanUpKeysAmount(), DEF_REDISSON.getCleanUpKeysAmount())) {
            target.setCleanUpKeysAmount(source.getCleanUpKeysAmount());
        }
        if (Objects.equals(target.getUseThreadClassLoader(), DEF_REDISSON.getUseThreadClassLoader())) {
            target.setUseThreadClassLoader(source.getUseThreadClassLoader());
        }
        if (Objects.equals(target.getIdleConnectionTimeout(), DEF_REDISSON.getIdleConnectionTimeout())) {
            target.setIdleConnectionTimeout(source.getIdleConnectionTimeout());
        }
        if (Objects.equals(target.getRetryInterval(), DEF_REDISSON.getRetryInterval())) {
            target.setRetryInterval(source.getRetryInterval());
        }
        if (Objects.equals(target.getRetryAttempts(), DEF_REDISSON.getRetryAttempts())) {
            target.setRetryAttempts(source.getRetryAttempts());
        }
        if (Objects.equals(target.getSubscriptionsPerConnection(), DEF_REDISSON.getSubscriptionsPerConnection())) {
            target.setSubscriptionsPerConnection(source.getSubscriptionsPerConnection());
        }
        if (Objects.equals(target.getSslEnableEndpointIdentification(), DEF_REDISSON.getSslEnableEndpointIdentification())) {
            target.setSslEnableEndpointIdentification(source.getSslEnableEndpointIdentification());
        }
        if (Objects.equals(target.getPingConnectionInterval(), DEF_REDISSON.getPingConnectionInterval())) {
            target.setPingConnectionInterval(source.getPingConnectionInterval());
        }
        if (Objects.equals(target.getKeepAlive(), DEF_REDISSON.getKeepAlive())) {
            target.setKeepAlive(source.getKeepAlive());
        }
        if (Objects.equals(target.getTcpNoDelay(), DEF_REDISSON.getTcpNoDelay())) {
            target.setTcpNoDelay(source.getTcpNoDelay());
        }
        target.setSingle(mergeRedissonSingle(source.getSingle(), target.getSingle()));
        target.setSlave(mergeRedissonSlave(source.getSlave(), target.getSlave()));
        target.setCluster(mergeRedissonCluster(source.getCluster(), target.getCluster()));
        target.setSentinel(mergeRedissonSentinel(source.getSentinel(), target.getSentinel()));
        return target;
    }

    private static RedisProperties.RedissonConfig.SingleConfig mergeRedissonSingle(RedisProperties.RedissonConfig.SingleConfig source, RedisProperties.RedissonConfig.SingleConfig target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getDnsMonitoringInterval(), DEF_REDISSON_SINGLE.getDnsMonitoringInterval())) {
            target.setDnsMonitoringInterval(source.getDnsMonitoringInterval());
        }
        if (Objects.equals(target.getConnectionPoolSize(), DEF_REDISSON_SINGLE.getConnectionPoolSize())) {
            target.setConnectionPoolSize(source.getConnectionPoolSize());
        }
        if (Objects.equals(target.getConnectionMinimumIdleSize(), DEF_REDISSON_SINGLE.getConnectionMinimumIdleSize())) {
            target.setConnectionMinimumIdleSize(source.getConnectionMinimumIdleSize());
        }
        if (Objects.equals(target.getSubscriptionConnectionPoolSize(), DEF_REDISSON_SINGLE.getSubscriptionConnectionPoolSize())) {
            target.setSubscriptionConnectionPoolSize(source.getSubscriptionConnectionPoolSize());
        }
        if (Objects.equals(target.getSubscriptionConnectionMinimumIdleSize(), DEF_REDISSON_SINGLE.getSubscriptionConnectionMinimumIdleSize())) {
            target.setSubscriptionConnectionMinimumIdleSize(source.getSubscriptionConnectionMinimumIdleSize());
        }
        return target;
    }

    private static RedisProperties.RedissonConfig.SlaveConfig mergeRedissonSlave(RedisProperties.RedissonConfig.SlaveConfig source, RedisProperties.RedissonConfig.SlaveConfig target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getSlaveConnectionPoolSize(), DEF_REDISSON_SLAVE.getSlaveConnectionPoolSize())) {
            target.setSlaveConnectionPoolSize(source.getSlaveConnectionPoolSize());
        }
        if (Objects.equals(target.getSlaveConnectionMinimumIdleSize(), DEF_REDISSON_SLAVE.getSlaveConnectionMinimumIdleSize())) {
            target.setSlaveConnectionMinimumIdleSize(source.getSlaveConnectionMinimumIdleSize());
        }
        if (Objects.equals(target.getSlaveFailsInterval(), DEF_REDISSON_SLAVE.getSlaveFailsInterval())) {
            target.setSlaveFailsInterval(source.getSlaveFailsInterval());
        }
        if (Objects.equals(target.getFailedSlavesReconnectionTimeout(), DEF_REDISSON_SLAVE.getFailedSlavesReconnectionTimeout())) {
            target.setFailedSlavesReconnectionTimeout(source.getFailedSlavesReconnectionTimeout());
        }
        if (Objects.equals(target.getMasterConnectionPoolSize(), DEF_REDISSON_SLAVE.getMasterConnectionPoolSize())) {
            target.setMasterConnectionPoolSize(source.getMasterConnectionPoolSize());
        }
        if (Objects.equals(target.getMasterConnectionMinimumIdleSize(), DEF_REDISSON_SLAVE.getMasterConnectionMinimumIdleSize())) {
            target.setMasterConnectionMinimumIdleSize(source.getMasterConnectionMinimumIdleSize());
        }
        if (Objects.equals(target.getReadMode(), DEF_REDISSON_SLAVE.getReadMode())) {
            target.setReadMode(source.getReadMode());
        }
        if (Objects.equals(target.getSubscriptionMode(), DEF_REDISSON_SLAVE.getSubscriptionMode())) {
            target.setSubscriptionMode(source.getSubscriptionMode());
        }
        if (Objects.equals(target.getSubscriptionConnectionPoolSize(), DEF_REDISSON_SLAVE.getSubscriptionConnectionPoolSize())) {
            target.setSubscriptionConnectionPoolSize(source.getSubscriptionConnectionPoolSize());
        }
        if (Objects.equals(target.getSubscriptionConnectionMinimumIdleSize(), DEF_REDISSON_SLAVE.getSubscriptionConnectionMinimumIdleSize())) {
            target.setSubscriptionConnectionMinimumIdleSize(source.getSubscriptionConnectionMinimumIdleSize());
        }
        if (Objects.equals(target.getDnsMonitoringInterval(), DEF_REDISSON_SLAVE.getDnsMonitoringInterval())) {
            target.setDnsMonitoringInterval(source.getDnsMonitoringInterval());
        }
        return target;
    }

    private static RedisProperties.RedissonConfig.ClusterConfig mergeRedissonCluster(RedisProperties.RedissonConfig.ClusterConfig source, RedisProperties.RedissonConfig.ClusterConfig target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getScanInterval(), DEF_REDISSON_CLUSTER.getScanInterval())) {
            target.setScanInterval(source.getScanInterval());
        }
        if (Objects.equals(target.getCheckSlotsCoverage(), DEF_REDISSON_CLUSTER.getCheckSlotsCoverage())) {
            target.setCheckSlotsCoverage(source.getCheckSlotsCoverage());
        }
        return target;
    }

    private static RedisProperties.RedissonConfig.SentinelConfig mergeRedissonSentinel(RedisProperties.RedissonConfig.SentinelConfig source, RedisProperties.RedissonConfig.SentinelConfig target) {
        if (source == null) {
            return target;
        }
        if (Objects.equals(target.getScanInterval(), DEF_REDISSON_SENTINEL.getScanInterval())) {
            target.setScanInterval(source.getScanInterval());
        }
        if (Objects.equals(target.getCheckSentinelsList(), DEF_REDISSON_SENTINEL.getCheckSentinelsList())) {
            target.setCheckSentinelsList(source.getCheckSentinelsList());
        }
        if (Objects.equals(target.getCheckSlaveStatusWithSyncing(), DEF_REDISSON_SENTINEL.getCheckSlaveStatusWithSyncing())) {
            target.setCheckSlaveStatusWithSyncing(source.getCheckSlaveStatusWithSyncing());
        }
        if (Objects.equals(target.getSentinelsDiscovery(), DEF_REDISSON_SENTINEL.getSentinelsDiscovery())) {
            target.setSentinelsDiscovery(source.getSentinelsDiscovery());
        }
        return target;
    }
}
