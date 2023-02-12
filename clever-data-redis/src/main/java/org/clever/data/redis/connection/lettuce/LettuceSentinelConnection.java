package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisURI.Builder;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import org.clever.data.redis.ExceptionTranslationStrategy;
import org.clever.data.redis.FallbackExceptionTranslationStrategy;
import org.clever.data.redis.connection.NamedNode;
import org.clever.data.redis.connection.RedisNode;
import org.clever.data.redis.connection.RedisSentinelConnection;
import org.clever.data.redis.connection.RedisServer;
import org.clever.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:13 <br/>
 */
public class LettuceSentinelConnection implements RedisSentinelConnection {
    private static final ExceptionTranslationStrategy EXCEPTION_TRANSLATION = new FallbackExceptionTranslationStrategy(LettuceConverters.exceptionConverter());

    private final LettuceConnectionProvider provider;
    // no that should not be null
    private StatefulRedisSentinelConnection<String, String> connection;

    /**
     * 为提供的 {@link RedisNode} 创建一个带有专用客户端的 {@link LettuceSentinelConnection}
     *
     * @param sentinel 要连接的哨兵
     */
    public LettuceSentinelConnection(RedisNode sentinel) {
        this(sentinel.getHost(), sentinel.getPort());
    }

    /**
     * 为提供的 {@code host} 和 {@code port} 创建一个带有客户端的 {@link LettuceSentinelConnection}
     *
     * @param host 不得为 {@literal null}
     * @param port 哨兵 port
     */
    public LettuceSentinelConnection(String host, int port) {
        Assert.notNull(host, "Cannot create LettuceSentinelConnection using 'null' as host.");
        this.provider = new DedicatedClientConnectionProvider(host, port);
        init();
    }

    /**
     * 为提供的 {@code host} 和 {@code port} 创建一个带有客户端的 {@link LettuceSentinelConnection} 并重用现有的 {@link ClientResources}
     *
     * @param host            不得为 {@literal null}
     * @param port            哨兵 port
     * @param clientResources 不得为 {@literal null}
     */
    public LettuceSentinelConnection(String host, int port, ClientResources clientResources) {
        Assert.notNull(clientResources, "Cannot create LettuceSentinelConnection using 'null' as ClientResources.");
        Assert.notNull(host, "Cannot create LettuceSentinelConnection using 'null' as host.");
        this.provider = new DedicatedClientConnectionProvider(host, port, clientResources);
        init();
    }

    /**
     * 使用提供的 {@link RedisClient} 创建一个 {@link LettuceSentinelConnection}
     *
     * @param redisClient 不得为 {@literal null}
     */
    public LettuceSentinelConnection(RedisClient redisClient) {
        Assert.notNull(redisClient, "Cannot create LettuceSentinelConnection using 'null' as client.");
        this.provider = new LettuceConnectionProvider() {
            @Override
            public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> t) {
                return t.cast(redisClient.connectSentinel());
            }

            @Override
            public <T extends StatefulConnection<?, ?>> CompletableFuture<T> getConnectionAsync(Class<T> t) {
                return CompletableFuture.completedFuture(t.cast(connection));
            }
        };
        init();
    }

    /**
     * 使用提供的 redis 连接创建 {@link LettuceSentinelConnection}
     *
     * @param connection 原生 Lettuce 连接，不能为 {@literal null}
     */
    protected LettuceSentinelConnection(StatefulRedisSentinelConnection<String, String> connection) {
        Assert.notNull(connection, "Cannot create LettuceSentinelConnection using 'null' as connection.");
        this.provider = new LettuceConnectionProvider() {
            @Override
            public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> t) {
                return t.cast(connection);
            }

            @Override
            public <T extends StatefulConnection<?, ?>> CompletableFuture<T> getConnectionAsync(Class<T> t) {
                return CompletableFuture.completedFuture(t.cast(connection));
            }
        };
        init();
    }

    /**
     * 使用 {@link LettuceConnectionProvider} 创建一个 {@link LettuceSentinelConnection}
     *
     * @param connectionProvider 不得为 {@literal null}
     */
    public LettuceSentinelConnection(LettuceConnectionProvider connectionProvider) {
        Assert.notNull(connectionProvider, "LettuceConnectionProvider must not be null!");
        this.provider = connectionProvider;
        init();
    }

    @Override
    public void failover(NamedNode master) {
        Assert.notNull(master, "Redis node master must not be 'null' for failover.");
        Assert.hasText(master.getName(), "Redis master name must not be 'null' or empty for failover.");
        getSentinelCommands().failover(master.getName());
    }

    @Override
    public List<RedisServer> masters() {
        try {
            return LettuceConverters.toListOfRedisServer(getSentinelCommands().masters());
        } catch (Exception e) {
            throw EXCEPTION_TRANSLATION.translate(e);
        }
    }

    @Override
    public List<RedisServer> slaves(NamedNode master) {
        Assert.notNull(master, "Master node cannot be 'null' when loading slaves.");
        return slaves(master.getName());
    }

    /**
     * @see org.clever.data.redis.connection.RedisSentinelCommands#slaves(org.clever.data.redis.connection.NamedNode)
     */
    public List<RedisServer> slaves(String masterName) {
        Assert.hasText(masterName, "Name of redis master cannot be 'null' or empty when loading slaves.");
        try {
            return LettuceConverters.toListOfRedisServer(getSentinelCommands().slaves(masterName));
        } catch (Exception e) {
            throw EXCEPTION_TRANSLATION.translate(e);
        }
    }

    @Override
    public void remove(NamedNode master) {
        Assert.notNull(master, "Master node cannot be 'null' when trying to remove.");
        remove(master.getName());
    }

    /**
     * @see org.clever.data.redis.connection.RedisSentinelCommands#remove(org.clever.data.redis.connection.NamedNode)
     */
    public void remove(String masterName) {
        Assert.hasText(masterName, "Name of redis master cannot be 'null' or empty when trying to remove.");
        getSentinelCommands().remove(masterName);
    }

    @Override
    public void monitor(RedisServer server) {
        Assert.notNull(server, "Cannot monitor 'null' server.");
        Assert.hasText(server.getName(), "Name of server to monitor must not be 'null' or empty.");
        Assert.hasText(server.getHost(), "Host must not be 'null' for server to monitor.");
        Assert.notNull(server.getPort(), "Port must not be 'null' for server to monitor.");
        Assert.notNull(server.getQuorum(), "Quorum must not be 'null' for server to monitor.");
        getSentinelCommands().monitor(server.getName(), server.getHost(), server.getPort(), server.getQuorum().intValue());
    }

    @Override
    public void close() throws IOException {
        provider.release(connection);
    }

    @SuppressWarnings({"unchecked"})
    private void init() {
        if (connection == null) {
            connection = provider.getConnection(StatefulRedisSentinelConnection.class);
        }
    }

    private RedisSentinelCommands<String, String> getSentinelCommands() {
        return connection.sync();
    }

    @Override
    public boolean isOpen() {
        return connection != null && connection.isOpen();
    }

    /**
     * {@link LettuceConnectionProvider} 用于专用客户端实例
     */
    private static class DedicatedClientConnectionProvider implements LettuceConnectionProvider {
        private final RedisClient redisClient;
        private final RedisURI uri;

        DedicatedClientConnectionProvider(String host, int port) {
            Assert.notNull(host, "Cannot create LettuceSentinelConnection using 'null' as host.");
            uri = Builder.redis(host, port).build();
            redisClient = RedisClient.create(uri);
        }

        DedicatedClientConnectionProvider(String host, int port, ClientResources clientResources) {
            Assert.notNull(clientResources, "Cannot create LettuceSentinelConnection using 'null' as ClientResources.");
            Assert.notNull(host, "Cannot create LettuceSentinelConnection using 'null' as host.");
            this.uri = Builder.redis(host, port).build();
            redisClient = RedisClient.create(clientResources, uri);
        }

        @Override
        public <T extends StatefulConnection<?, ?>> CompletableFuture<T> getConnectionAsync(Class<T> connectionType) {
            return redisClient.connectSentinelAsync(StringCodec.UTF8, uri).thenApply(connectionType::cast);
        }

        @Override
        public void release(StatefulConnection<?, ?> connection) {
            connection.close();
            redisClient.shutdown();
        }

        @Override
        public CompletableFuture<Void> releaseAsync(StatefulConnection<?, ?> connection) {
            return connection.closeAsync().exceptionally(LettuceFutureUtils.ignoreErrors()).thenCompose(it -> redisClient.shutdownAsync());
        }
    }
}
