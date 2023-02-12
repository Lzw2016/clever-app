package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.KeyValue;
import org.clever.data.redis.connection.ClusterSlotHashUtil;
import org.clever.data.redis.connection.lettuce.LettuceClusterConnection.LettuceMultiKeyClusterCommandCallback;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:05 <br/>
 */
class LettuceClusterListCommands extends LettuceListCommands {
    private final LettuceClusterConnection connection;

    LettuceClusterListCommands(LettuceClusterConnection connection) {
        super(connection);
        this.connection = connection;
    }

    @Override
    public List<byte[]> bLPop(int timeout, byte[]... keys) {
        Assert.notNull(keys, "Keys must not be null!");
        Assert.noNullElements(keys, "Keys must not contain null elements!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(keys)) {
            return super.bLPop(timeout, keys);
        }
        List<KeyValue<byte[], byte[]>> resultList = connection.getClusterCommandExecutor().executeMultiKeyCommand(
                (LettuceMultiKeyClusterCommandCallback<KeyValue<byte[], byte[]>>) (client, key) -> client.blpop(timeout, key),
                Arrays.asList(keys)
        ).resultsAsList();
        for (KeyValue<byte[], byte[]> kv : resultList) {
            if (kv != null) {
                return LettuceConverters.toBytesList(kv);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<byte[]> bRPop(int timeout, byte[]... keys) {
        Assert.notNull(keys, "Keys must not be null!");
        Assert.noNullElements(keys, "Keys must not contain null elements!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(keys)) {
            return super.bRPop(timeout, keys);
        }
        List<KeyValue<byte[], byte[]>> resultList = connection.getClusterCommandExecutor().executeMultiKeyCommand(
                (LettuceMultiKeyClusterCommandCallback<KeyValue<byte[], byte[]>>) (client, key) -> client.brpop(timeout, key),
                Arrays.asList(keys)
        ).resultsAsList();
        for (KeyValue<byte[], byte[]> kv : resultList) {
            if (kv != null) {
                return LettuceConverters.toBytesList(kv);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public byte[] rPopLPush(byte[] srcKey, byte[] dstKey) {
        Assert.notNull(srcKey, "Source key must not be null!");
        Assert.notNull(dstKey, "Destination key must not be null!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(srcKey, dstKey)) {
            return super.rPopLPush(srcKey, dstKey);
        }
        byte[] val = rPop(srcKey);
        lPush(dstKey, val);
        return val;
    }

    @Override
    public byte[] bRPopLPush(int timeout, byte[] srcKey, byte[] dstKey) {
        Assert.notNull(srcKey, "Source key must not be null!");
        Assert.notNull(dstKey, "Destination key must not be null!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(srcKey, dstKey)) {
            return super.bRPopLPush(timeout, srcKey, dstKey);
        }
        List<byte[]> val = bRPop(timeout, srcKey);
        if (!CollectionUtils.isEmpty(val)) {
            lPush(dstKey, val.get(1));
            return val.get(1);
        }
        return null;
    }
}
