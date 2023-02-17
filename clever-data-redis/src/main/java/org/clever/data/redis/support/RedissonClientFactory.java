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
        // clientName
        // username
        // password
        // timeout
        // connectTimeout
        // idleConnectionTimeout
        // retryInterval
        // retryAttempts
        // subscriptionsPerConnection
        // sslEnableEndpointIdentification
        // pingConnectionInterval
        // keepAlive
        // tcpNoDelay
    }

    private static void applyConfig(RedisProperties properties, BaseMasterSlaveServersConfig<?> config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseConfig<?>) config);
        // BaseMasterSlaveServersConfig
        // slaveConnectionPoolSize
        // slaveConnectionMinimumIdleSize
        // failedSlaveCheckInterval
        // failedSlaveReconnectionInterval
        // masterConnectionPoolSize
        // masterConnectionMinimumIdleSize
        // readMode
        // subscriptionMode
        // subscriptionConnectionPoolSize
        // subscriptionConnectionMinimumIdleSize
        // dnsMonitoringInterval
    }

    private static void applyConfig(RedisProperties properties, SingleServerConfig config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseConfig<?>) config);
        // singleServerConfig.setDnsMonitoringInterval()
        // singleServerConfig.setConnectionPoolSize()
        // singleServerConfig.setConnectionMinimumIdleSize()
        // singleServerConfig.setSubscriptionConnectionPoolSize()
        // singleServerConfig.setSubscriptionConnectionMinimumIdleSize()
    }

    private static void applyConfig(RedisProperties properties, SentinelServersConfig config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseMasterSlaveServersConfig<?>) config);
        // sentinelServersConfig.setNatMapper()
        // sentinelServersConfig.setScanInterval()
        // sentinelServersConfig.setCheckSentinelsList()
        // sentinelServersConfig.setCheckSlaveStatusWithSyncing()
        // sentinelServersConfig.setSentinelsDiscovery()
    }

    private static void applyConfig(RedisProperties properties, ClusterServersConfig config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        applyConfig(properties, (BaseMasterSlaveServersConfig<?>) config);
        // clusterServersConfig.setNatMapper()
        // clusterServersConfig.setScanInterval()
        // clusterServersConfig.setCheckSlotsCoverage()
    }

    private static void applyConfig(RedisProperties properties, Config config) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        // threads
        // nettyThreads
        // executor
        // referenceEnabled
        // transportMode
        // lockWatchdogTimeout
        // checkLockSyncedSlaves
        // reliableTopicWatchdogTimeout
        // keepPubSubOrder
        // useScriptCache
        // minCleanUpDelay
        // maxCleanUpDelay
        // cleanUpKeysAmount
        // useThreadClassLoader
    }
}
