package org.clever.data.redis.core;

import org.clever.data.redis.connection.RedisClusterCommands.AddSlots;
import org.clever.data.redis.connection.RedisClusterConnection;
import org.clever.data.redis.connection.RedisClusterNode;
import org.clever.data.redis.connection.RedisClusterNode.SlotRange;
import org.clever.data.redis.connection.RedisServerCommands.MigrateOption;
import org.clever.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 默认 {@link ClusterOperations} 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:55 <br/>
 */
class DefaultClusterOperations<K, V> extends AbstractOperations<K, V> implements ClusterOperations<K, V> {
    private final RedisTemplate<K, V> template;

    /**
     * 创建新的 {@link DefaultClusterOperations} 委托给给定的 {@link RedisTemplate}
     *
     * @param template 不得为 {@literal null}
     */
    DefaultClusterOperations(RedisTemplate<K, V> template) {
        super(template);
        this.template = template;
    }

    @Override
    public Set<K> keys(final RedisClusterNode node, final K pattern) {
        Assert.notNull(node, "ClusterNode must not be null.");
        return doInCluster(connection -> deserializeKeys(connection.keys(node, rawKey(pattern))));
    }

    @Override
    public K randomKey(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        return doInCluster(connection -> deserializeKey(connection.randomKey(node)));
    }

    @Override
    public String ping(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        return doInCluster(connection -> connection.ping(node));
    }

    @Override
    public void addSlots(final RedisClusterNode node, final int... slots) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.clusterAddSlots(node, slots);
            return null;
        });
    }

    @Override
    public void addSlots(RedisClusterNode node, SlotRange range) {
        Assert.notNull(node, "ClusterNode must not be null.");
        Assert.notNull(range, "Range must not be null.");
        addSlots(node, range.getSlotsArray());
    }

    @Override
    public void bgReWriteAof(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.bgReWriteAof(node);
            return null;
        });
    }

    @Override
    public void bgSave(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.bgSave(node);
            return null;
        });
    }

    @Override
    public void meet(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.clusterMeet(node);
            return null;
        });
    }

    @Override
    public void forget(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.clusterForget(node);
            return null;
        });
    }

    @Override
    public void flushDb(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.flushDb(node);
            return null;
        });
    }

    @Override
    public Collection<RedisClusterNode> getSlaves(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        return doInCluster(connection -> connection.clusterGetSlaves(node));
    }

    @Override
    public void save(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.save(node);
            return null;
        });
    }

    @Override
    public void shutdown(final RedisClusterNode node) {
        Assert.notNull(node, "ClusterNode must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.shutdown(node);
            return null;
        });
    }

    @Override
    public void reshard(final RedisClusterNode source, final int slot, final RedisClusterNode target) {
        Assert.notNull(source, "Source node must not be null.");
        Assert.notNull(target, "Target node must not be null.");
        doInCluster((RedisClusterCallback<Void>) connection -> {
            connection.clusterSetSlot(target, slot, AddSlots.IMPORTING);
            connection.clusterSetSlot(source, slot, AddSlots.MIGRATING);
            List<byte[]> keys = connection.clusterGetKeysInSlot(slot, Integer.MAX_VALUE);
            for (byte[] key : keys) {
                connection.migrate(key, source, 0, MigrateOption.COPY);
            }
            connection.clusterSetSlot(target, slot, AddSlots.NODE);
            return null;
        });
    }

    /**
     * 在 {@link RedisClusterConnection} 上执行包装命令
     *
     * @param callback 不得为 {@literal null}
     * @return 执行结果。可以是 {@literal null}。
     */
    <T> T doInCluster(RedisClusterCallback<T> callback) {
        Assert.notNull(callback, "ClusterCallback must not be null!");
        try (RedisClusterConnection connection = template.getConnectionFactory().getClusterConnection()) {
            return callback.doInRedis(connection);
        }
    }
}
