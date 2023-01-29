package org.clever.data.redis.connection;

import org.clever.data.redis.core.types.RedisClientInfo;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:29 <br/>
 */
public interface RedisClusterServerCommands extends RedisServerCommands {
    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#bgReWriteAof()
     */
    void bgReWriteAof(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#bgSave()
     */
    void bgSave(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#lastSave()
     */
    Long lastSave(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#save()
     */
    void save(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#dbSize()
     */
    Long dbSize(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#flushDb()
     */
    void flushDb(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#flushAll()
     */
    void flushAll(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#info()
     */
    Properties info(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#info(String)
     */
    Properties info(RedisClusterNode node, String section);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#shutdown()
     */
    void shutdown(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#getConfig(String)
     */
    Properties getConfig(RedisClusterNode node, String pattern);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#setConfig(String, String)
     */
    void setConfig(RedisClusterNode node, String param, String value);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#resetConfigStats()
     */
    void resetConfigStats(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#rewriteConfig()
     */
    void rewriteConfig(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#time()
     */
    default Long time(RedisClusterNode node) {
        return time(node, TimeUnit.MILLISECONDS);
    }

    /**
     * @param node     不能是 {@literal null}
     * @param timeUnit 不能是 {@literal null}
     * @see RedisServerCommands#time(TimeUnit)
     */
    Long time(RedisClusterNode node, TimeUnit timeUnit);

    /**
     * @param node 不能是 {@literal null}
     * @see RedisServerCommands#getClientList()
     */
    List<RedisClientInfo> getClientList(RedisClusterNode node);
}
