package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * {@link LettuceConnectionProvider} 实现静态 MasterReplica 连接，适用于例如。具有副本设置的 AWS ElasticCache。<br/>
 * Lettuce 自动从静态 {@link RedisURI} 集合中发现节点角色。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:15 <br/>
 */
class StaticMasterReplicaConnectionProvider implements LettuceConnectionProvider {
    private final RedisClient client;
    private final RedisCodec<?, ?> codec;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<ReadFrom> readFrom;
    private final Collection<RedisURI> nodes;

    /**
     * 创建新的 {@link StaticMasterReplicaConnectionProvider}
     *
     * @param client   不得为 {@literal null}
     * @param codec    不得为 {@literal null}
     * @param nodes    不得为 {@literal null}
     * @param readFrom 可以是 {@literal null}。
     */
    StaticMasterReplicaConnectionProvider(RedisClient client, RedisCodec<?, ?> codec, Collection<RedisURI> nodes, ReadFrom readFrom) {
        this.client = client;
        this.codec = codec;
        this.readFrom = Optional.ofNullable(readFrom);
        this.nodes = nodes;
    }

    @Override
    public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType) {
        if (connectionType.equals(StatefulRedisPubSubConnection.class)) {
            throw new UnsupportedOperationException("Pub/Sub connections not supported with Master/Replica configurations");
        }
        if (StatefulConnection.class.isAssignableFrom(connectionType)) {
            StatefulRedisMasterReplicaConnection<?, ?> connection = MasterReplica.connect(client, codec, nodes);
            readFrom.ifPresent(connection::setReadFrom);
            return connectionType.cast(connection);
        }
        throw new UnsupportedOperationException(String.format("Connection type %s not supported!", connectionType));
    }

    @Override
    public <T extends StatefulConnection<?, ?>> CompletionStage<T> getConnectionAsync(Class<T> connectionType) {
        if (StatefulConnection.class.isAssignableFrom(connectionType)) {
            CompletableFuture<? extends StatefulRedisMasterReplicaConnection<?, ?>> connection = MasterReplica.connectAsync(client, codec, nodes);
            return connection.thenApply(it -> {
                // noinspection Convert2MethodRef
                readFrom.ifPresent(readFrom -> it.setReadFrom(readFrom));
                return connectionType.cast(it);
            });
        }
        throw new UnsupportedOperationException(String.format("Connection type %s not supported!", connectionType));
    }
}
