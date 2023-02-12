package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.sync.RedisKeyCommands;
import org.clever.data.redis.connection.ClusterSlotHashUtil;
import org.clever.data.redis.connection.RedisClusterNode;
import org.clever.data.redis.connection.SortParameters;
import org.clever.data.redis.connection.lettuce.LettuceClusterConnection.LettuceClusterCommandCallback;
import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.ScanCursor;
import org.clever.data.redis.core.ScanOptions;
import org.clever.util.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:01 <br/>
 */
class LettuceClusterKeyCommands extends LettuceKeyCommands {
    private final LettuceClusterConnection connection;

    LettuceClusterKeyCommands(LettuceClusterConnection connection) {
        super(connection);
        this.connection = connection;
    }

    @Override
    public byte[] randomKey() {
        List<RedisClusterNode> nodes = connection.clusterGetNodes();
        Set<RedisClusterNode> inspectedNodes = new HashSet<>(nodes.size());
        do {
            RedisClusterNode node = nodes.get(ThreadLocalRandom.current().nextInt(nodes.size()));
            while (inspectedNodes.contains(node)) {
                node = nodes.get(ThreadLocalRandom.current().nextInt(nodes.size()));
            }
            inspectedNodes.add(node);
            byte[] key = randomKey(node);
            if (key != null && key.length > 0) {
                return key;
            }
        } while (nodes.size() != inspectedNodes.size());
        return null;
    }

    @Override
    public Set<byte[]> keys(byte[] pattern) {
        Assert.notNull(pattern, "Pattern must not be null!");
        Collection<List<byte[]>> keysPerNode = connection.getClusterCommandExecutor()
                .executeCommandOnAllNodes((LettuceClusterCommandCallback<List<byte[]>>) connection -> connection.keys(pattern))
                .resultsAsList();
        Set<byte[]> keys = new HashSet<>();
        for (List<byte[]> keySet : keysPerNode) {
            keys.addAll(keySet);
        }
        return keys;
    }

    @Override
    public void rename(byte[] oldKey, byte[] newKey) {
        Assert.notNull(oldKey, "Old key must not be null!");
        Assert.notNull(newKey, "New key must not be null!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(oldKey, newKey)) {
            super.rename(oldKey, newKey);
            return;
        }
        byte[] value = dump(oldKey);
        if (value != null && value.length > 0) {
            restore(newKey, 0, value, true);
            del(oldKey);
        }
    }

    @Override
    public Boolean renameNX(byte[] sourceKey, byte[] targetKey) {
        Assert.notNull(sourceKey, "Source key must not be null!");
        Assert.notNull(targetKey, "Target key must not be null!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(sourceKey, targetKey)) {
            return super.renameNX(sourceKey, targetKey);
        }
        byte[] value = dump(sourceKey);
        if (value != null && value.length > 0 && !exists(targetKey)) {
            restore(targetKey, 0, value);
            del(sourceKey);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean move(byte[] key, int dbIndex) {
        throw new UnsupportedOperationException("MOVE not supported in CLUSTER mode!");
    }

    public byte[] randomKey(RedisClusterNode node) {
        return connection.getClusterCommandExecutor()
                .executeCommandOnSingleNode((LettuceClusterCommandCallback<byte[]>) RedisKeyCommands::randomkey, node)
                .getValue();
    }

    public Set<byte[]> keys(RedisClusterNode node, byte[] pattern) {
        Assert.notNull(pattern, "Pattern must not be null!");
        return LettuceConverters.toBytesSet(connection.getClusterCommandExecutor()
                .executeCommandOnSingleNode((LettuceClusterCommandCallback<List<byte[]>>) client -> client.keys(pattern), node)
                .getValue());
    }

    /**
     * 使用 {@link Cursor} 遍历存储在给定 {@link RedisClusterNode} 上的键
     *
     * @param node    不得为 {@literal null}
     * @param options 不得为 {@literal null}
     * @return 从不为 {@literal null}.
     */
    Cursor<byte[]> scan(RedisClusterNode node, ScanOptions options) {
        Assert.notNull(node, "RedisClusterNode must not be null!");
        Assert.notNull(options, "Options must not be null!");
        // noinspection resource
        return connection.getClusterCommandExecutor().executeCommandOnSingleNode(
                (LettuceClusterCommandCallback<ScanCursor<byte[]>>) client -> new LettuceScanCursor<byte[]>(options) {
                    @Override
                    protected LettuceScanIteration<byte[]> doScan(io.lettuce.core.ScanCursor cursor, ScanOptions options) {
                        ScanArgs scanArgs = LettuceConverters.toScanArgs(options);
                        KeyScanCursor<byte[]> keyScanCursor = client.scan(cursor, scanArgs);
                        return new LettuceScanIteration<>(keyScanCursor, keyScanCursor.getKeys());
                    }
                }.open(), node
        ).getValue();
    }

    @Override
    public Long sort(byte[] key, SortParameters params, byte[] storeKey) {
        Assert.notNull(key, "Key must not be null!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(key, storeKey)) {
            return super.sort(key, params, storeKey);
        }
        List<byte[]> sorted = sort(key, params);
        byte[][] arr = new byte[sorted.size()][];
        connection.keyCommands().unlink(storeKey);
        connection.listCommands().lPush(storeKey, sorted.toArray(arr));
        return (long) sorted.size();
    }
}
