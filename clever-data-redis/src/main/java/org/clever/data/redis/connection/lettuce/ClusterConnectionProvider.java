package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.clever.util.Assert;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 集群连接的连接提供程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:16 <br/>
 */
class ClusterConnectionProvider implements LettuceConnectionProvider, RedisClientProvider {
    private final RedisClusterClient client;
    private final RedisCodec<?, ?> codec;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<ReadFrom> readFrom;
    private final Object monitor = new Object();
    private volatile boolean initialized;

    /**
     * 创建新的 {@link ClusterConnectionProvider}
     *
     * @param client 不得为 {@literal null}
     * @param codec  不得为 {@literal null}
     */
    ClusterConnectionProvider(RedisClusterClient client, RedisCodec<?, ?> codec) {
        this(client, codec, null);
    }

    /**
     * 创建新的 {@link ClusterConnectionProvider}
     *
     * @param client   不得为 {@literal null}
     * @param codec    不得为 {@literal null}
     * @param readFrom 可以是 {@literal null}。
     */
    ClusterConnectionProvider(RedisClusterClient client, RedisCodec<?, ?> codec, ReadFrom readFrom) {
        Assert.notNull(client, "Client must not be null!");
        Assert.notNull(codec, "Codec must not be null!");
        this.client = client;
        this.codec = codec;
        this.readFrom = Optional.ofNullable(readFrom);
    }

    @Override
    public <T extends StatefulConnection<?, ?>> CompletableFuture<T> getConnectionAsync(Class<T> connectionType) {
        if (!initialized) {
            // partitions have to be initialized before asynchronous usage.
            // Needs to happen only once. Initialize eagerly if blocking is not an options.
            synchronized (monitor) {
                if (!initialized) {
                    client.getPartitions();
                    initialized = true;
                }
            }
        }
        if (connectionType.equals(StatefulRedisPubSubConnection.class) || connectionType.equals(StatefulRedisClusterPubSubConnection.class)) {
            return client.connectPubSubAsync(codec).thenApply(connectionType::cast);
        }
        if (StatefulRedisClusterConnection.class.isAssignableFrom(connectionType) || connectionType.equals(StatefulConnection.class)) {
            return client.connectAsync(codec).thenApply(connection -> {
                readFrom.ifPresent(connection::setReadFrom);
                return connectionType.cast(connection);
            });
        }
        return LettuceFutureUtils.failed(new UnsupportedOperationException("Connection type " + connectionType + " not supported!"));
    }

    @Override
    public RedisClusterClient getRedisClient() {
        return client;
    }
}
