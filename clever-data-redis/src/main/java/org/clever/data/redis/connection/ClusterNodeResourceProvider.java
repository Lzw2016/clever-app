package org.clever.data.redis.connection;

import org.clever.dao.DataAccessResourceFailureException;

/**
 * {@link ClusterNodeResourceProvider} 提供对低级客户端 api 的访问，以直接对 Redis 实例执行操作
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:30 <br/>
 */
public interface ClusterNodeResourceProvider {
    /**
     * 获取给定节点的客户端资源
     *
     * @param node 不得为 {@literal null}
     * @return 从不为 {@literal null}
     * @throws DataAccessResourceFailureException 如果集群不知道节点
     */
    <S> S getResourceForSpecificNode(RedisClusterNode node);

    /**
     * 返回给定节点的资源对象。这可能意味着释放资源或将元素返回到池中
     *
     * @param node     不得为 {@literal null}
     * @param resource 不得为 {@literal null}
     */
    void returnResourceForSpecificNode(RedisClusterNode node, Object resource);
}
