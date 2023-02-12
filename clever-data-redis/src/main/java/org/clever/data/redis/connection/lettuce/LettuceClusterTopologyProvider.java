package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.cluster.RedisClusterClient;
import org.clever.data.redis.connection.ClusterTopology;
import org.clever.data.redis.connection.ClusterTopologyProvider;
import org.clever.util.Assert;

import java.util.LinkedHashSet;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:29 <br/>
 */
class LettuceClusterTopologyProvider implements ClusterTopologyProvider {
    private final RedisClusterClient client;

    /**
     * @param client 不得为 {@literal null}
     */
    LettuceClusterTopologyProvider(RedisClusterClient client) {
        Assert.notNull(client, "RedisClusterClient must not be null.");
        this.client = client;
    }

    @Override
    public ClusterTopology getTopology() {
        return new ClusterTopology(new LinkedHashSet<>(LettuceConverters.partitionsToClusterNodes(client.getPartitions())));
    }
}
