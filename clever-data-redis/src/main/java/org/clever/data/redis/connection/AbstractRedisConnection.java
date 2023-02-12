package org.clever.data.redis.connection;

import org.clever.dao.DataAccessException;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.dao.InvalidDataAccessResourceUsageException;
import org.clever.data.redis.RedisSystemException;
import org.clever.util.Assert;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:17 <br/>
 */
public abstract class AbstractRedisConnection implements DefaultedRedisConnection {
    private RedisSentinelConfiguration sentinelConfiguration;
    private final Map<RedisNode, RedisSentinelConnection> connectionCache = new ConcurrentHashMap<>();

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        if (!hasRedisSentinelConfigured()) {
            throw new InvalidDataAccessResourceUsageException("No sentinels configured.");
        }
        RedisNode node = selectActiveSentinel();
        RedisSentinelConnection connection = connectionCache.get(node);
        if (connection == null || !connection.isOpen()) {
            connection = getSentinelConnection(node);
            connectionCache.putIfAbsent(node, connection);
        }
        return connection;
    }

    public void setSentinelConfiguration(RedisSentinelConfiguration sentinelConfiguration) {
        this.sentinelConfiguration = sentinelConfiguration;
    }

    public boolean hasRedisSentinelConfigured() {
        return this.sentinelConfiguration != null;
    }

    private RedisNode selectActiveSentinel() {
        Assert.state(hasRedisSentinelConfigured(), "Sentinel configuration missing!");
        for (RedisNode node : this.sentinelConfiguration.getSentinels()) {
            if (isActive(node)) {
                return node;
            }
        }
        throw new InvalidDataAccessApiUsageException("Could not find any active sentinels");
    }

    /**
     * 通过发送 ping 检查节点是否处于活动状态
     */
    protected boolean isActive(RedisNode node) {
        return false;
    }

    /**
     * 获取连接到给定节点的 {@link RedisSentinelCommands}
     */
    protected RedisSentinelConnection getSentinelConnection(RedisNode sentinel) {
        throw new UnsupportedOperationException("Sentinel is not supported by this client.");
    }

    @Override
    public void close() throws DataAccessException {
        if (!connectionCache.isEmpty()) {
            for (RedisNode node : connectionCache.keySet()) {
                RedisSentinelConnection connection = connectionCache.remove(node);
                if (connection.isOpen()) {
                    try {
                        connection.close();
                    } catch (IOException e) {
                        throw new RedisSystemException("Failed to close sentinel connection", e);
                    }
                }
            }
        }
    }
}
