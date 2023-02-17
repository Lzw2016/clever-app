package org.clever.data.redis.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.clever.data.redis.config.RedisProperties;
import org.clever.data.redis.connection.*;
import org.clever.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.clever.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.clever.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.clever.data.redis.core.RedisTemplate;
import org.clever.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/13 16:34 <br/>
 */
public class RedisTemplateFactory {
    public static RedisTemplate<String, String> createRedisTemplate(RedisProperties properties,
                                                                    ClientResources clientResources,
                                                                    List<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
                                                                    ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper不能为空");
        // 创建 RedisTemplate
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(createConnectionFactory(properties, clientResources, builderCustomizers));
        initRedisTemplate(redisTemplate, objectMapper);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    private static void initRedisTemplate(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        // redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(objectMapper);
        // 设置序列化规则
        redisTemplate.setStringSerializer(RedisSerializer.string());
        redisTemplate.setDefaultSerializer(RedisSerializer.string());
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setEnableDefaultSerializer(true);
        redisTemplate.setValueSerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(serializer);
        // redisTemplate.setHashValueSerializer(RedisSerializer.string());
    }

    public static LettuceConnectionFactory createConnectionFactory(RedisProperties properties,
                                                                   ClientResources clientResources,
                                                                   List<LettuceClientConfigurationBuilderCustomizer> builderCustomizers) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        Assert.notNull(clientResources, "参数 clientResources 不能为 null");
        Assert.notNull(builderCustomizers, "参数 builderCustomizers 不能为 null");
        LettuceClientConfiguration clientConfiguration = createClientConfiguration(properties, clientResources, builderCustomizers);
        LettuceConnectionFactory connectionFactory = createConnectionFactory(properties, clientConfiguration);
        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }

    private static LettuceClientConfiguration createClientConfiguration(RedisProperties properties,
                                                                        ClientResources clientResources,
                                                                        List<LettuceClientConfigurationBuilderCustomizer> builderCustomizers) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        RedisProperties.Pool pool = properties.getPool();
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder;
        if (pool != null && pool.isEnabled()) {
            builder = LettucePoolingClientConfiguration.builder().poolConfig(createPoolConfig(pool));
        } else {
            builder = LettuceClientConfiguration.builder();
        }
        // 应用 properties 配置
        if (properties.isSsl()) {
            builder.useSsl();
        }
        if (properties.getReadTimeout() != null) {
            builder.commandTimeout(properties.getReadTimeout());
        }
        if (properties.getShutdownTimeout() != null) {
            builder.shutdownTimeout(properties.getShutdownTimeout());
        }
        if (properties.getClientName() != null) {
            builder.clientName(properties.getClientName());
        }
        builder.clientOptions(createClientOptions(properties));
        builder.clientResources(clientResources);
        // 应用自定义配置
        if (builderCustomizers != null) {
            builderCustomizers.forEach((customizer) -> customizer.customize(builder));
        }
        // 构建 LettuceClientConfiguration
        return builder.build();
    }

    private static GenericObjectPoolConfig<?> createPoolConfig(RedisProperties.Pool pool) {
        GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
        config.setMaxIdle(pool.getMaxIdle());
        config.setMinIdle(pool.getMinIdle());
        config.setMaxTotal(pool.getMaxActive());
        if (pool.getMaxWait() != null) {
            config.setMaxWait(pool.getMaxWait());
        }
        if (pool.getTimeBetweenEvictionRuns() != null) {
            config.setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns());
        }
        return config;
    }

    private static ClientOptions createClientOptions(RedisProperties properties) {
        Assert.notNull(properties, "参数 properties 不能为 null");
        ClientOptions.Builder builder;
        if (Objects.equals(properties.getMode(), RedisProperties.Mode.Cluster)) {
            ClusterClientOptions.Builder clusterBuilder = ClusterClientOptions.builder();
            if (properties.getCluster() != null && properties.getCluster().getRefresh() != null) {
                RedisProperties.Cluster.Refresh refresh = properties.getCluster().getRefresh();
                ClusterTopologyRefreshOptions.Builder refreshBuilder = ClusterTopologyRefreshOptions.builder().dynamicRefreshSources(refresh.isDynamicRefreshSources());
                if (refresh.getPeriod() != null) {
                    refreshBuilder.enablePeriodicRefresh(refresh.getPeriod());
                }
                if (refresh.isAdaptive()) {
                    refreshBuilder.enableAllAdaptiveRefreshTriggers();
                }
                clusterBuilder.topologyRefreshOptions(refreshBuilder.build());
            }
            builder = clusterBuilder;
        } else {
            builder = ClientOptions.builder();
        }
        if (properties.getConnectTimeout() != null) {
            builder.socketOptions(SocketOptions.builder().connectTimeout(properties.getConnectTimeout()).build());
        }
        return builder.timeoutOptions(TimeoutOptions.enabled()).build();
    }

    private static LettuceConnectionFactory createConnectionFactory(RedisProperties properties, LettuceClientConfiguration clientConfiguration) {
        RedisProperties.Mode mode = properties.getMode();
        Assert.notNull(mode, "参数 properties.getMode() 不能为 null");
        switch (mode) {
            case Standalone:
                return new LettuceConnectionFactory(
                        createStandaloneConfig(properties.getStandalone()),
                        clientConfiguration
                );
            case Sentinel:
                return new LettuceConnectionFactory(
                        createSentinelConfig(properties.getSentinel()),
                        clientConfiguration
                );
            case Cluster:
                return new LettuceConnectionFactory(
                        createClusterConfig(properties.getCluster()),
                        clientConfiguration
                );
        }
        throw new IllegalArgumentException("参数 properties.getMode() 不能为 null");
    }

    private static RedisStandaloneConfiguration createStandaloneConfig(RedisProperties.Standalone standalone) {
        Assert.notNull(standalone, "参数 standalone 不能为 null");
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(standalone.getHost());
        config.setPort(standalone.getPort());
        config.setDatabase(standalone.getDatabase());
        if (standalone.getUsername() != null) {
            config.setUsername(standalone.getUsername());
        }
        if (standalone.getPassword() != null) {
            config.setPassword(RedisPassword.of(standalone.getPassword()));
        }
        return config;
    }

    private static RedisSentinelConfiguration createSentinelConfig(RedisProperties.Sentinel sentinel) {
        Assert.notNull(sentinel, "参数 sentinel 不能为 null");
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinel.getMaster());
        List<RedisNode> sentinels = new ArrayList<>();
        for (String node : sentinel.getNodes()) {
            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts != null && parts.length == 2, "Must be defined as 'host:port'");
                sentinels.add(new RedisNode(parts[0], Integer.parseInt(parts[1])));
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Invalid redis sentinel " + "property '" + node + "'", ex);
            }
        }
        config.setSentinels(sentinels);
        config.setDatabase(sentinel.getDatabase());
        if (sentinel.getUsername() != null) {
            config.setUsername(sentinel.getUsername());
        }
        if (sentinel.getPassword() != null) {
            config.setPassword(RedisPassword.of(sentinel.getPassword()));
        }
        return config;
    }

    private static RedisClusterConfiguration createClusterConfig(RedisProperties.Cluster cluster) {
        Assert.notNull(cluster, "参数 cluster 不能为 null");
        RedisClusterConfiguration config = new RedisClusterConfiguration(cluster.getNodes());
        if (cluster.getMaxRedirects() != null) {
            config.setMaxRedirects(cluster.getMaxRedirects());
        }
        if (cluster.getUsername() != null) {
            config.setUsername(cluster.getUsername());
        }
        if (cluster.getPassword() != null) {
            config.setPassword(RedisPassword.of(cluster.getPassword()));
        }
        return config;
    }
}
