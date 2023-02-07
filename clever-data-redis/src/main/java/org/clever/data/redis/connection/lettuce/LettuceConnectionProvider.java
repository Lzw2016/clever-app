package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 定义Lettuce连接的提供程序。
 * <p>
 * 此接口通常用于封装本机工厂，该工厂在每次调用时返回 {@link StatefulConnection 连接}。
 * <p>
 * 连接提供程序可以在每个调用或返回池实例上创建新连接。
 * 每个获取的连接都必须通过其连接提供程序释放，以允许处置或释放回池。
 * <p>
 * 连接提供程序通常与 {@link io.lettuce.core.codec.RedisCodec} 相关联，以创建具有相应编解码器的连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:53 <br/>
 *
 * @see StatefulConnection
 */
@FunctionalInterface
public interface LettuceConnectionProvider {
    /**
     * 请求给定 {@code connectionType} 的连接。提供连接类型允许专用化提供更具体的连接类型。
     *
     * @param connectionType 不得为 {@literal null}
     * @return 请求的连接。如果连接不再使用，则必须为 {@link #release(StatefulConnection) released}
     */
    default <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType) {
        return LettuceFutureUtils.join(getConnectionAsync(connectionType));
    }

    /**
     * 异步请求给定 {@code connectionType} 的连接。提供连接类型允许专用化提供更具体的连接类型
     *
     * @param connectionType 不得为 {@literal null}
     * @return 收到连接进度通知的 {@link CompletionStage}。如果连接不再使用，则必须 {@link #releaseAsync(StatefulConnection) released}。
     */
    <T extends StatefulConnection<?, ?>> CompletionStage<T> getConnectionAsync(Class<T> connectionType);

    /**
     * 释放 {@link StatefulConnection 连接}。默认情况下关闭连接 {@link StatefulConnection#close()}。
     * 实现可以选择是否覆盖此方法并将连接返回到池中。
     *
     * @param connection 不得为 {@literal null}
     */
    default void release(StatefulConnection<?, ?> connection) {
        LettuceFutureUtils.join(releaseAsync(connection));
    }

    /**
     * 异步释放 {@link StatefulConnection 连接}。
     * 默认情况下关闭连接 {@link StatefulConnection#closeAsync()}。
     * 实现可以选择是否覆盖此方法并将连接返回到池中。
     *
     * @param connection 不得为 {@literal null}
     * @return Close {@link CompletableFuture future} notified once the connection is released.
     */
    default CompletableFuture<Void> releaseAsync(StatefulConnection<?, ?> connection) {
        return connection.closeAsync();
    }

    /**
     * {@link LettuceConnectionProvider} 的扩展，用于允许创建到特定节点的连接的提供程序
     */
    @FunctionalInterface
    interface TargetAware {
        /**
         * 为特定的 {@link RedisURI} 请求给定 {@code connectionType} 的连接。提供连接类型允许专门化以提供更具体的连接类型。
         *
         * @param connectionType 不得为 {@literal null}
         * @param redisURI       不得为 {@literal null}
         * @return 请求的连接
         */
        default <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType, RedisURI redisURI) {
            return LettuceFutureUtils.join(getConnectionAsync(connectionType, redisURI));
        }

        /**
         * 为特定的 {@link RedisURI} 异步请求给定 {@code connectionType} 的连接。提供连接类型允许专门化以提供更具体的连接类型。
         *
         * @param connectionType 不得为 {@literal null}
         * @param redisURI       不得为 {@literal null}
         * @return a {@link CompletionStage} 连接进度通知
         */
        <T extends StatefulConnection<?, ?>> CompletionStage<T> getConnectionAsync(Class<T> connectionType, RedisURI redisURI);
    }
}
