package org.clever.data.redis.connection;

import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:24 <br/>
 */
public interface DefaultedRedisClusterConnection extends RedisClusterConnection, DefaultedRedisConnection {
    @Override
    @Deprecated
    default void bgReWriteAof(RedisClusterNode node) {
        serverCommands().bgReWriteAof(node);
    }

    @Deprecated
    @Override
    default void bgSave(RedisClusterNode node) {
        serverCommands().bgSave(node);
    }

    @Deprecated
    @Override
    default Long lastSave(RedisClusterNode node) {
        return serverCommands().lastSave(node);
    }

    @Deprecated
    @Override
    default void save(RedisClusterNode node) {
        serverCommands().save(node);
    }

    @Deprecated
    @Override
    default Long dbSize(RedisClusterNode node) {
        return serverCommands().dbSize(node);
    }

    @Deprecated
    @Override
    default void flushDb(RedisClusterNode node) {
        serverCommands().flushDb(node);
    }

    @Deprecated
    @Override
    default void flushAll(RedisClusterNode node) {
        serverCommands().flushAll(node);
    }

    @Deprecated
    @Override
    default Properties info(RedisClusterNode node) {
        return serverCommands().info(node);
    }

    @Deprecated
    @Override
    default Properties info(RedisClusterNode node, String section) {
        return serverCommands().info(node, section);
    }

    @Deprecated
    @Override
    default void shutdown(RedisClusterNode node) {
        serverCommands().shutdown(node);
    }

    @Deprecated
    @Override
    default Properties getConfig(RedisClusterNode node, String pattern) {
        return serverCommands().getConfig(node, pattern);
    }

    @Deprecated
    @Override
    default void setConfig(RedisClusterNode node, String param, String value) {
        serverCommands().setConfig(node, param, value);
    }

    @Deprecated
    @Override
    default void resetConfigStats(RedisClusterNode node) {
        serverCommands().resetConfigStats(node);
    }

    @Deprecated
    @Override
    default void rewriteConfig(RedisClusterNode node) {
        serverCommands().rewriteConfig(node);
    }

    @Deprecated
    @Override
    default Long time(RedisClusterNode node) {
        return serverCommands().time(node);
    }

    @Deprecated
    @Override
    default Long time(RedisClusterNode node, TimeUnit timeUnit) {
        return serverCommands().time(node, timeUnit);
    }

    @Deprecated
    @Override
    default List<RedisClientInfo> getClientList(RedisClusterNode node) {
        return serverCommands().getClientList(node);
    }

    @SuppressWarnings("unchecked")
    @Override
    default <T> T execute(String command, byte[] key, Collection<byte[]> args) {
        Assert.notNull(command, "Command must not be null!");
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(args, "Args must not be null!");
        byte[][] commandArgs = new byte[args.size() + 1][];
        commandArgs[0] = key;
        int targetIndex = 1;
        for (byte[] binaryArgument : args) {
            commandArgs[targetIndex++] = binaryArgument;
        }
        return (T) execute(command, commandArgs);
    }
}
