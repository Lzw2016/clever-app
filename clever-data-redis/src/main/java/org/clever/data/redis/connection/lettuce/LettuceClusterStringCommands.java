package org.clever.data.redis.connection.lettuce;

import org.clever.data.redis.connection.ClusterSlotHashUtil;
import org.clever.util.Assert;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:14 <br/>
 */
class LettuceClusterStringCommands extends LettuceStringCommands {
    LettuceClusterStringCommands(LettuceClusterConnection connection) {
        super(connection);
    }

    @Override
    public Boolean mSetNX(Map<byte[], byte[]> tuples) {
        Assert.notNull(tuples, "Tuples must not be null!");
        if (ClusterSlotHashUtil.isSameSlotForAllKeys(tuples.keySet().toArray(new byte[tuples.keySet().size()][]))) {
            return super.mSetNX(tuples);
        }
        boolean result = true;
        for (Map.Entry<byte[], byte[]> entry : tuples.entrySet()) {
            if (!setNX(entry.getKey(), entry.getValue()) && result) {
                result = false;
            }
        }
        return result;
    }
}
