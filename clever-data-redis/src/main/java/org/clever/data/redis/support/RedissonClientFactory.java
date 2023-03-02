package org.clever.data.redis.support;

import org.clever.data.redis.RedissonClientConfigurationCustomizer;
import org.clever.data.redis.config.RedisProperties;
import org.clever.util.Assert;
import org.redisson.config.*;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/16 10:16 <br/>
 */
public class RedissonClientFactory {
    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDIS_SSL_PROTOCOL_PREFIX = "rediss://";

    public static Config createConfig(RedisProperties properties, List<RedissonClientConfigurationCustomizer> customizers) {
        final Config config = createConfig(properties);
        applyConfig(properties, config);
        // 应用自定义配置
        if (customizers != null) {
            customizers.forEach((customizer) -> customizer.customize(config));
        }
        return config;
    }

    private static Config createConfig(RedisProperties properties) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(properties.getMode(), "参数 properties.getMode() 不能为 null");
        final String clientName = properties.getClientName();
        final Config config = new Config();
        final RedisProperties.Sentinel sentinel = properties.getSentinel();
        if (Objects.equals(RedisProperties.Mode.Sentinel, properties.getMode()) && sentinel != null) {
            String[] nodes = convert(sentinel.getNodes());
            SentinelServersConfig sentinelServersConfig = config.useSentinelServers()
                    .setMasterName(sentinel.getMaster())
                    .addSentinelAddress(nodes)
                    .setDatabase(sentinel.getDatabase())
                    .setUsername(sentinel.getUsername())
                    .setPassword(sentinel.getPassword())
                    .setClientName(clientName);
            applyConfig(properties, sentinelServersConfig);
            return config;
        }
        final RedisProperties.Cluster cluster = properties.getCluster();
        if (Objects.equals(RedisProperties.Mode.Cluster, properties.getMode()) && cluster != null) {
            String[] nodes = convert(cluster.getNodes());
            ClusterServersConfig clusterServersConfig = config.useClusterServers()
                    .addNodeAddress(nodes)
                    .setUsername(cluster.getUsername())
                    .setPassword(cluster.getPassword())
                    .setClientName(clientName);
            applyConfig(properties, clusterServersConfig);
            return config;
        }
        final RedisProperties.Standalone standalone = properties.getStandalone();
        if (Objects.equals(RedisProperties.Mode.Standalone, properties.getMode()) && standalone != null) {
            String prefix = properties.isSsl() ? REDIS_SSL_PROTOCOL_PREFIX : REDIS_PROTOCOL_PREFIX;
            SingleServerConfig singleServerConfig = config.useSingleServer()
                    .setAddress(prefix + standalone.getHost() + ":" + standalone.getPort())
                    .setDatabase(standalone.getDatabase())
                    .setUsername(standalone.getUsername())
                    .setPassword(standalone.getPassword())
                    .setClientName(clientName);
            applyConfig(properties, singleServerConfig);
            return config;
        }
        throw new IllegalArgumentException("未知的Mode=" + properties.getMode());
    }

    private static String[] convert(List<String> nodes) {
        Assert.notNull(nodes, "参数 nodes 不能为 null");
        return nodes.stream().map(node -> {
            if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDIS_SSL_PROTOCOL_PREFIX)) {
                return REDIS_PROTOCOL_PREFIX + node;
            } else {
                return node;
            }
        }).toArray(String[]::new);
    }

    private static void applyConfig(RedisProperties properties, BaseConfig<?> config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        final Duration readTimeout = properties.getReadTimeout();
        final Duration connectTimeout = properties.getConnectTimeout();
        if (connectTimeout != null) {
            config.setConnectTimeout((int) connectTimeout.toMillis());
        }
        if (readTimeout != null) {
            config.setTimeout((int) readTimeout.toMillis());
        }
        // BaseConfig
        RedisProperties.RedissonConfig redisson = properties.getRedisson();
        if (redisson == null) {
            return;
        }
        Integer idleConnectionTimeout = redisson.getIdleConnectionTimeout();
        if (idleConnectionTimeout != null) {
            config.setIdleConnectionTimeout(idleConnectionTimeout);
        }
        Integer retryInterval = redisson.getRetryInterval();
        if (retryInterval != null) {
            config.setRetryInterval(retryInterval);
        }
        Integer retryAttempts = redisson.getRetryAttempts();
        if (retryAttempts != null) {
            config.setRetryAttempts(retryAttempts);
        }
        Integer subscriptionsPerConnection = redisson.getSubscriptionsPerConnection();
        if (subscriptionsPerConnection != null) {
            config.setSubscriptionsPerConnection(subscriptionsPerConnection);
        }
        Boolean sslEnableEndpointIdentification = redisson.getSslEnableEndpointIdentification();
        if (sslEnableEndpointIdentification != null) {
            config.setSslEnableEndpointIdentification(sslEnableEndpointIdentification);
        }
        Integer pingConnectionInterval = redisson.getPingConnectionInterval();
        if (pingConnectionInterval != null) {
            config.setPingConnectionInterval(pingConnectionInterval);
        }
        Boolean keepAlive = redisson.getKeepAlive();
        if (keepAlive != null) {
            config.setKeepAlive(keepAlive);
        }
        Boolean tcpNoDelay = redisson.getTcpNoDelay();
        if (tcpNoDelay != null) {
            config.setTcpNoDelay(tcpNoDelay);
        }
    }

    private static void applyConfig(RedisProperties properties, BaseMasterSlaveServersConfig<?> config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseConfig<?>) config);
        // BaseMasterSlaveServersConfig
        RedisProperties.RedissonConfig redisson = properties.getRedisson();
        if (redisson == null) {
            return;
        }
        RedisProperties.RedissonConfig.SlaveConfig slave = redisson.getSlave();
        if (slave == null) {
            return;
        }
        Integer slaveConnectionPoolSize = slave.getSlaveConnectionPoolSize();
        if (slaveConnectionPoolSize != null) {
            config.setSlaveConnectionPoolSize(slaveConnectionPoolSize);
        }
        Integer slaveConnectionMinimumIdleSize = slave.getSlaveConnectionMinimumIdleSize();
        if (slaveConnectionMinimumIdleSize != null) {
            config.setSlaveConnectionMinimumIdleSize(slaveConnectionMinimumIdleSize);
        }
        Integer slaveFailsInterval = slave.getSlaveFailsInterval();
        if (slaveFailsInterval != null) {
            config.setFailedSlaveCheckInterval(slaveFailsInterval);
        }
        Integer failedSlavesReconnectionTimeout = slave.getFailedSlavesReconnectionTimeout();
        if (failedSlavesReconnectionTimeout != null) {
            config.setFailedSlaveReconnectionInterval(failedSlavesReconnectionTimeout);
        }
        Integer masterConnectionPoolSize = slave.getMasterConnectionPoolSize();
        if (masterConnectionPoolSize != null) {
            config.setMasterConnectionPoolSize(masterConnectionPoolSize);
        }
        Integer masterConnectionMinimumIdleSize = slave.getMasterConnectionMinimumIdleSize();
        if (masterConnectionMinimumIdleSize != null) {
            config.setMasterConnectionMinimumIdleSize(masterConnectionMinimumIdleSize);
        }
        ReadMode readMode = slave.getReadMode();
        if (readMode != null) {
            config.setReadMode(readMode);
        }
        SubscriptionMode subscriptionMode = slave.getSubscriptionMode();
        if (subscriptionMode != null) {
            config.setSubscriptionMode(subscriptionMode);
        }
        Integer subscriptionConnectionPoolSize = slave.getSubscriptionConnectionPoolSize();
        if (subscriptionConnectionPoolSize != null) {
            config.setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize);
        }
        Integer subscriptionConnectionMinimumIdleSize = slave.getSubscriptionConnectionMinimumIdleSize();
        if (subscriptionConnectionMinimumIdleSize != null) {
            config.setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize);
        }
        Long dnsMonitoringInterval = slave.getDnsMonitoringInterval();
        if (dnsMonitoringInterval != null) {
            config.setDnsMonitoringInterval(dnsMonitoringInterval);
        }
    }

    private static void applyConfig(RedisProperties properties, SingleServerConfig config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseConfig<?>) config);
        // SingleServerConfig
        RedisProperties.RedissonConfig redisson = properties.getRedisson();
        if (redisson == null) {
            return;
        }
        RedisProperties.RedissonConfig.SingleConfig single = redisson.getSingle();
        if (single == null) {
            return;
        }
        Long dnsMonitoringInterval = single.getDnsMonitoringInterval();
        if (dnsMonitoringInterval != null) {
            config.setDnsMonitoringInterval(dnsMonitoringInterval);
        }
        Integer connectionPoolSize = single.getConnectionPoolSize();
        if (connectionPoolSize != null) {
            config.setConnectionPoolSize(connectionPoolSize);
        }
        Integer connectionMinimumIdleSize = single.getConnectionMinimumIdleSize();
        if (connectionMinimumIdleSize != null) {
            config.setConnectionMinimumIdleSize(connectionMinimumIdleSize);
        }
        Integer subscriptionConnectionPoolSize = single.getSubscriptionConnectionPoolSize();
        if (subscriptionConnectionPoolSize != null) {
            config.setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize);
        }
        Integer subscriptionConnectionMinimumIdleSize = single.getSubscriptionConnectionMinimumIdleSize();
        if (subscriptionConnectionMinimumIdleSize != null) {
            config.setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize);
        }
    }

    private static void applyConfig(RedisProperties properties, SentinelServersConfig config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseMasterSlaveServersConfig<?>) config);
        // SentinelServersConfig
        RedisProperties.RedissonConfig redisson = properties.getRedisson();
        if (redisson == null) {
            return;
        }
        RedisProperties.RedissonConfig.SentinelConfig sentinel = redisson.getSentinel();
        if (sentinel == null) {
            return;
        }
        // config.setNatMapper()
        Integer scanInterval = sentinel.getScanInterval();
        if (scanInterval != null) {
            config.setScanInterval(scanInterval);
        }
        Boolean checkSentinelsList = sentinel.getCheckSentinelsList();
        if (checkSentinelsList != null) {
            config.setCheckSentinelsList(checkSentinelsList);
        }
        Boolean checkSlaveStatusWithSyncing = sentinel.getCheckSlaveStatusWithSyncing();
        if (checkSlaveStatusWithSyncing != null) {
            config.setCheckSlaveStatusWithSyncing(checkSlaveStatusWithSyncing);
        }
        Boolean sentinelsDiscovery = sentinel.getSentinelsDiscovery();
        if (sentinelsDiscovery != null) {
            config.setSentinelsDiscovery(sentinelsDiscovery);
        }
    }

    private static void applyConfig(RedisProperties properties, ClusterServersConfig config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseMasterSlaveServersConfig<?>) config);
        // ClusterServersConfig
        RedisProperties.RedissonConfig redisson = properties.getRedisson();
        if (redisson == null) {
            return;
        }
        RedisProperties.RedissonConfig.ClusterConfig cluster = redisson.getCluster();
        if (cluster == null) {
            return;
        }
        // config.setNatMapper()
        Integer scanInterval = cluster.getScanInterval();
        if (scanInterval != null) {
            config.setScanInterval(scanInterval);
        }
        Boolean checkSlotsCoverage = cluster.getCheckSlotsCoverage();
        if (checkSlotsCoverage != null) {
            config.setCheckSlotsCoverage(checkSlotsCoverage);
        }
    }

    private static void applyConfig(RedisProperties properties, Config config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        // Config
        RedisProperties.RedissonConfig redisson = properties.getRedisson();
        if (redisson == null) {
            return;
        }
        Integer threads = redisson.getThreads();
        if (threads != null) {
            config.setThreads(threads);
        }
        Integer nettyThreads = redisson.getThreads();
        if (nettyThreads != null) {
            config.setNettyThreads(nettyThreads);
        }
        // config.setExecutor()
        Boolean redissonReferenceEnabled = redisson.getRedissonReferenceEnabled();
        if (redissonReferenceEnabled != null) {
            config.setReferenceEnabled(redissonReferenceEnabled);
        }
        TransportMode transportMode = redisson.getTransportMode();
        if (transportMode != null) {
            config.setTransportMode(transportMode);
        }
        Long lockWatchdogTimeout = redisson.getLockWatchdogTimeout();
        if (lockWatchdogTimeout != null) {
            config.setLockWatchdogTimeout(lockWatchdogTimeout);
        }
        Boolean checkLockSyncedSlaves = redisson.getCheckLockSyncedSlaves();
        if (checkLockSyncedSlaves != null) {
            config.setCheckLockSyncedSlaves(checkLockSyncedSlaves);
        }
        Long reliableTopicWatchdogTimeout = redisson.getReliableTopicWatchdogTimeout();
        if (reliableTopicWatchdogTimeout != null) {
            config.setReliableTopicWatchdogTimeout(reliableTopicWatchdogTimeout);
        }
        Boolean keepPubSubOrder = redisson.getKeepPubSubOrder();
        if (keepPubSubOrder != null) {
            config.setKeepPubSubOrder(keepPubSubOrder);
        }
        Boolean useScriptCache = redisson.getUseScriptCache();
        if (useScriptCache != null) {
            config.setUseScriptCache(useScriptCache);
        }
        Integer minCleanUpDelay = redisson.getMinCleanUpDelay();
        if (minCleanUpDelay != null) {
            config.setMinCleanUpDelay(minCleanUpDelay);
        }
        Integer maxCleanUpDelay = redisson.getMaxCleanUpDelay();
        if (maxCleanUpDelay != null) {
            config.setMaxCleanUpDelay(maxCleanUpDelay);
        }
        Integer cleanUpKeysAmount = redisson.getCleanUpKeysAmount();
        if (cleanUpKeysAmount != null) {
            config.setCleanUpKeysAmount(cleanUpKeysAmount);
        }
        Boolean useThreadClassLoader = redisson.getUseThreadClassLoader();
        if (useThreadClassLoader != null) {
            config.setUseThreadClassLoader(useThreadClassLoader);
        }
    }
}
