package org.clever.data.redis.connection;

import org.clever.core.env.MapPropertySource;
import org.clever.core.env.PropertySource;
import org.clever.data.redis.connection.RedisConfiguration.SentinelConfiguration;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.util.*;

import static org.clever.util.StringUtils.commaDelimitedListToSet;

/**
 * 用于通过 {@link RedisConnectionFactory} 使用连接到 <a href="https://redis.io/topics/sentinel">Redis Sentinel(s)</a> 设置 {@link RedisConnection} 的配置类。
 * 在设置高可用性 Redis 环境时很有用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:24 <br/>
 */
public class RedisSentinelConfiguration implements RedisConfiguration, SentinelConfiguration {
    private static final String REDIS_SENTINEL_MASTER_CONFIG_PROPERTY = "clever.redis.sentinel.master";
    private static final String REDIS_SENTINEL_NODES_CONFIG_PROPERTY = "clever.redis.sentinel.nodes";
    private static final String REDIS_SENTINEL_PASSWORD_CONFIG_PROPERTY = "clever.redis.sentinel.password";

    private NamedNode master;
    private final Set<RedisNode> sentinels;
    private int database;
    private String dataNodeUsername = null;
    private RedisPassword dataNodePassword = RedisPassword.none();
    private RedisPassword sentinelPassword = RedisPassword.none();

    /**
     * Creates new {@link RedisSentinelConfiguration}.
     */
    public RedisSentinelConfiguration() {
        this(new MapPropertySource("RedisSentinelConfiguration", Collections.emptyMap()));
    }

    /**
     * 为给定的主机端口组合创建 {@link RedisSentinelConfiguration}
     *
     * <pre>{@code
     * sentinelHostAndPorts[0] = 127.0.0.1:23679 sentinelHostAndPorts[1] = 127.0.0.1:23680 ...
     * }</pre>
     *
     * @param sentinelHostAndPorts 不得为 {@literal null}
     */
    public RedisSentinelConfiguration(String master, Set<String> sentinelHostAndPorts) {
        this(new MapPropertySource("RedisSentinelConfiguration", asMap(master, sentinelHostAndPorts)));
    }

    /**
     * 创建 {@link RedisSentinelConfiguration} 在给定的 {@link PropertySource} 中查找值
     *
     * <pre>{@code
     * clever.redis.sentinel.master=myMaster
     * clever.redis.sentinel.nodes=127.0.0.1:23679,127.0.0.1:23680,127.0.0.1:23681
     * }</pre>
     *
     * @param propertySource 不得为 {@literal null}
     */
    public RedisSentinelConfiguration(PropertySource<?> propertySource) {
        Assert.notNull(propertySource, "PropertySource must not be null!");
        this.sentinels = new LinkedHashSet<>();
        if (propertySource.containsProperty(REDIS_SENTINEL_MASTER_CONFIG_PROPERTY)) {
            this.setMaster(propertySource.getProperty(REDIS_SENTINEL_MASTER_CONFIG_PROPERTY).toString());
        }
        if (propertySource.containsProperty(REDIS_SENTINEL_NODES_CONFIG_PROPERTY)) {
            appendSentinels(commaDelimitedListToSet(propertySource.getProperty(REDIS_SENTINEL_NODES_CONFIG_PROPERTY).toString()));
        }
        if (propertySource.containsProperty(REDIS_SENTINEL_PASSWORD_CONFIG_PROPERTY)) {
            this.setSentinelPassword(propertySource.getProperty(REDIS_SENTINEL_PASSWORD_CONFIG_PROPERTY).toString());
        }
    }

    /**
     * 设置要连接的 {@literal Sentinels}
     *
     * @param sentinels 不得为 {@literal null}
     */
    public void setSentinels(Iterable<RedisNode> sentinels) {
        Assert.notNull(sentinels, "Cannot set sentinels to 'null'.");
        this.sentinels.clear();
        for (RedisNode sentinel : sentinels) {
            addSentinel(sentinel);
        }
    }

    public Set<RedisNode> getSentinels() {
        return Collections.unmodifiableSet(sentinels);
    }

    /**
     * 添加哨兵
     *
     * @param sentinel 不得为 {@literal null}
     */
    public void addSentinel(RedisNode sentinel) {
        Assert.notNull(sentinel, "Sentinel must not be 'null'.");
        this.sentinels.add(sentinel);
    }

    public void setMaster(NamedNode master) {
        Assert.notNull(master, "Sentinel master node must not be 'null'.");
        this.master = master;
    }

    public NamedNode getMaster() {
        return master;
    }

    /**
     * @param master 主节点名称
     * @return this
     * @see #setMaster(String)
     */
    public RedisSentinelConfiguration master(String master) {
        this.setMaster(master);
        return this;
    }

    /**
     * @param master 主节点
     * @return this
     * @see #setMaster(NamedNode)
     */
    public RedisSentinelConfiguration master(NamedNode master) {
        this.setMaster(master);
        return this;
    }

    /**
     * @param sentinel 要添加为哨兵的节点
     * @return this
     * @see #addSentinel(RedisNode)
     */
    public RedisSentinelConfiguration sentinel(RedisNode sentinel) {
        this.addSentinel(sentinel);
        return this;
    }

    /**
     * @param host redis 哨兵节点主机名或 ip
     * @param port Redis哨兵端口
     * @return this
     * @see #sentinel(RedisNode)
     */
    public RedisSentinelConfiguration sentinel(String host, Integer port) {
        return sentinel(new RedisNode(host, port));
    }

    private void appendSentinels(Set<String> hostAndPorts) {
        for (String hostAndPort : hostAndPorts) {
            addSentinel(RedisNode.fromString(hostAndPort));
        }
    }

    @Override
    public int getDatabase() {
        return database;
    }

    @Override
    public void setDatabase(int index) {
        Assert.isTrue(index >= 0, () -> String.format("Invalid DB index '%s' (a positive index required)", index));
        this.database = index;
    }

    @Override
    public void setUsername(String username) {
        this.dataNodeUsername = username;
    }

    @Override
    public String getUsername() {
        return this.dataNodeUsername;
    }

    @Override
    public RedisPassword getPassword() {
        return dataNodePassword;
    }

    @Override
    public void setPassword(RedisPassword password) {
        Assert.notNull(password, "RedisPassword must not be null!");
        this.dataNodePassword = password;
    }

    public void setSentinelPassword(RedisPassword sentinelPassword) {
        Assert.notNull(sentinelPassword, "SentinelPassword must not be null!");
        this.sentinelPassword = sentinelPassword;
    }

    @Override
    public RedisPassword getSentinelPassword() {
        return sentinelPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisSentinelConfiguration)) {
            return false;
        }
        RedisSentinelConfiguration that = (RedisSentinelConfiguration) o;
        if (database != that.database) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(master, that.master)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(sentinels, that.sentinels)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(dataNodeUsername, that.dataNodeUsername)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(dataNodePassword, that.dataNodePassword)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(sentinelPassword, that.sentinelPassword);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(master);
        result = 31 * result + ObjectUtils.nullSafeHashCode(sentinels);
        result = 31 * result + database;
        result = 31 * result + ObjectUtils.nullSafeHashCode(dataNodeUsername);
        result = 31 * result + ObjectUtils.nullSafeHashCode(dataNodePassword);
        result = 31 * result + ObjectUtils.nullSafeHashCode(sentinelPassword);
        return result;
    }

    /**
     * @param master               不得为 {@literal null} 或为空
     * @param sentinelHostAndPorts 不得为 {@literal null}
     * @return 配置 Map
     */
    private static Map<String, Object> asMap(String master, Set<String> sentinelHostAndPorts) {
        Assert.hasText(master, "Master address must not be null or empty!");
        Assert.notNull(sentinelHostAndPorts, "SentinelHostAndPorts must not be null!");
        Assert.noNullElements(sentinelHostAndPorts, "ClusterHostAndPorts must not contain null elements!");
        Map<String, Object> map = new HashMap<>();
        map.put(REDIS_SENTINEL_MASTER_CONFIG_PROPERTY, master);
        map.put(REDIS_SENTINEL_NODES_CONFIG_PROPERTY, StringUtils.collectionToCommaDelimitedString(sentinelHostAndPorts));
        return map;
    }
}
