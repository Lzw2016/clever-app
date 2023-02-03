package org.clever.data.redis.connection;

import org.clever.data.redis.connection.RedisConfiguration.WithDatabaseIndex;
import org.clever.data.redis.connection.RedisConfiguration.WithHostAndPort;
import org.clever.data.redis.connection.RedisConfiguration.WithPassword;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 配置类用于通过 {@link RedisConnectionFactory} 建立 {@link RedisConnectionFactory，连接到单个 <a href="https://redis.io/">Redis</a> 节点
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:28 <br/>
 */
public class RedisStandaloneConfiguration implements RedisConfiguration, WithHostAndPort, WithPassword, WithDatabaseIndex {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;

    private String hostName = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private int database;
    private String username = null;
    private RedisPassword password = RedisPassword.none();

    /**
     * 创建新的默认 {@link RedisStandaloneConfiguration}
     */
    public RedisStandaloneConfiguration() {
    }

    /**
     * 在给定 {@code hostName} 的情况下创建新的 {@link RedisStandaloneConfiguration}
     *
     * @param hostName 不能为 {@literal null} 或空
     */
    public RedisStandaloneConfiguration(String hostName) {
        this(hostName, DEFAULT_PORT);
    }

    /**
     * 在给定 {@code hostName} 和 {@code port} 的情况下创建新的 {@link RedisStandaloneConfiguration}
     *
     * @param hostName 不能为 {@literal null} 或空
     * @param port     有效的TCP端口（1-65535）
     */
    public RedisStandaloneConfiguration(String hostName, int port) {
        Assert.hasText(hostName, "Host name must not be null or empty!");
        Assert.isTrue(port >= 1 && port <= 65535, () -> String.format("Port %d must be a valid TCP port in the range between 1-65535!", port));
        this.hostName = hostName;
        this.port = port;
    }

    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setPort(int port) {
        this.port = port;
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
        if (!(o instanceof RedisStandaloneConfiguration)) {
            return false;
        }
        RedisStandaloneConfiguration that = (RedisStandaloneConfiguration) o;
        if (port != that.port) {
            return false;
        }
        if (database != that.database) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(hostName, that.hostName)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(username, that.username)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(password, that.password);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(hostName);
        result = 31 * result + port;
        result = 31 * result + database;
        result = 31 * result + ObjectUtils.nullSafeHashCode(username);
        result = 31 * result + ObjectUtils.nullSafeHashCode(password);
        return result;
    }
}
