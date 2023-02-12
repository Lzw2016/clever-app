package org.clever.data.redis.connection.lettuce;

import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.data.redis.connection.ClusterSlotHashUtil;
import org.clever.data.redis.util.ByteUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 18:11 <br/>
 */
class LettuceClusterHyperLogLogCommands extends LettuceHyperLogLogCommands {
    LettuceClusterHyperLogLogCommands(LettuceClusterConnection connection) {
        super(connection);
    }

    @Override
    public Long pfCount(byte[]... keys) {
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(keys)) {
            return super.pfCount(keys);
        }
        throw new InvalidDataAccessApiUsageException("All keys must map to same slot for pfcount in cluster mode.");
    }

    @Override
    public void pfMerge(byte[] destinationKey, byte[]... sourceKeys) {
        byte[][] allKeys = ByteUtils.mergeArrays(destinationKey, sourceKeys);
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(allKeys)) {
            super.pfMerge(destinationKey, sourceKeys);
            return;
        }
        throw new InvalidDataAccessApiUsageException("All keys must map to same slot for pfmerge in cluster mode.");
    }
}
