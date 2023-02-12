package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.clever.beans.factory.DisposableBean;
import org.clever.dao.DataAccessException;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.data.redis.ExceptionTranslationStrategy;
import org.clever.data.redis.PassThroughExceptionTranslationStrategy;
import org.clever.data.redis.connection.*;
import org.clever.data.redis.connection.ClusterCommandExecutor.ClusterCommandCallback;
import org.clever.data.redis.connection.ClusterCommandExecutor.MultiKeyClusterCommandCallback;
import org.clever.data.redis.connection.ClusterCommandExecutor.NodeResult;
import org.clever.data.redis.connection.RedisClusterNode.SlotRange;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.ScanOptions;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

/**
 * {@code RedisClusterConnection} 在 <a href="https://github.com/mp911de/lettuce">Lettuce</a> Redis 客户端之上实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:23 <br/>
 */
public class LettuceClusterConnection extends LettuceConnection implements DefaultedRedisClusterConnection {
    static final ExceptionTranslationStrategy exceptionConverter = new PassThroughExceptionTranslationStrategy(new LettuceExceptionConverter());

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ClusterCommandExecutor clusterCommandExecutor;
    private final ClusterTopologyProvider topologyProvider;
    private final boolean disposeClusterCommandExecutorOnClose;

    /**
     * 使用具有默认 {@link RedisURI#DEFAULT_TIMEOUT_DURATION 超时} 的 {@link RedisClusterClient}
     * 创建新的 {@link LettuceClusterConnection} 和一个在关闭时被销毁的新 {@link ClusterCommandExecutor}
     *
     * @param clusterClient 不得为 {@literal null}
     */
    public LettuceClusterConnection(RedisClusterClient clusterClient) {
        this(new ClusterConnectionProvider(clusterClient, CODEC));
    }

    /**
     * 使用 {@link RedisClusterClient} 通过给定的 {@link ClusterCommandExecutor} 在集群中运行命令，
     * 使用默认 {@link RedisURI#DEFAULT_TIMEOUT_DURATION 超时} 创建新的 {@link LettuceClusterConnection}。
     *
     * @param clusterClient 不得为 {@literal null}
     * @param executor      不得为 {@literal null}
     */
    public LettuceClusterConnection(RedisClusterClient clusterClient, ClusterCommandExecutor executor) {
        this(clusterClient, executor, RedisURI.DEFAULT_TIMEOUT_DURATION);
    }

    /**
     * 通过给定的 {@link ClusterCommandExecutor} 使用 {@link RedisClusterClient} 在集群中运行命令，
     * 使用给定的命令 {@code timeout} 创建新的 {@link LettuceClusterConnection}
     *
     * @param clusterClient 不得为 {@literal null}
     * @param timeout       不得为 {@literal null}
     * @param executor      不得为 {@literal null}
     */
    public LettuceClusterConnection(RedisClusterClient clusterClient, ClusterCommandExecutor executor, Duration timeout) {
        this(new ClusterConnectionProvider(clusterClient, CODEC), executor, timeout);
    }

    /**
     * 通过给定的 {@link ClusterCommandExecutor} 使用 {@link LettuceConnectionProvider} 在集群中运行命令创建新的 {@link LettuceClusterConnection}
     *
     * @param connectionProvider 不得为 {@literal null}
     */
    public LettuceClusterConnection(LettuceConnectionProvider connectionProvider) {
        super(null, connectionProvider, RedisURI.DEFAULT_TIMEOUT_DURATION.toMillis(), 0);
        Assert.isTrue(connectionProvider instanceof ClusterConnectionProvider, "LettuceConnectionProvider must be a ClusterConnectionProvider.");
        this.topologyProvider = new LettuceClusterTopologyProvider(getClient());
        this.clusterCommandExecutor = new ClusterCommandExecutor(
                this.topologyProvider,
                new LettuceClusterNodeResourceProvider(getConnectionProvider()),
                exceptionConverter
        );
        this.disposeClusterCommandExecutorOnClose = true;
    }

    /**
     * 通过给定的 {@link ClusterCommandExecutor} 使用 {@link LettuceConnectionProvider} 在集群中运行命令创建新的 {@link LettuceClusterConnection}
     *
     * @param connectionProvider 不得为 {@literal null}
     * @param executor           不得为 {@literal null}
     */
    public LettuceClusterConnection(LettuceConnectionProvider connectionProvider, ClusterCommandExecutor executor) {
        this(connectionProvider, executor, RedisURI.DEFAULT_TIMEOUT_DURATION);
    }

    /**
     * 通过给定的 {@link ClusterCommandExecutor} 使用 {@link LettuceConnectionProvider} 在集群中运行命令创建新的 {@link LettuceClusterConnection}
     *
     * @param connectionProvider 不得为 {@literal null}
     * @param executor           不得为 {@literal null}
     * @param timeout            不得为 {@literal null}
     */
    public LettuceClusterConnection(LettuceConnectionProvider connectionProvider, ClusterCommandExecutor executor, Duration timeout) {
        super(null, connectionProvider, timeout.toMillis(), 0);
        Assert.notNull(executor, "ClusterCommandExecutor must not be null.");
        Assert.isTrue(connectionProvider instanceof ClusterConnectionProvider, "LettuceConnectionProvider must be a ClusterConnectionProvider.");
        this.topologyProvider = new LettuceClusterTopologyProvider(getClient());
        this.clusterCommandExecutor = executor;
        this.disposeClusterCommandExecutorOnClose = false;
    }

    /**
     * 在给定共享 {@link StatefulRedisClusterConnection} 和 {@link LettuceConnectionProvider} 通过给定的 {@link ClusterCommandExecutor} 在集群中运行命令的情况下，
     * 创建新的 {@link LettuceClusterConnection}
     *
     * @param sharedConnection        如果没有使用共享连接，则可能是 {@literal null}
     * @param connectionProvider      不得为 {@literal null}
     * @param clusterTopologyProvider 不得为 {@literal null}
     * @param executor                不得为 {@literal null}
     * @param timeout                 不得为 {@literal null}
     */
    protected LettuceClusterConnection(StatefulRedisClusterConnection<byte[], byte[]> sharedConnection,
                                       LettuceConnectionProvider connectionProvider,
                                       ClusterTopologyProvider clusterTopologyProvider,
                                       ClusterCommandExecutor executor,
                                       Duration timeout) {
        super(sharedConnection, connectionProvider, timeout.toMillis(), 0);
        Assert.notNull(executor, "ClusterCommandExecutor must not be null.");
        this.topologyProvider = clusterTopologyProvider;
        this.clusterCommandExecutor = executor;
        this.disposeClusterCommandExecutorOnClose = false;
    }

    /**
     * @return 访问 {@link RedisClusterClient} 进行非连接访问
     */
    private RedisClusterClient getClient() {
        LettuceConnectionProvider connectionProvider = getConnectionProvider();
        if (connectionProvider instanceof RedisClientProvider) {
            return (RedisClusterClient) ((RedisClientProvider) getConnectionProvider()).getRedisClient();
        }
        throw new IllegalStateException(String.format("Connection provider %s does not implement RedisClientProvider!", connectionProvider.getClass().getName()));
    }

    @Override
    public RedisGeoCommands geoCommands() {
        return new LettuceClusterGeoCommands(this);
    }

    @Override
    public RedisHashCommands hashCommands() {
        return new LettuceClusterHashCommands(this);
    }

    @Override
    public RedisHyperLogLogCommands hyperLogLogCommands() {
        return new LettuceClusterHyperLogLogCommands(this);
    }

    @Override
    public RedisKeyCommands keyCommands() {
        return doGetClusterKeyCommands();
    }

    private LettuceClusterKeyCommands doGetClusterKeyCommands() {
        return new LettuceClusterKeyCommands(this);
    }

    @Override
    public RedisListCommands listCommands() {
        return new LettuceClusterListCommands(this);
    }

    @Override
    public RedisStringCommands stringCommands() {
        return new LettuceClusterStringCommands(this);
    }

    @Override
    public RedisSetCommands setCommands() {
        return new LettuceClusterSetCommands(this);
    }

    @Override
    public RedisZSetCommands zSetCommands() {
        return new LettuceClusterZSetCommands(this);
    }

    @Override
    public RedisClusterServerCommands serverCommands() {
        return new LettuceClusterServerCommands(this);
    }

    @Override
    public String ping() {
        Collection<String> ping = clusterCommandExecutor.executeCommandOnAllNodes(
                (LettuceClusterCommandCallback<String>) BaseRedisCommands::ping
        ).resultsAsList();
        for (String result : ping) {
            if (!ObjectUtils.nullSafeEquals("PONG", result)) {
                return "";
            }
        }
        return "PONG";
    }

    @Override
    public String ping(RedisClusterNode node) {
        return clusterCommandExecutor.executeCommandOnSingleNode(
                (LettuceClusterCommandCallback<String>) BaseRedisCommands::ping, node
        ).getValue();
    }

    @Override
    public List<RedisClusterNode> clusterGetNodes() {
        return new ArrayList<>(topologyProvider.getTopology().getNodes());
    }

    @Override
    public Set<RedisClusterNode> clusterGetSlaves(RedisClusterNode master) {
        Assert.notNull(master, "Master must not be null!");
        RedisClusterNode nodeToUse = topologyProvider.getTopology().lookup(master);
        return clusterCommandExecutor.executeCommandOnSingleNode(
                (LettuceClusterCommandCallback<Set<RedisClusterNode>>) client -> LettuceConverters.toSetOfRedisClusterNodes(client.clusterReplicas(nodeToUse.getId())),
                master
        ).getValue();
    }

    @Override
    public Map<RedisClusterNode, Collection<RedisClusterNode>> clusterGetMasterSlaveMap() {
        List<NodeResult<Collection<RedisClusterNode>>> nodeResults = clusterCommandExecutor.executeCommandAsyncOnNodes(
                (LettuceClusterCommandCallback<Collection<RedisClusterNode>>) client -> Converters.toSetOfRedisClusterNodes(client.clusterReplicas(client.clusterMyId())),
                topologyProvider.getTopology().getActiveMasterNodes()
        ).getResults();
        Map<RedisClusterNode, Collection<RedisClusterNode>> result = new LinkedHashMap<>();
        for (NodeResult<Collection<RedisClusterNode>> nodeResult : nodeResults) {
            result.put(nodeResult.getNode(), nodeResult.getValue());
        }
        return result;
    }

    @Override
    public Integer clusterGetSlotForKey(byte[] key) {
        return SlotHash.getSlot(key);
    }

    @Override
    public RedisClusterNode clusterGetNodeForSlot(int slot) {
        Set<RedisClusterNode> nodes = topologyProvider.getTopology().getSlotServingNodes(slot);
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.iterator().next();
    }

    @Override
    public RedisClusterNode clusterGetNodeForKey(byte[] key) {
        return clusterGetNodeForSlot(clusterGetSlotForKey(key));
    }

    @Override
    public ClusterInfo clusterGetClusterInfo() {
        return clusterCommandExecutor.executeCommandOnArbitraryNode(
                (LettuceClusterCommandCallback<ClusterInfo>) client -> new ClusterInfo(LettuceConverters.toProperties(client.clusterInfo()))
        ).getValue();
    }

    @Override
    public void clusterAddSlots(RedisClusterNode node, int... slots) {
        clusterCommandExecutor.executeCommandOnSingleNode(
                (LettuceClusterCommandCallback<String>) client -> client.clusterAddSlots(slots), node
        );
    }

    @Override
    public void clusterAddSlots(RedisClusterNode node, SlotRange range) {
        Assert.notNull(range, "Range must not be null.");
        clusterAddSlots(node, range.getSlotsArray());
    }

    @Override
    public Long clusterCountKeysInSlot(int slot) {
        try {
            return getConnection().clusterCountKeysInSlot(slot);
        } catch (Exception ex) {
            throw exceptionConverter.translate(ex);
        }
    }

    @Override
    public void clusterDeleteSlots(RedisClusterNode node, int... slots) {
        clusterCommandExecutor.executeCommandOnSingleNode(
                (LettuceClusterCommandCallback<String>) client -> client.clusterDelSlots(slots), node
        );
    }

    @Override
    public void clusterDeleteSlotsInRange(RedisClusterNode node, SlotRange range) {
        Assert.notNull(range, "Range must not be null.");
        clusterDeleteSlots(node, range.getSlotsArray());
    }

    @Override
    public void clusterForget(RedisClusterNode node) {
        List<RedisClusterNode> nodes = new ArrayList<>(clusterGetNodes());
        RedisClusterNode nodeToRemove = topologyProvider.getTopology().lookup(node);
        nodes.remove(nodeToRemove);
        this.clusterCommandExecutor.executeCommandAsyncOnNodes(
                (LettuceClusterCommandCallback<String>) client -> client.clusterForget(nodeToRemove.getId()), nodes
        );
    }

    @Override
    public void clusterMeet(RedisClusterNode node) {
        Assert.notNull(node, "Cluster node must not be null for CLUSTER MEET command!");
        Assert.hasText(node.getHost(), "Node to meet cluster must have a host!");
        Assert.isTrue(node.getPort() > 0, "Node to meet cluster must have a port greater 0!");
        this.clusterCommandExecutor.executeCommandOnAllNodes(
                (LettuceClusterCommandCallback<String>) client -> client.clusterMeet(node.getHost(), node.getPort())
        );
    }

    @Override
    public void clusterSetSlot(RedisClusterNode node, int slot, AddSlots mode) {
        Assert.notNull(node, "Node must not be null.");
        Assert.notNull(mode, "AddSlots mode must not be null.");
        RedisClusterNode nodeToUse = topologyProvider.getTopology().lookup(node);
        String nodeId = nodeToUse.getId();
        clusterCommandExecutor.executeCommandOnSingleNode((LettuceClusterCommandCallback<String>) client -> {
            switch (mode) {
                case MIGRATING:
                    return client.clusterSetSlotMigrating(slot, nodeId);
                case IMPORTING:
                    return client.clusterSetSlotImporting(slot, nodeId);
                case NODE:
                    return client.clusterSetSlotNode(slot, nodeId);
                case STABLE:
                    return client.clusterSetSlotStable(slot);
                default:
                    throw new InvalidDataAccessApiUsageException("Invalid import mode for cluster slot: " + slot);
            }
        }, node);
    }

    @Override
    public List<byte[]> clusterGetKeysInSlot(int slot, Integer count) {
        try {
            return getConnection().clusterGetKeysInSlot(slot, count);
        } catch (Exception ex) {
            throw exceptionConverter.translate(ex);
        }
    }

    @Override
    public void clusterReplicate(RedisClusterNode master, RedisClusterNode replica) {
        RedisClusterNode masterNode = topologyProvider.getTopology().lookup(master);
        clusterCommandExecutor.executeCommandOnSingleNode(
                (LettuceClusterCommandCallback<String>) client -> client.clusterReplicate(masterNode.getId()), replica
        );
    }

    @Override
    public Set<byte[]> keys(RedisClusterNode node, byte[] pattern) {
        return doGetClusterKeyCommands().keys(node, pattern);
    }

    @Override
    public Cursor<byte[]> scan(RedisClusterNode node, ScanOptions options) {
        return doGetClusterKeyCommands().scan(node, options);
    }

    public byte[] randomKey(RedisClusterNode node) {
        return doGetClusterKeyCommands().randomKey(node);
    }

    @Override
    public void select(int dbIndex) {
        if (dbIndex != 0) {
            throw new InvalidDataAccessApiUsageException("Cannot SELECT non zero index in cluster mode.");
        }
    }

    // --> cluster node stuff

    @Override
    public void watch(byte[]... keys) {
        throw new InvalidDataAccessApiUsageException("WATCH is currently not supported in cluster mode.");
    }

    @Override
    public void unwatch() {
        throw new InvalidDataAccessApiUsageException("UNWATCH is currently not supported in cluster mode.");
    }

    @Override
    public void multi() {
        throw new InvalidDataAccessApiUsageException("MULTI is currently not supported in cluster mode.");
    }

    public ClusterCommandExecutor getClusterCommandExecutor() {
        return clusterCommandExecutor;
    }

    @Override
    public void close() throws DataAccessException {
        if (!isClosed() && disposeClusterCommandExecutorOnClose) {
            try {
                clusterCommandExecutor.destroy();
            } catch (Exception ex) {
                log.warn("Cannot properly close cluster command executor", ex);
            }
        }
        super.close();
    }

    /**
     * {@link ClusterCommandCallback} 的Lettuce具体实现
     */
    protected interface LettuceClusterCommandCallback<T> extends ClusterCommandCallback<RedisClusterCommands<byte[], byte[]>, T> {
    }

    /**
     * {@link MultiKeyClusterCommandCallback} 的Lettuce具体实现
     */
    protected interface LettuceMultiKeyClusterCommandCallback<T> extends MultiKeyClusterCommandCallback<RedisClusterCommands<byte[], byte[]>, T> {
    }

    /**
     * {@link ClusterNodeResourceProvider} 的Lettuce具体实现
     */
    static class LettuceClusterNodeResourceProvider implements ClusterNodeResourceProvider, DisposableBean {
        private final LettuceConnectionProvider connectionProvider;
        private volatile StatefulRedisClusterConnection<byte[], byte[]> connection;

        LettuceClusterNodeResourceProvider(LettuceConnectionProvider connectionProvider) {
            this.connectionProvider = connectionProvider;
        }

        @SuppressWarnings("unchecked")
        @Override
        public RedisClusterCommands<byte[], byte[]> getResourceForSpecificNode(RedisClusterNode node) {
            Assert.notNull(node, "Node must not be null!");
            if (connection == null) {
                synchronized (this) {
                    if (connection == null) {
                        this.connection = connectionProvider.getConnection(StatefulRedisClusterConnection.class);
                    }
                }
            }
            return connection.getConnection(node.getHost(), node.getPort()).sync();
        }

        @Override
        public void returnResourceForSpecificNode(RedisClusterNode node, Object resource) {
        }

        @Override
        public void destroy() throws Exception {
            if (connection != null) {
                connectionProvider.release(connection);
            }
        }
    }
}
