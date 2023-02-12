package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.AbstractRedisClient;

/**
 * 扩展到 {@link LettuceConnectionProvider} 以提供者公开 {@link io.lettuce.core.RedisClient}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:17 <br/>
 */
interface RedisClientProvider {
    /**
     * 返回底层的 Redis 客户端
     *
     * @return {@link AbstractRedisClient}。永远不要{@literal null}
     */
    AbstractRedisClient getRedisClient();
}
