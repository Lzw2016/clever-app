package org.clever.data.redis.connection;

import org.clever.beans.factory.DisposableBean;
import org.clever.core.thread.SharedThreadPoolExecutor;
import org.clever.dao.DataAccessException;
import org.clever.data.redis.ClusterRedirectException;
import org.clever.data.redis.ClusterStateFailureException;
import org.clever.data.redis.ExceptionTranslationStrategy;
import org.clever.data.redis.TooManyClusterRedirectionsException;
import org.clever.data.redis.connection.util.ByteArraySet;
import org.clever.data.redis.connection.util.ByteArrayWrapper;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;
import org.clever.util.ObjectUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link ClusterCommandExecutor} 负责跨已知群集节点运行命令。通过提供 {@link ExecutorService}，可以影响执行行为。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:15 <br/>
 */
public class ClusterCommandExecutor implements DisposableBean {
    private final ExecutorService executor;
    private final ClusterTopologyProvider topologyProvider;
    private final ClusterNodeResourceProvider resourceProvider;
    private final ExceptionTranslationStrategy exceptionTranslationStrategy;
    private int maxRedirects = 5;

    /**
     * 创建 {@link ClusterCommandExecutor} 的新实例
     *
     * @param topologyProvider     不得为 {@literal null}
     * @param resourceProvider     不得为 {@literal null}
     * @param exceptionTranslation 不得为 {@literal null}
     */
    public ClusterCommandExecutor(ClusterTopologyProvider topologyProvider,
                                  ClusterNodeResourceProvider resourceProvider,
                                  ExceptionTranslationStrategy exceptionTranslation) {
        Assert.notNull(topologyProvider, "ClusterTopologyProvider must not be null!");
        Assert.notNull(resourceProvider, "ClusterNodeResourceProvider must not be null!");
        Assert.notNull(exceptionTranslation, "ExceptionTranslationStrategy must not be null!");
        this.topologyProvider = topologyProvider;
        this.resourceProvider = resourceProvider;
        this.exceptionTranslationStrategy = exceptionTranslation;
        this.executor = SharedThreadPoolExecutor.getSmall();
    }

    /**
     * @param topologyProvider     不得为 {@literal null}
     * @param resourceProvider     不得为 {@literal null}
     * @param exceptionTranslation 不得为 {@literal null}
     * @param executor             可以是 {@literal null}。 默认为 {@link ThreadPoolExecutor}
     */
    public ClusterCommandExecutor(ClusterTopologyProvider topologyProvider,
                                  ClusterNodeResourceProvider resourceProvider,
                                  ExceptionTranslationStrategy exceptionTranslation,
                                  ExecutorService executor) {
        Assert.notNull(topologyProvider, "ClusterTopologyProvider must not be null!");
        Assert.notNull(resourceProvider, "ClusterNodeResourceProvider must not be null!");
        Assert.notNull(exceptionTranslation, "ExceptionTranslationStrategy must not be null!");
        this.topologyProvider = topologyProvider;
        this.resourceProvider = resourceProvider;
        this.exceptionTranslationStrategy = exceptionTranslation;
        if (executor == null) {
            executor = SharedThreadPoolExecutor.getSmall();
        }
        this.executor = executor;
    }

    /**
     * 在随机节点上运行 {@link ClusterCommandCallback}
     *
     * @param cmd 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    public <T> NodeResult<T> executeCommandOnArbitraryNode(ClusterCommandCallback<?, T> cmd) {
        Assert.notNull(cmd, "ClusterCommandCallback must not be null!");
        List<RedisClusterNode> nodes = new ArrayList<>(getClusterTopology().getActiveNodes());
        return executeCommandOnSingleNode(cmd, nodes.get(new Random().nextInt(nodes.size())));
    }

    /**
     * 在给定的 {@link RedisClusterNode} 上运行 {@link ClusterCommandCallback}
     *
     * @param cmd  不得为 {@literal null}
     * @param node 不得为 {@literal null}
     * @throws IllegalArgumentException 如果无法为给定节点获取资源
     */
    public <S, T> NodeResult<T> executeCommandOnSingleNode(ClusterCommandCallback<S, T> cmd, RedisClusterNode node) {
        return executeCommandOnSingleNode(cmd, node, 0);
    }

    private <S, T> NodeResult<T> executeCommandOnSingleNode(ClusterCommandCallback<S, T> cmd, RedisClusterNode node, int redirectCount) {
        Assert.notNull(cmd, "ClusterCommandCallback must not be null!");
        Assert.notNull(node, "RedisClusterNode must not be null!");
        if (redirectCount > maxRedirects) {
            throw new TooManyClusterRedirectionsException(String.format(
                    "Cannot follow Cluster Redirects over more than %s legs. Please consider increasing the number of redirects to follow. Current value is: %s.",
                    redirectCount,
                    maxRedirects
            ));
        }
        RedisClusterNode nodeToUse = lookupNode(node);
        S client = this.resourceProvider.getResourceForSpecificNode(nodeToUse);
        Assert.notNull(client, "Could not acquire resource for node. Is your cluster info up to date?");
        try {
            return new NodeResult<>(node, cmd.doInCluster(client));
        } catch (RuntimeException ex) {
            RuntimeException translatedException = convertToDataAccessException(ex);
            if (translatedException instanceof ClusterRedirectException) {
                ClusterRedirectException cre = (ClusterRedirectException) translatedException;
                return executeCommandOnSingleNode(
                        cmd,
                        topologyProvider.getTopology().lookup(cre.getTargetHost(), cre.getTargetPort()),
                        redirectCount + 1
                );
            } else {
                throw translatedException != null ? translatedException : ex;
            }
        } finally {
            this.resourceProvider.returnResourceForSpecificNode(nodeToUse, client);
        }
    }

    /**
     * 从拓扑中查找节点
     *
     * @param node 不得为 {@literal null}
     * @return 从不为 {@literal null}
     * @throws IllegalArgumentException 如果无法将节点解析为拓扑已知节点
     */
    private RedisClusterNode lookupNode(RedisClusterNode node) {
        try {
            return topologyProvider.getTopology().lookup(node);
        } catch (ClusterStateFailureException e) {
            throw new IllegalArgumentException(String.format("Node %s is unknown to cluster", node), e);
        }
    }

    /**
     * 在所有可访问的主节点上运行 {@link ClusterCommandCallback}
     *
     * @param cmd 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    public <S, T> MultiNodeResult<T> executeCommandOnAllNodes(final ClusterCommandCallback<S, T> cmd) {
        return executeCommandAsyncOnNodes(cmd, getClusterTopology().getActiveMasterNodes());
    }

    /**
     * @param callback 不得为 {@literal null}
     * @param nodes    不得为 {@literal null}
     * @return 从不为 {@literal null}
     * @throws IllegalArgumentException 如果无法将节点解析为拓扑已知节点
     */
    public <S, T> MultiNodeResult<T> executeCommandAsyncOnNodes(ClusterCommandCallback<S, T> callback, Iterable<RedisClusterNode> nodes) {
        Assert.notNull(callback, "Callback must not be null!");
        Assert.notNull(nodes, "Nodes must not be null!");
        List<RedisClusterNode> resolvedRedisClusterNodes = new ArrayList<>();
        ClusterTopology topology = topologyProvider.getTopology();
        for (RedisClusterNode node : nodes) {
            try {
                resolvedRedisClusterNodes.add(topology.lookup(node));
            } catch (ClusterStateFailureException e) {
                throw new IllegalArgumentException(String.format("Node %s is unknown to cluster", node), e);
            }
        }
        Map<NodeExecution, Future<NodeResult<T>>> futures = new LinkedHashMap<>();
        for (RedisClusterNode node : resolvedRedisClusterNodes) {
            futures.put(new NodeExecution(node), executor.submit(() -> executeCommandOnSingleNode(callback, node)));
        }
        return collectResults(futures);
    }

    private <T> MultiNodeResult<T> collectResults(Map<NodeExecution, Future<NodeResult<T>>> futures) {
        boolean done = false;
        MultiNodeResult<T> result = new MultiNodeResult<>();
        Map<RedisClusterNode, Throwable> exceptions = new HashMap<>();
        Set<String> saveGuard = new HashSet<>();
        while (!done) {
            done = true;
            for (Map.Entry<NodeExecution, Future<NodeResult<T>>> entry : futures.entrySet()) {
                if (!entry.getValue().isDone() && !entry.getValue().isCancelled()) {
                    done = false;
                } else {
                    NodeExecution execution = entry.getKey();
                    try {
                        String futureId = ObjectUtils.getIdentityHexString(entry.getValue());
                        if (!saveGuard.contains(futureId)) {
                            if (execution.isPositional()) {
                                result.add(execution.getPositionalKey(), entry.getValue().get());
                            } else {
                                result.add(entry.getValue().get());
                            }
                            saveGuard.add(futureId);
                        }
                    } catch (ExecutionException e) {
                        RuntimeException ex = convertToDataAccessException((Exception) e.getCause());
                        exceptions.put(execution.getNode(), ex != null ? ex : e.getCause());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        RuntimeException ex = convertToDataAccessException((Exception) e.getCause());
                        exceptions.put(execution.getNode(), ex != null ? ex : e.getCause());
                        break;
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                done = true;
                Thread.currentThread().interrupt();
            }
        }
        if (!exceptions.isEmpty()) {
            throw new ClusterCommandExecutionFailureException(new ArrayList<>(exceptions.values()));
        }
        return result;
    }

    /**
     * 在一组提供一个或多个密钥的精选节点上运行 {@link MultiKeyClusterCommandCallback}
     *
     * @param cmd 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    public <S, T> MultiNodeResult<T> executeMultiKeyCommand(MultiKeyClusterCommandCallback<S, T> cmd, Iterable<byte[]> keys) {
        Map<RedisClusterNode, PositionalKeys> nodeKeyMap = new HashMap<>();
        int index = 0;
        for (byte[] key : keys) {
            for (RedisClusterNode node : getClusterTopology().getKeyServingNodes(key)) {
                nodeKeyMap.computeIfAbsent(node, val -> PositionalKeys.empty()).append(PositionalKey.of(key, index++));
            }
        }
        Map<NodeExecution, Future<NodeResult<T>>> futures = new LinkedHashMap<>();
        for (Entry<RedisClusterNode, PositionalKeys> entry : nodeKeyMap.entrySet()) {
            if (entry.getKey().isMaster()) {
                for (PositionalKey key : entry.getValue()) {
                    futures.put(
                            new NodeExecution(entry.getKey(), key),
                            executor.submit(() -> executeMultiKeyCommandOnSingleNode(cmd, entry.getKey(), key.getBytes()))
                    );
                }
            }
        }
        return collectResults(futures);
    }

    private <S, T> NodeResult<T> executeMultiKeyCommandOnSingleNode(MultiKeyClusterCommandCallback<S, T> cmd, RedisClusterNode node, byte[] key) {
        Assert.notNull(cmd, "MultiKeyCommandCallback must not be null!");
        Assert.notNull(node, "RedisClusterNode must not be null!");
        Assert.notNull(key, "Keys for execution must not be null!");
        S client = this.resourceProvider.getResourceForSpecificNode(node);
        Assert.notNull(client, "Could not acquire resource for node. Is your cluster info up to date?");
        try {
            return new NodeResult<>(node, cmd.doInCluster(client, key), key);
        } catch (RuntimeException ex) {
            RuntimeException translatedException = convertToDataAccessException(ex);
            throw translatedException != null ? translatedException : ex;
        } finally {
            this.resourceProvider.returnResourceForSpecificNode(node, client);
        }
    }

    private ClusterTopology getClusterTopology() {
        return this.topologyProvider.getTopology();
    }

    private DataAccessException convertToDataAccessException(Exception e) {
        return exceptionTranslationStrategy.translate(e);
    }

    /**
     * 设置重定向的最大数量以遵循 {@code MOVED} 或 {@code ASK}
     *
     * @param maxRedirects 设置为零以暂停重定向
     */
    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    @Override
    public void destroy() throws Exception {
        if (executor instanceof DisposableBean) {
            ((DisposableBean) executor).destroy();
        }
        if (resourceProvider instanceof DisposableBean) {
            ((DisposableBean) resourceProvider).destroy();
        }
    }

    /**
     * 直接使用集群客户端的 Redis“低级”代码的回调接口。与 {@link ClusterCommandExecutor} 执行方法一起使用
     *
     * @param <T> 本机驱动程序连接
     */
    public interface ClusterCommandCallback<T, S> {
        S doInCluster(T client);
    }

    /**
     * 使用集群客户端执行多键命令的 Redis“低级”代码的回调接口
     *
     * @param <T> 本机驱动程序连接
     */
    public interface MultiKeyClusterCommandCallback<T, S> {
        S doInCluster(T client, byte[] key);
    }

    /**
     * {@link NodeExecution} 封装了命令在特定节点上的执行以及所涉及的参数，例如 key
     */
    private static class NodeExecution {
        private final RedisClusterNode node;
        private final PositionalKey positionalKey;

        NodeExecution(RedisClusterNode node) {
            this(node, null);
        }

        NodeExecution(RedisClusterNode node, PositionalKey positionalKey) {
            this.node = node;
            this.positionalKey = positionalKey;
        }

        /**
         * 获取执行发生的 {@link RedisClusterNode}
         */
        RedisClusterNode getNode() {
            return node;
        }

        /**
         * 获取本次执行的 {@link PositionalKey}
         */
        PositionalKey getPositionalKey() {
            return positionalKey;
        }

        boolean isPositional() {
            return positionalKey != null;
        }
    }

    /**
     * {@link NodeResult} 封装了给定 {@link RedisClusterNode} 上的 {@link ClusterCommandCallback} 返回的实际值
     */
    public static class NodeResult<T> {
        private final RedisClusterNode node;
        private final T value;
        private final ByteArrayWrapper key;

        /**
         * 创建新的 {@link NodeResult}
         *
         * @param node  不得为 {@literal null}
         * @param value 可以是 {@literal null}。
         */
        public NodeResult(RedisClusterNode node, T value) {
            this(node, value, new byte[]{});
        }

        /**
         * 创建新的 {@link NodeResult}
         *
         * @param node  不得为 {@literal null}
         * @param value 可以是 {@literal null}。
         * @param key   不得为 {@literal null}
         */
        public NodeResult(RedisClusterNode node, T value, byte[] key) {
            this.node = node;
            this.value = value;
            this.key = new ByteArrayWrapper(key);
        }

        /**
         * 获取命令执行的实际值
         *
         * @return 可以是 {@literal null}。
         */
        public T getValue() {
            return value;
        }

        /**
         * 获取执行命令的 {@link RedisClusterNode}
         *
         * @return 从不为 {@literal null}
         */
        public RedisClusterNode getNode() {
            return node;
        }

        public byte[] getKey() {
            return key.getArray();
        }

        /**
         * 将 {@link Function mapper function} 应用于该值并返回映射值
         *
         * @param mapper 不得为 {@literal null}
         * @param <U>    映射值的类型
         * @return 映射值
         */
        public <U> U mapValue(Function<? super T, ? extends U> mapper) {
            Assert.notNull(mapper, "Mapper function must not be null!");
            return mapper.apply(getValue());
        }
    }

    /**
     * {@link MultiNodeResult} 包含在多个 {@link RedisClusterNode} 上执行的命令的所有 {@link NodeResult}
     */
    public static class MultiNodeResult<T> {
        List<NodeResult<T>> nodeResults = new ArrayList<>();
        Map<PositionalKey, NodeResult<T>> positionalResults = new LinkedHashMap<>();

        private void add(NodeResult<T> result) {
            nodeResults.add(result);
        }

        private void add(PositionalKey key, NodeResult<T> result) {
            positionalResults.put(key, result);
            add(result);
        }

        /**
         * @return 从不为 {@literal null}
         */
        public List<NodeResult<T>> getResults() {
            return Collections.unmodifiableList(nodeResults);
        }

        /**
         * 获取所有单个 {@link NodeResult#value} 的 {@link List}。 <br />
         * 生成的 {@link List} 可能包含 {@literal null} 值。
         *
         * @return 从不为 {@literal null}
         */
        public List<T> resultsAsList() {
            return toList(nodeResults);
        }

        /**
         * 获取所有单个 {@link NodeResult#value} 的 {@link List} <br />
         * 生成的 {@link List} 可能包含 {@literal null} 值
         *
         * @return 从不为 {@literal null}
         */
        public List<T> resultsAsListSortBy(byte[]... keys) {
            if (positionalResults.isEmpty()) {
                List<NodeResult<T>> clone = new ArrayList<>(nodeResults);
                clone.sort(new ResultByReferenceKeyPositionComparator(keys));
                return toList(clone);
            }
            Map<PositionalKey, NodeResult<T>> result = new TreeMap<>(new ResultByKeyPositionComparator(keys));
            result.putAll(positionalResults);
            return result.values().stream().map(tNodeResult -> tNodeResult.value).collect(Collectors.toList());
        }

        /**
         * @param returnValue 可以是 {@literal null}。
         * @return 可以是 {@literal null}。
         */
        public T getFirstNonNullNotEmptyOrDefault(T returnValue) {
            for (NodeResult<T> nodeResult : nodeResults) {
                if (nodeResult.getValue() != null) {
                    if (nodeResult.getValue() instanceof Map) {
                        if (CollectionUtils.isEmpty((Map<?, ?>) nodeResult.getValue())) {
                            return nodeResult.getValue();
                        }
                    } else if (nodeResult.getValue() instanceof Collection && CollectionUtils.isEmpty((Collection<?>) nodeResult.getValue())) {
                        return nodeResult.getValue();
                    } else {
                        return nodeResult.getValue();
                    }
                }
            }
            return returnValue;
        }

        private List<T> toList(Collection<NodeResult<T>> source) {
            ArrayList<T> result = new ArrayList<>();
            for (NodeResult<T> nodeResult : source) {
                result.add(nodeResult.getValue());
            }
            return result;
        }

        /**
         * {@link Comparator} 用于按引用键对 {@link NodeResult} 进行排序
         */
        private static class ResultByReferenceKeyPositionComparator implements Comparator<NodeResult<?>> {
            private final List<ByteArrayWrapper> reference;

            ResultByReferenceKeyPositionComparator(byte[]... keys) {
                reference = new ArrayList<>(new ByteArraySet(Arrays.asList(keys)));
            }

            @Override
            public int compare(NodeResult<?> o1, NodeResult<?> o2) {
                return Integer.compare(reference.indexOf(o1.key), reference.indexOf(o2.key));
            }
        }

        /**
         * {@link Comparator} 用于按外部 {@link PositionalKeys} 对 {@link PositionalKey} 进行排序
         */
        private static class ResultByKeyPositionComparator implements Comparator<PositionalKey> {
            private final PositionalKeys reference;

            ResultByKeyPositionComparator(byte[]... keys) {
                reference = PositionalKeys.of(keys);
            }

            @Override
            public int compare(PositionalKey o1, PositionalKey o2) {
                return Integer.compare(reference.indexOf(o1), reference.indexOf(o2));
            }
        }
    }

    /**
     * 表示特定命令位置处的 Redis 键的值对象
     */
    private static class PositionalKey {
        private final ByteArrayWrapper key;
        private final int position;

        private PositionalKey(ByteArrayWrapper key, int position) {
            this.key = key;
            this.position = position;
        }

        static PositionalKey of(byte[] key, int index) {
            return new PositionalKey(new ByteArrayWrapper(key), index);
        }

        /**
         * @return 二进制 key
         */
        byte[] getBytes() {
            return key.getArray();
        }

        public ByteArrayWrapper getKey() {
            return this.key;
        }

        public int getPosition() {
            return this.position;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            PositionalKey that = (PositionalKey) o;
            if (position != that.position)
                return false;
            return ObjectUtils.nullSafeEquals(key, that.key);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode(key);
            result = 31 * result + position;
            return result;
        }
    }

    /**
     * 表示多个 {@link PositionalKey} 的可变数据结构
     */
    private static class PositionalKeys implements Iterable<PositionalKey> {
        private final List<PositionalKey> keys;

        private PositionalKeys(List<PositionalKey> keys) {
            this.keys = keys;
        }

        /**
         * 创建一个空的 {@link PositionalKeys}
         */
        static PositionalKeys empty() {
            return new PositionalKeys(new ArrayList<>());
        }

        /**
         * 从 {@code keys} 创建一个 {@link PositionalKeys}
         */
        static PositionalKeys of(byte[]... keys) {
            List<PositionalKey> result = new ArrayList<>(keys.length);
            for (int i = 0; i < keys.length; i++) {
                result.add(PositionalKey.of(keys[i], i));
            }
            return new PositionalKeys(result);
        }

        /**
         * 从 {@link PositionalKey} 创建一个 {@link PositionalKeys}
         */
        static PositionalKeys of(PositionalKey... keys) {
            PositionalKeys result = PositionalKeys.empty();
            result.append(keys);
            return result;
        }

        /**
         * Append {@link PositionalKey} 到此对象
         */
        void append(PositionalKey... keys) {
            this.keys.addAll(Arrays.asList(keys));
        }

        /**
         * @return {@link PositionalKey} 的索引
         */
        int indexOf(PositionalKey key) {
            return keys.indexOf(key);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<PositionalKey> iterator() {
            return keys.iterator();
        }
    }
}
