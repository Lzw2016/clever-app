package org.clever.data.redis.connection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Redis 支持的 {@literal cluster} 命令的接口。
 * {@link RedisClusterNode} 可以从 {@link #clusterGetNodes()} 获得，也可以使用 {@link RedisClusterNode#getHost() host} 和 {@link RedisClusterNode#getPort()} 或 {@link RedisClusterNode#getId() 节点 Id 来构建}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:28 <br/>
 */
public interface RedisClusterCommands {
    /**
     * 检索集群节点信息，例如 {@literal id}、{@literal host}、{@literal port} 和 {@literal slots}
     *
     * @return 从不为 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-nodes">Redis 文档: CLUSTER NODES</a>
     */
    Iterable<RedisClusterNode> clusterGetNodes();

    /**
     * 检索有关给定主节点的已连接从属的信息
     *
     * @param master 不能是 {@literal null}
     * @return 从不为 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-slaves">Redis 文档: CLUSTER SLAVES</a>
     */
    Collection<RedisClusterNode> clusterGetSlaves(RedisClusterNode master);

    /**
     * 检索有关主设备及其连接的从属设备的信息
     *
     * @return 从不为 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-slaves">Redis 文档: CLUSTER SLAVES</a>
     */
    Map<RedisClusterNode, Collection<RedisClusterNode>> clusterGetMasterSlaveMap();

    /**
     * 找到给定 {@code key} 的插槽
     *
     * @param key 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-keyslot">Redis 文档: CLUSTER KEYSLOT</a>
     */
    Integer clusterGetSlotForKey(byte[] key);

    /**
     * 找到 {@link RedisClusterNode} 服务给定 {@literal slot}
     */
    RedisClusterNode clusterGetNodeForSlot(int slot);

    /**
     * 找到 {@link RedisClusterNode} 服务给定 {@literal key}
     *
     * @param key 不能是 {@literal null}
     */
    RedisClusterNode clusterGetNodeForKey(byte[] key);

    /**
     * 获取群集信息
     *
     * @see <a href="https://redis.io/commands/cluster-info">Redis 文档: CLUSTER INFO</a>
     */
    ClusterInfo clusterGetClusterInfo();

    /**
     * 将插槽分配给给定的 {@link RedisClusterNode}
     *
     * @param node  不能是 {@literal null}
     * @param slots 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-addslots">Redis 文档: CLUSTER ADDSLOTS</a>
     */
    void clusterAddSlots(RedisClusterNode node, int... slots);

    /**
     * 将 {@link RedisClusterNode.SlotRange#getSlotsArray()} 分配给给定的 {@link RedisClusterNode}
     *
     * @param node  不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-addslots">Redis 文档: CLUSTER ADDSLOTS</a>
     */
    void clusterAddSlots(RedisClusterNode node, RedisClusterNode.SlotRange range);

    /**
     * 计算分配给一个 {@literal slot} 的键数
     *
     * @see <a href="https://redis.io/commands/cluster-countkeysinslot">Redis 文档: CLUSTER COUNTKEYSINSLOT</a>
     */
    Long clusterCountKeysInSlot(int slot);

    /**
     * 从 {@link RedisClusterNode} 中删除插槽
     *
     * @param node  不能是 {@literal null}
     * @param slots 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-delslots">Redis 文档: CLUSTER DELSLOTS</a>
     */
    void clusterDeleteSlots(RedisClusterNode node, int... slots);

    /**
     * 从给定的 {@link RedisClusterNode} 中删除 {@link RedisClusterNode.SlotRange#getSlotsArray()}
     *
     * @param node  不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-delslots">Redis 文档: CLUSTER DELSLOTS</a>
     */
    void clusterDeleteSlotsInRange(RedisClusterNode node, RedisClusterNode.SlotRange range);

    /**
     * 从集群中删除给定的 {@literal node}
     *
     * @param node 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-forget">Redis 文档: CLUSTER FORGET</a>
     */
    void clusterForget(RedisClusterNode node);

    /**
     * 将给定的 {@literal node} 添加到集群
     *
     * @param node 必须包含 {@link RedisClusterNode#getHost() host} 和 {@link RedisClusterNode#getPort()} 并且不能为 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-meet">Redis 文档: CLUSTER MEET</a>
     */
    void clusterMeet(RedisClusterNode node);

    /**
     * @param node 不能是 {@literal null}
     * @param slot 不能是 {@literal null}
     * @param mode 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-setslot">Redis 文档: CLUSTER SETSLOT</a>
     */
    void clusterSetSlot(RedisClusterNode node, int slot, RedisClusterCommands.AddSlots mode);

    /**
     * 获取插槽提供的 {@literal keys}
     *
     * @param slot  不能是 {@literal null}
     * @param count 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-getkeysinslot">Redis 文档: CLUSTER GETKEYSINSLOT</a>
     */
    List<byte[]> clusterGetKeysInSlot(int slot, Integer count);

    /**
     * 将 {@literal slave} 分配给给定的 {@literal master}
     *
     * @param master  不能是 {@literal null}
     * @param replica 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/cluster-replicate">Redis 文档: CLUSTER REPLICATE</a>
     */
    void clusterReplicate(RedisClusterNode master, RedisClusterNode replica);

    enum AddSlots {
        MIGRATING, IMPORTING, STABLE, NODE
    }
}
