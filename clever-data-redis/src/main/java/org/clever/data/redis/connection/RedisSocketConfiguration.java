package org.clever.data.redis.connection;

import org.clever.data.redis.connection.RedisConfiguration.DomainSocketConfiguration;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 配置类用于设置 {@link RedisConnection} 通过 {@link RedisConnectionFactory} 连接到单个 <a href="https://redis.io/">Redis</a> 使用本地 unix domain socket
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:44 <br/>
 */
public class RedisSocketConfiguration implements RedisConfiguration, DomainSocketConfiguration {
    private static final String DEFAULT_SOCKET = "/tmp/redis.sock";

    private String socket = DEFAULT_SOCKET;
    private int database;
    private String username = null;
    private RedisPassword password = RedisPassword.none();

    /**
     * 创建一个新的默认 {@link RedisSocketConfiguration}
     */
    public RedisSocketConfiguration() {
    }

    /**
     * 给 {@code socket} 创建一个新的 {@link RedisSocketConfiguration}
     *
     * @param socket 不能为 {@literal null} 或空
     */
    public RedisSocketConfiguration(String socket) {
        Assert.hasText(socket, "Socket path must not be null nor empty!");
        this.socket = socket;
    }

    @Override
    public String getSocket() {
        return socket;
    }

    @Override
    public void setSocket(String socket) {
        Assert.hasText(socket, "Socket must not be null nor empty!");
        this.socket = socket;
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
        if (!(o instanceof RedisSocketConfiguration)) {
            return false;
        }
        RedisSocketConfiguration that = (RedisSocketConfiguration) o;
        if (database != that.database) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(socket, that.socket)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(username, that.username)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(password, that.password);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(socket);
        result = 31 * result + database;
        result = 31 * result + ObjectUtils.nullSafeHashCode(username);
        result = 31 * result + ObjectUtils.nullSafeHashCode(password);
        return result;
    }
}
