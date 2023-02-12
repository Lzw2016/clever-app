package org.clever.data.redis.connection;

import org.clever.core.env.MapPropertySource;
import org.clever.core.env.PropertySource;
import org.clever.data.redis.connection.RedisConfiguration.ClusterConfiguration;
import org.clever.util.Assert;
import org.clever.util.NumberUtils;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.util.*;

import static org.clever.util.StringUtils.commaDelimitedListToSet;

/**
 * 用于通过 {@link RedisConnectionFactory} 使用连接到 <a href="https://redis.io/topics/cluster-spec">Redis Cluster</a> 设置 {@link RedisConnection} 的配置类。
 * 在设置高可用性 Redis 环境时很有用
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:17 <br/>
 */
public class RedisClusterConfiguration implements RedisConfiguration, ClusterConfiguration {
    private static final String REDIS_CLUSTER_NODES_CONFIG_PROPERTY = "clever.redis.cluster.nodes";
    private static final String REDIS_CLUSTER_MAX_REDIRECTS_CONFIG_PROPERTY = "clever.redis.cluster.max-redirects";

    private final Set<RedisNode> clusterNodes;
    private Integer maxRedirects;
    private String username = null;
    private RedisPassword password = RedisPassword.none();

    /**
     * 创建新的 {@link RedisClusterConfiguration}
     */
    public RedisClusterConfiguration() {
        this(new MapPropertySource("RedisClusterConfiguration", Collections.emptyMap()));
    }

    /**
     * 为给定的主机端口组合创建 {@link RedisClusterConfiguration}
     *
     * <pre>{@code
     * clusterHostAndPorts[0] = 127.0.0.1:23679
     * clusterHostAndPorts[1] = 127.0.0.1:23680 ...
     * }
     * </pre>
     *
     * @param clusterNodes 不得为 {@literal null}
     */
    public RedisClusterConfiguration(Collection<String> clusterNodes) {
        this(new MapPropertySource("RedisClusterConfiguration", asMap(clusterNodes, -1)));
    }

    /**
     * 创建 {@link RedisClusterConfiguration} 在给定的 {@link PropertySource} 中查找值
     *
     * <pre>{@code
     * clever.redis.cluster.nodes=127.0.0.1:23679,127.0.0.1:23680,127.0.0.1:23681
     * clever.redis.cluster.max-redirects=3
     * }</pre>
     *
     * @param propertySource 不得为 {@literal null}
     */
    public RedisClusterConfiguration(PropertySource<?> propertySource) {
        Assert.notNull(propertySource, "PropertySource must not be null!");
        this.clusterNodes = new LinkedHashSet<>();
        if (propertySource.containsProperty(REDIS_CLUSTER_NODES_CONFIG_PROPERTY)) {
            appendClusterNodes(commaDelimitedListToSet(
                    propertySource.getProperty(REDIS_CLUSTER_NODES_CONFIG_PROPERTY).toString()
            ));
        }
        if (propertySource.containsProperty(REDIS_CLUSTER_MAX_REDIRECTS_CONFIG_PROPERTY)) {
            this.maxRedirects = NumberUtils.parseNumber(
                    propertySource.getProperty(REDIS_CLUSTER_MAX_REDIRECTS_CONFIG_PROPERTY).toString(),
                    Integer.class
            );
        }
    }

    /**
     * 设置要连接的 {@literal cluster nodes}
     *
     * @param nodes 不得为 {@literal null}
     */
    public void setClusterNodes(Iterable<RedisNode> nodes) {
        Assert.notNull(nodes, "Cannot set cluster nodes to 'null'.");
        this.clusterNodes.clear();
        for (RedisNode clusterNode : nodes) {
            addClusterNode(clusterNode);
        }
    }

    @Override
    public Set<RedisNode> getClusterNodes() {
        return Collections.unmodifiableSet(clusterNodes);
    }

    /**
     * 将集群节点添加到配置中
     *
     * @param node 不得为 {@literal null}
     */
    public void addClusterNode(RedisNode node) {
        Assert.notNull(node, "ClusterNode must not be 'null'.");
        this.clusterNodes.add(node);
    }

    /**
     * @return this
     */
    public RedisClusterConfiguration clusterNode(RedisNode node) {
        this.clusterNodes.add(node);
        return this;
    }

    @Override
    public Integer getMaxRedirects() {
        return maxRedirects != null && maxRedirects > Integer.MIN_VALUE ? maxRedirects : null;
    }

    /**
     * @param maxRedirects 要遵循的最大重定向数
     */
    public void setMaxRedirects(int maxRedirects) {
        Assert.isTrue(maxRedirects >= 0, "MaxRedirects must be greater or equal to 0");
        this.maxRedirects = maxRedirects;
    }

    /**
     * @param host Redis集群节点主机名或ip地址
     * @param port Redis集群节点端口
     * @return this
     */
    public RedisClusterConfiguration clusterNode(String host, Integer port) {
        return clusterNode(new RedisNode(host, port));
    }

    private void appendClusterNodes(Set<String> hostAndPorts) {
        for (String hostAndPort : hostAndPorts) {
            addClusterNode(RedisNode.fromString(hostAndPort));
        }
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public RedisPassword getPassword() {
        return password;
    }

    @Override
    public void setPassword(RedisPassword password) {
        Assert.notNull(password, "RedisPassword must not be null!");
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisClusterConfiguration)) {
            return false;
        }
        RedisClusterConfiguration that = (RedisClusterConfiguration) o;
        if (!ObjectUtils.nullSafeEquals(clusterNodes, that.clusterNodes)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(maxRedirects, that.maxRedirects)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(username, that.username)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(password, that.password);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(clusterNodes);
        result = 31 * result + ObjectUtils.nullSafeHashCode(maxRedirects);
        result = 31 * result + ObjectUtils.nullSafeHashCode(username);
        result = 31 * result + ObjectUtils.nullSafeHashCode(password);
        return result;
    }

    /**
     * @param clusterHostAndPorts 不得为 {@literal null} 或为空
     * @param redirects           要遵循的最大重定向数
     * @return 具有属性的集群配置映射
     */
    @SuppressWarnings("SameParameterValue")
    private static Map<String, Object> asMap(Collection<String> clusterHostAndPorts, int redirects) {
        Assert.notNull(clusterHostAndPorts, "ClusterHostAndPorts must not be null!");
        Assert.noNullElements(clusterHostAndPorts, "ClusterHostAndPorts must not contain null elements!");
        Map<String, Object> map = new HashMap<>();
        map.put(REDIS_CLUSTER_NODES_CONFIG_PROPERTY, StringUtils.collectionToCommaDelimitedString(clusterHostAndPorts));
        if (redirects >= 0) {
            map.put(REDIS_CLUSTER_MAX_REDIRECTS_CONFIG_PROPERTY, redirects);
        }
        return map;
    }
}
