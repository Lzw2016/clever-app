package org.clever.data.redis.core;

import org.clever.data.redis.connection.RedisClusterCommands;
import org.clever.data.redis.connection.RedisClusterNode;
import org.clever.data.redis.connection.RedisClusterNode.SlotRange;
import org.clever.data.redis.connection.RedisConnection;

import java.util.Collection;
import java.util.Set;

/**
 * 集群特定操作的 Redis 操作。
 * {@link RedisClusterNode} 可以从 {@link RedisClusterCommands#clusterGetNodes() a connection} 获得，
 * 也可以使用 {@link RedisClusterNode#getHost() host} 和 {@link RedisClusterNode#getPort()} 或 {@link RedisClusterNode#getId() node Id}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:28 <br/>
 */
public interface ClusterOperations<K, V> {
    /**
     * 获取位于给定节点的所有键
     *
     * @param node 不得为 {@literal null}
     * @return 从不{@literal null}
     * @see RedisConnection#keys(byte[])
     */
    Set<K> keys(RedisClusterNode node, K pattern);

    /**
     * ping 给定的节点
     *
     * @param node 不得为 {@literal null}
     * @see RedisConnection#ping()
     */
    String ping(RedisClusterNode node);

    /**
     * 从给定节点服务的范围中获取随机密钥
     *
     * @param node 不得为 {@literal null}
     * @see RedisConnection#randomKey()
     */
    K randomKey(RedisClusterNode node);

    /**
     * 向给定节点添加插槽
     *
     * @param node  不得为 {@literal null}
     * @param slots 不得为 {@literal null}
     */
    void addSlots(RedisClusterNode node, int... slots);

    /**
     * 将 {@link SlotRange} 中的插槽添加到给定节点
     *
     * @param node  不得为 {@literal null}
     * @param range 不得为 {@literal null}
     */
    void addSlots(RedisClusterNode node, SlotRange range);

    /**
     * 在给定节点上启动 {@literal Append Only File} 重写过程
     *
     * @param node 不得为 {@literal null}
     * @see RedisConnection#bgReWriteAof()
     */
    void bgReWriteAof(RedisClusterNode node);

    /**
     * 在给定节点上开始后台保存数据库
     *
     * @param node 不得为 {@literal null}
     * @see RedisConnection#bgSave()
     */
    void bgSave(RedisClusterNode node);

    /**
     * 将节点添加到集群
     *
     * @param node 不得为 {@literal null}
     */
    void meet(RedisClusterNode node);

    /**
     * 从集群中删除节点
     *
     * @param node 不得为 {@literal null}
     */
    void forget(RedisClusterNode node);

    /**
     * 刷新节点上的数据库
     *
     * @param node 不得为 {@literal null}
     * @see RedisConnection#flushDb()
     */
    void flushDb(RedisClusterNode node);

    /**
     * @param node 不得为 {@literal null}
     */
    Collection<RedisClusterNode> getSlaves(RedisClusterNode node);

    /**
     * 在服务器上同步保存当前数据库快照
     *
     * @param node 不得为 {@literal null}
     * @see RedisConnection#save()
     */
    void save(RedisClusterNode node);

    /**
     * 关闭给定节点
     *
     * @param node 不得为 {@literal null}
     * @see RedisConnection#shutdown()
     */
    void shutdown(RedisClusterNode node);

    /**
     * 将插槽分配从一个源移动到目标节点并复制与插槽关联的键
     *
     * @param source 不得为 {@literal null}
     * @param target 不得为 {@literal null}
     */
    void reshard(RedisClusterNode source, int slot, RedisClusterNode target);
}
