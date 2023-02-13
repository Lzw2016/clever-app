package org.clever.data.redis.connection;

import org.clever.beans.factory.DisposableBean;

/**
 * Redis 连接的线程安全工厂
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 16:42 <br/>
 */
public interface RedisConnectionFactory extends DisposableBean {
    /**
     * 为与 Redis 交互提供合适的连接
     */
    RedisConnection getConnection();

    /**
     * 为与 Redis 集群交互提供合适的连接
     */
    RedisClusterConnection getClusterConnection();

    /**
     * 指定是否应将流水线结果转换为预期的数据类型。如果为 false，{@link RedisConnection#closePipeline()} 和 {@link RedisConnection#exec()} 的结果将是底层驱动程序返回的类型。
     * 此方法主要是为了向后兼容 1.0。
     * 允许转换和反序列化结果通常总是一个好主意。事实上，这是现在的默认行为。
     *
     * @return 是否转换管道和事务结果
     */
    boolean getConvertPipelineAndTxResults();

    /**
     * 为与 Redis Sentinel 交互提供合适的连接
     */
    RedisSentinelConnection getSentinelConnection();
}
