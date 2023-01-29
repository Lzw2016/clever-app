package org.clever.data.redis.connection;

import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.ScanOptions;

import java.util.Collection;
import java.util.Set;

/**
 * {@link RedisClusterConnection} 允许向集群内的专用节点发送命令。<br/>
 * {@link RedisClusterNode} 可以从 {@link #clusterGetNodes()} 获得，
 * 也可以使用 {@link RedisClusterNode#getHost() host}
 * 和 {@link RedisClusterNode#getPort()} 或 {@link RedisClusterNode#getId() 节点 Id 来构建}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:27 <br/>
 */
public interface RedisClusterConnection extends RedisConnection, RedisClusterCommands, RedisClusterServerCommands {
    /**
     * @param node 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see RedisConnectionCommands#ping()
     */
    String ping(RedisClusterNode node);

    /**
     * @param node    不得为 {@literal null}
     * @param pattern 不得为 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see RedisKeyCommands#keys(byte[])
     */
    Set<byte[]> keys(RedisClusterNode node, byte[] pattern);

    /**
     * 使用 {@link Cursor} 遍历键
     *
     * @param node    不得为 {@literal null}
     * @param options 不得为 {@literal null}
     * @return 从不{@literal null}
     * @see <a href="https://redis.io/commands/scan">Redis 文档：SCAN</a>
     */
    Cursor<byte[]> scan(RedisClusterNode node, ScanOptions options);

    /**
     * @param node 不得为 {@literal null}
     * @return {@literal null} 当没有密钥存储在节点或在管道/事务中使用时
     * @see RedisKeyCommands#randomKey()
     */
    byte[] randomKey(RedisClusterNode node);

    /**
     * 为提供可能附加参数的 {@code key} 执行给定命令。<br />
     * 除了 {@link #execute(String, byte[]...)} 之外，此方法将命令分派给 {@code key} 服务主节点。
     *
     * <pre>{@code
     * // SET foo bar EX 10 NX
     * execute("SET", "foo".getBytes(), asBinaryList("bar", "EX", 10, "NX"))
     * }</pre>
     *
     * @param command 不得为 {@literal null}
     * @param key     不得为 {@literal null}
     * @param args    不得为 {@literal null}
     * @return 由底层 Redis 驱动程序传送的命令结果。可以是 {@literal null}
     */
    <T> T execute(String command, byte[] key, Collection<byte[]> args);

    /**
     * 返回 {@link RedisClusterServerCommands}.
     *
     * @return 从不{@literal null}
     */
    default RedisClusterServerCommands serverCommands() {
        return this;
    }
}
