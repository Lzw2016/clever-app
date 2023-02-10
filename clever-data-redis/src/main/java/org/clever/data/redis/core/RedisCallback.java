package org.clever.data.redis.core;

import org.clever.dao.DataAccessException;
import org.clever.data.redis.connection.RedisConnection;

/**
 * Redis“低级”代码的回调接口。
 * 与 {@link RedisTemplate} 执行方法一起使用，通常作为方法实现中的匿名类。 通常，用于将多个操作链接在一起
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:26 <br/>
 */
public interface RedisCallback<T> {
    /**
     * 由具有活动 Redis 连接的 {@link RedisTemplate} 调用。
     * 不需要关心激活或关闭连接或处理异常。
     *
     * @param connection 活跃的 Redis 连接
     * @return 结果对象或 {@code null} 如果没有
     */
    T doInRedis(RedisConnection connection) throws DataAccessException;
}
