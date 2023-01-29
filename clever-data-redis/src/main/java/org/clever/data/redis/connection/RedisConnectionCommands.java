package org.clever.data.redis.connection;

/**
 * Redis 支持的特定于连接的命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:21 <br/>
 */
public interface RedisConnectionCommands {
    /**
     * 选择具有给定正 {@code dbIndex} 的数据库
     *
     * @param dbIndex 数据库索引
     * @see <a href="https://redis.io/commands/select">Redis 文档: SELECT</a>
     */
    void select(int dbIndex);

    /**
     * 通过服务器往返返回 {@code message}
     *
     * @param message 要回显的消息
     * @return 消息 或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/echo">Redis 文档: ECHO</a>
     */
    byte[] echo(byte[] message);

    /**
     * 测试连接
     *
     * @return 服务器响应消息 - 通常是 {@literal PONG}。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/ping">Redis 文档: PING</a>
     */
    String ping();
}
