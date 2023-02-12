package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.*;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.beans.factory.DisposableBean;
import org.clever.data.redis.connection.PoolException;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link LettuceConnectionProvider} 支持连接池。
 * 此连接提供程序拥有多个池（每个连接类型和分配类型（同步异步）一个）用于上下文连接分配。
 * <p>
 * 每个分配的连接都会被跟踪并返回到创建连接的池中。
 * 此类的实例需要 {@link #destroy() disposal} 来取消分配未返回到池中的延迟连接并关闭池。
 * <p>
 * 由于分配性质（同步异步），此提供程序维护单独的池。
 * 异步连接池需要非阻塞分配 API。
 * 异步请求的连接可以同步返回，反之亦然。
 * 即使{@link #releaseAsync(StatefulConnection) 异步释放}，同步获得的连接也会返回到同步池。
 * 这是一种不受欢迎的情况，因为同步池将在释放时阻塞异步流。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:21 <br/>
 *
 * @see #getConnection(Class)
 */
class LettucePoolingConnectionProvider implements LettuceConnectionProvider, RedisClientProvider, DisposableBean {
    private final static Logger log = LoggerFactory.getLogger(LettucePoolingConnectionProvider.class);
    private final LettuceConnectionProvider connectionProvider;
    private final GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig;
    private final Map<StatefulConnection<?, ?>, GenericObjectPool<StatefulConnection<?, ?>>> poolRef = new ConcurrentHashMap<>(32);
    private final Map<StatefulConnection<?, ?>, AsyncPool<StatefulConnection<?, ?>>> asyncPoolRef = new ConcurrentHashMap<>(32);
    private final Map<CompletableFuture<StatefulConnection<?, ?>>, AsyncPool<StatefulConnection<?, ?>>> inProgressAsyncPoolRef = new ConcurrentHashMap<>(32);
    private final Map<Class<?>, GenericObjectPool<StatefulConnection<?, ?>>> pools = new ConcurrentHashMap<>(32);
    private final Map<Class<?>, AsyncPool<StatefulConnection<?, ?>>> asyncPools = new ConcurrentHashMap<>(32);
    private final BoundedPoolConfig asyncPoolConfig;

    LettucePoolingConnectionProvider(LettuceConnectionProvider connectionProvider, LettucePoolingClientConfiguration clientConfiguration) {
        Assert.notNull(connectionProvider, "ConnectionProvider must not be null!");
        Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");
        this.connectionProvider = connectionProvider;
        this.poolConfig = clientConfiguration.getPoolConfig();
        this.asyncPoolConfig = CommonsPool2ConfigConverter.bounded(this.poolConfig);
    }

    @Override
    public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType) {
        GenericObjectPool<StatefulConnection<?, ?>> pool = pools.computeIfAbsent(
                connectionType,
                poolType -> ConnectionPoolSupport.createGenericObjectPool(
                        () -> connectionProvider.getConnection(connectionType),
                        poolConfig,
                        false
                )
        );
        try {
            StatefulConnection<?, ?> connection = pool.borrowObject();
            poolRef.put(connection, pool);
            return connectionType.cast(connection);
        } catch (Exception e) {
            throw new PoolException("Could not get a resource from the pool", e);
        }
    }

    @Override
    public <T extends StatefulConnection<?, ?>> CompletionStage<T> getConnectionAsync(Class<T> connectionType) {
        AsyncPool<StatefulConnection<?, ?>> pool = asyncPools.computeIfAbsent(
                connectionType,
                poolType -> AsyncConnectionPoolSupport.createBoundedObjectPool(
                        () -> connectionProvider.getConnectionAsync(connectionType).thenApply(connectionType::cast),
                        asyncPoolConfig,
                        false
                )
        );
        CompletableFuture<StatefulConnection<?, ?>> acquire = pool.acquire();
        inProgressAsyncPoolRef.put(acquire, pool);
        return acquire.whenComplete((connection, e) -> {
            // noinspection resource
            inProgressAsyncPoolRef.remove(acquire);
            if (connection != null) {
                asyncPoolRef.put(connection, pool);
            }
        }).thenApply(connectionType::cast);
    }

    @Override
    public AbstractRedisClient getRedisClient() {
        if (connectionProvider instanceof RedisClientProvider) {
            return ((RedisClientProvider) connectionProvider).getRedisClient();
        }
        throw new IllegalStateException(String.format(
                "Underlying connection provider %s does not implement RedisClientProvider!",
                connectionProvider.getClass().getName()
        ));
    }

    @Override
    public void release(StatefulConnection<?, ?> connection) {
        GenericObjectPool<StatefulConnection<?, ?>> pool = poolRef.remove(connection);
        if (pool == null) {
            AsyncPool<StatefulConnection<?, ?>> asyncPool = asyncPoolRef.remove(connection);
            if (asyncPool == null) {
                throw new PoolException("Returned connection " + connection + " was either previously returned or does not belong to this connection provider");
            }
            discardIfNecessary(connection);
            asyncPool.release(connection).join();
            return;
        }
        discardIfNecessary(connection);
        pool.returnObject(connection);
    }

    private void discardIfNecessary(StatefulConnection<?, ?> connection) {
        if (connection instanceof StatefulRedisConnection) {
            StatefulRedisConnection<?, ?> redisConnection = (StatefulRedisConnection<?, ?>) connection;
            if (redisConnection.isMulti()) {
                redisConnection.async().discard();
            }
        }
    }

    @Override
    public CompletableFuture<Void> releaseAsync(StatefulConnection<?, ?> connection) {
        GenericObjectPool<StatefulConnection<?, ?>> blockingPool = poolRef.remove(connection);
        if (blockingPool != null) {
            log.warn("Releasing asynchronously a connection that was obtained from a non-blocking pool");
            blockingPool.returnObject(connection);
            return CompletableFuture.completedFuture(null);
        }
        AsyncPool<StatefulConnection<?, ?>> pool = asyncPoolRef.remove(connection);
        if (pool == null) {
            return LettuceFutureUtils.failed(new PoolException("Returned connection " + connection + " was either previously returned or does not belong to this connection provider"));
        }
        return pool.release(connection);
    }

    @Override
    public void destroy() throws Exception {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        if (!poolRef.isEmpty() || !asyncPoolRef.isEmpty()) {
            log.warn("LettucePoolingConnectionProvider contains unreleased connections");
        }
        if (!inProgressAsyncPoolRef.isEmpty()) {
            log.warn("LettucePoolingConnectionProvider has active connection retrievals");
            inProgressAsyncPoolRef.forEach((k, v) -> futures.add(k.thenApply(StatefulConnection::closeAsync)));
        }
        if (!poolRef.isEmpty()) {
            poolRef.forEach((connection, pool) -> pool.returnObject(connection));
            poolRef.clear();
        }
        if (!asyncPoolRef.isEmpty()) {
            asyncPoolRef.forEach((connection, pool) -> futures.add(pool.release(connection)));
            asyncPoolRef.clear();
        }
        pools.forEach((type, pool) -> pool.close());
        CompletableFuture
                .allOf(futures.stream().map(it -> it.exceptionally(LettuceFutureUtils.ignoreErrors())).toArray(CompletableFuture[]::new))
                .thenCompose(ignored -> {
                    CompletableFuture<?>[] poolClose = asyncPools.values().stream().map(AsyncPool::closeAsync)
                            .map(it -> it.exceptionally(LettuceFutureUtils.ignoreErrors()))
                            .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(poolClose);
                }).thenRun(() -> {
                    asyncPoolRef.clear();
                    inProgressAsyncPoolRef.clear();
                }).join();
        pools.clear();
    }
}
