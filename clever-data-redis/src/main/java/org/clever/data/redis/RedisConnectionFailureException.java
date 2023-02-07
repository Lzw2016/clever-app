package org.clever.data.redis;

import org.clever.dao.DataAccessResourceFailureException;

/**
 * 当 Redis 连接完全失败时抛出致命异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:42 <br/>
 */
public class RedisConnectionFailureException extends DataAccessResourceFailureException {
    public RedisConnectionFailureException(String msg) {
        super(msg);
    }

    public RedisConnectionFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
