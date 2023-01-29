package org.clever.data.redis.connection;

import java.util.Collection;

/**
 * Redis Sentinel特定命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:34 <br/>
 */
public interface RedisSentinelCommands {
    /**
     * 强制进行故障转移，就好像 {@literal master} 无法访问一样
     *
     * @param master 不能是 {@literal null}
     */
    void failover(NamedNode master);

    /**
     * 获取受监控主机及其状态的 {@link Collection}
     *
     * @return Collection of {@link RedisServer}s. 从不为 {@literal null}
     */
    Collection<RedisServer> masters();

    /**
     * 显示给定 {@literal master} 的奴隶列表
     *
     * @param master 不能是 {@literal null}
     * @return Collection of {@link RedisServer}s. 从不为 {@literal null}
     */
    Collection<RedisServer> slaves(NamedNode master);

    /**
     * 删除给定的 {@literal master}。服务器将不再被监控，也不再由 {@link #masters()} 返回。
     *
     * @param master 不能是 {@literal null}
     */
    void remove(NamedNode master);

    /**
     * 告诉哨兵开始使用指定的 {@link RedisServer#getName()}、{@link RedisServer#getHost()}、{@link RedisServer#getPort()} 和 {@link RedisServer#getQuorum()} 监视新的 {@literal master}。
     *
     * @param master 不能是 {@literal null}
     */
    void monitor(RedisServer master);
}
