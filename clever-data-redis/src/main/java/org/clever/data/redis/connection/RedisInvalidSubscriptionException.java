package org.clever.data.redis.connection;

import org.clever.core.NestedRuntimeException;

/**
 * 订阅expired/dead {@link Subscription} 时引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:39 <br/>
 */
public class RedisInvalidSubscriptionException extends NestedRuntimeException {
    /**
     * 构造一个新的<code>RedisInvalidSubscriptionException<code/>实例
     */
    public RedisInvalidSubscriptionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * 构造一个新的<code>RedisInvalidSubscriptionException<code/>实例
     */
    public RedisInvalidSubscriptionException(String msg) {
        super(msg);
    }
}
