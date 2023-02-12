package org.clever.data.redis.connection;

import org.clever.data.redis.ClusterStateFailureException;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link ClusterTopology} 保存有关 {@link RedisClusterNode} 的快照类似信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:26 <br/>
 */
public class ClusterTopology {
    private final Set<RedisClusterNode> nodes;

    /**
     * 创建 {@link ClusterTopology} 的新实例
     *
     * @param nodes 可以是 {@literal null}。
     */
    public ClusterTopology(Set<RedisClusterNode> nodes) {
        this.nodes = nodes != null ? nodes : Collections.emptySet();
    }

    /**
     * 获取所有 {@link RedisClusterNode}
     *
     * @return 从不为 {@literal null}.
     */
    public Set<RedisClusterNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    /**
     * 获取群集中的所有节点（主节点和从节点），其中 {@code link-state} 为 {@literal connected} 并且 {@code flags} 不包含 {@literal fail} 或 {@literal fail？}
     *
     * @return 从不为 {@literal null}.
     */
    public Set<RedisClusterNode> getActiveNodes() {
        Set<RedisClusterNode> activeNodes = new LinkedHashSet<>(nodes.size());
        for (RedisClusterNode node : nodes) {
            if (node.isConnected() && !node.isMarkedAsFail()) {
                activeNodes.add(node);
            }
        }
        return activeNodes;
    }

    /**
     * 获取集群中 {@code link-state} 为 {@literal connected} 且 {@code flags} 不包含 {@literal fail} 或 {@literal fail？} 的所有主节点
     *
     * @return 从不为 {@literal null}.
     */
    public Set<RedisClusterNode> getActiveMasterNodes() {
        Set<RedisClusterNode> activeMasterNodes = new LinkedHashSet<>(nodes.size());
        for (RedisClusterNode node : nodes) {
            if (node.isMaster() && node.isConnected() && !node.isMarkedAsFail()) {
                activeMasterNodes.add(node);
            }
        }
        return activeMasterNodes;
    }

    /**
     * 获取集群中的所有主节点
     *
     * @return 从不为 {@literal null}
     */
    public Set<RedisClusterNode> getMasterNodes() {
        Set<RedisClusterNode> masterNodes = new LinkedHashSet<>(nodes.size());
        for (RedisClusterNode node : nodes) {
            if (node.isMaster()) {
                masterNodes.add(node);
            }
        }
        return masterNodes;
    }

    /**
     * 获取 {@link RedisClusterNode}（主站和从站）服务的特定插槽
     *
     * @return 从不为 {@literal null}
     */
    public Set<RedisClusterNode> getSlotServingNodes(int slot) {
        Set<RedisClusterNode> slotServingNodes = new LinkedHashSet<>(nodes.size());
        for (RedisClusterNode node : nodes) {
            if (node.servesSlot(slot)) {
                slotServingNodes.add(node);
            }
        }
        return slotServingNodes;
    }

    /**
     * 获取 {@link RedisClusterNode}，它是当前为给定密钥提供服务的主节点
     *
     * @param key 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    public RedisClusterNode getKeyServingMasterNode(byte[] key) {
        Assert.notNull(key, "Key for node lookup must not be null!");
        int slot = ClusterSlotHashUtil.calculateSlot(key);
        for (RedisClusterNode node : nodes) {
            if (node.isMaster() && node.servesSlot(slot)) {
                return node;
            }
        }
        throw new ClusterStateFailureException(String.format("Could not find master node serving slot %s for key '%s',", slot, Arrays.toString(key)));
    }

    /**
     * 获取给定 {@literal host} 和 {@literal port} 的 {@link RedisClusterNode} 匹配项
     *
     * @param host 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    public RedisClusterNode lookup(String host, int port) {
        for (RedisClusterNode node : nodes) {
            if (host.equals(node.getHost()) && (node.getPort() != null && port == node.getPort())) {
                return node;
            }
        }
        throw new ClusterStateFailureException(String.format("Could not find node at %s:%s. Is your cluster info up to date?", host, port));
    }

    /**
     * 获取给定 {@literal nodeId} 的 {@link RedisClusterNode} 匹配项
     *
     * @param nodeId 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    public RedisClusterNode lookup(String nodeId) {
        Assert.notNull(nodeId, "NodeId must not be null!");
        for (RedisClusterNode node : nodes) {
            if (nodeId.equals(node.getId())) {
                return node;
            }
        }
        throw new ClusterStateFailureException(String.format("Could not find node at %s. Is your cluster info up to date?", nodeId));
    }

    /**
     * 获取 {@link RedisClusterNode} 匹配匹配 {@link RedisClusterNode#getHost() host} 和 {@link RedisClusterNode#getPort() port} 或 {@link RedisClusterNode#getId() nodeId}
     *
     * @param node 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    public RedisClusterNode lookup(RedisClusterNode node) {
        Assert.notNull(node, "RedisClusterNode must not be null!");
        if (nodes.contains(node) && node.hasValidHost() && StringUtils.hasText(node.getId())) {
            return node;
        }
        if (node.hasValidHost() && node.getPort() != null) {
            return lookup(node.getHost(), node.getPort());
        }
        if (StringUtils.hasText(node.getId())) {
            return lookup(node.getId());
        }
        throw new ClusterStateFailureException(String.format("Could not find node at %s. Have you provided either host and port or the nodeId?", node));
    }

    /**
     * @param key 不得为 {@literal null}
     * @return {@literal null}
     */
    public Set<RedisClusterNode> getKeyServingNodes(byte[] key) {
        Assert.notNull(key, "Key must not be null for Cluster Node lookup.");
        return getSlotServingNodes(ClusterSlotHashUtil.calculateSlot(key));
    }
}
