package org.clever.data.redis.connection;

import org.clever.data.redis.connection.RedisConfiguration.StaticMasterReplicaConfiguration;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置类用于通过 {@link RedisConnectionFactory} 设置 {@link RedisConnection} 使用提供的主副本配置节点知道不更改地址。
 * 例如。当连接到 <a href="https://aws.amazon.com/documentation/elasticache/">AWS ElastiCache with Read Replicas</a> 时。<br/>
 * 注意：Redis 正在进行术语更改，其中术语副本与从属同义。
 * 另请注意，MasterReplica 连接不能用于 Pub/Sub 操作。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:14 <br/>
 */
public class RedisStaticMasterReplicaConfiguration implements RedisConfiguration, StaticMasterReplicaConfiguration {
    private static final int DEFAULT_PORT = 6379;

    private final List<RedisStandaloneConfiguration> nodes = new ArrayList<>();
    private int database;
    private String username = null;
    private RedisPassword password = RedisPassword.none();

    /**
     * 给定 {@code hostName} 创建一个新的 {@link StaticMasterReplicaConfiguration}
     *
     * @param hostName 不能为 {@literal null} or empty.
     */
    public RedisStaticMasterReplicaConfiguration(String hostName) {
        this(hostName, DEFAULT_PORT);
    }

    /**
     * 给定 {@code hostName} 和 {@code port} 创建一个新的 {@link StaticMasterReplicaConfiguration}
     *
     * @param hostName 不得为 {@literal null} 或为空
     * @param port     一个有效的 TCP 端口 (1-65535)
     */
    public RedisStaticMasterReplicaConfiguration(String hostName, int port) {
        addNode(hostName, port);
    }

    /**
     * 将 {@link RedisStandaloneConfiguration node} 添加到给定 {@code hostName} 的节点列表中
     *
     * @param hostName 不得为 {@literal null} 或为空
     * @param port     一个有效的 TCP 端口 (1-65535)
     */
    public void addNode(String hostName, int port) {
        addNode(new RedisStandaloneConfiguration(hostName, port));
    }

    /**
     * 将 {@link RedisStandaloneConfiguration node} 添加到节点列表
     *
     * @param node 不得为 {@literal null}
     */
    private void addNode(RedisStandaloneConfiguration node) {
        Assert.notNull(node, "RedisStandaloneConfiguration must not be null!");
        node.setPassword(password);
        node.setDatabase(database);
        nodes.add(node);
    }

    /**
     * 将 {@link RedisStandaloneConfiguration node} 添加到给定 {@code hostName} 的节点列表中
     *
     * @param hostName 不得为 {@literal null} 或为空
     * @return {@code this} {@link StaticMasterReplicaConfiguration}.
     */
    public RedisStaticMasterReplicaConfiguration node(String hostName) {
        return node(hostName, DEFAULT_PORT);
    }

    /**
     * 将 {@link RedisStandaloneConfiguration node} 添加到给定 {@code hostName} 和 {@code port} 的节点列表中
     *
     * @param hostName 不得为 {@literal null} 或为空
     * @param port     一个有效的 TCP 端口 (1-65535)
     * @return {@code this} {@link StaticMasterReplicaConfiguration}.
     */
    public RedisStaticMasterReplicaConfiguration node(String hostName, int port) {
        addNode(hostName, port);
        return this;
    }

    @Override
    public int getDatabase() {
        return database;
    }

    @Override
    public void setDatabase(int index) {
        Assert.isTrue(index >= 0, () -> String.format("Invalid DB index '%s' (a positive index required)", index));
        this.database = index;
        this.nodes.forEach(it -> it.setDatabase(database));
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
        this.nodes.forEach(it -> it.setPassword(password));
    }

    @Override
    public List<RedisStandaloneConfiguration> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisStaticMasterReplicaConfiguration)) {
            return false;
        }
        RedisStaticMasterReplicaConfiguration that = (RedisStaticMasterReplicaConfiguration) o;
        if (database != that.database) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(nodes, that.nodes)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(username, that.username)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(password, that.password);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(nodes);
        result = 31 * result + database;
        result = 31 * result + ObjectUtils.nullSafeHashCode(username);
        result = 31 * result + ObjectUtils.nullSafeHashCode(password);
        return result;
    }
}
