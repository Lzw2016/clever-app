package org.clever.data.redis.connection;

import org.clever.dao.InvalidDataAccessApiUsageException;

/**
 * 在订阅并等待事件的连接上发出命令时抛出异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:39 <br/>
 *
 * @see org.springframework.data.redis.connection.RedisPubSubCommands
 */
public class RedisSubscribedConnectionException extends InvalidDataAccessApiUsageException {
    /**
     * 构造一个新的 <code>RedisSubscribedConnectionException</code> 实例
     */
    public RedisSubscribedConnectionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * 构造一个新的 <code>RedisSubscribedConnectionException</code> 实例
     */
    public RedisSubscribedConnectionException(String msg) {
        super(msg);
    }
}
