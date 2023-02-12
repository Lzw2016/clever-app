package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.clever.data.redis.connection.MessageListener;
import org.clever.data.redis.connection.SubscriptionListener;
import org.clever.data.redis.connection.util.AbstractSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lettuce 之上的消息订阅
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:00 <br/>
 */
public class LettuceSubscription extends AbstractSubscription {
    private final StatefulRedisPubSubConnection<byte[], byte[]> connection;
    private final LettuceMessageListener listener;
    private final LettuceConnectionProvider connectionProvider;
    private final RedisPubSubCommands<byte[], byte[]> pubsub;
    private final RedisPubSubAsyncCommands<byte[], byte[]> pubSubAsync;

    /**
     * 给定 {@link MessageListener}、{@link StatefulRedisPubSubConnection} 和 {@link LettuceConnectionProvider} 创建一个新的 {@link LettuceSubscription}
     *
     * @param listener           监听通知，不得为 {@literal null}
     * @param pubsubConnection   不得为 {@literal null}
     * @param connectionProvider 不得为 {@literal null}
     */
    protected LettuceSubscription(MessageListener listener, StatefulRedisPubSubConnection<byte[], byte[]> pubsubConnection, LettuceConnectionProvider connectionProvider) {
        super(listener);
        this.connection = pubsubConnection;
        this.listener = new LettuceMessageListener(
                listener,
                listener instanceof SubscriptionListener ? (SubscriptionListener) listener : SubscriptionListener.NO_OP_SUBSCRIPTION_LISTENER
        );
        this.connectionProvider = connectionProvider;
        this.pubsub = connection.sync();
        this.pubSubAsync = connection.async();
        this.connection.addListener(this.listener);
    }

    protected StatefulRedisPubSubConnection<byte[], byte[]> getNativeConnection() {
        return connection;
    }

    @Override
    protected void doClose() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        if (!getChannels().isEmpty()) {
            futures.add(pubSubAsync.unsubscribe().toCompletableFuture());
        }
        if (!getPatterns().isEmpty()) {
            futures.add(pubSubAsync.punsubscribe().toCompletableFuture());
        }
        if (!futures.isEmpty()) {
            // this is to ensure completion of the futures and result processing.
            // Since we're unsubscribing first, we expect that we receive pub/sub confirmations before the PING response.
            futures.add(pubSubAsync.ping().toCompletableFuture());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, t) -> {
                connection.removeListener(listener);
            });
        } else {
            connection.removeListener(listener);
        }
        connectionProvider.release(connection);
    }

    @Override
    protected void doPsubscribe(byte[]... patterns) {
        pubsub.psubscribe(patterns);
    }

    @Override
    protected void doPUnsubscribe(boolean all, byte[]... patterns) {
        if (all) {
            pubsub.punsubscribe();
        } else {
            pubsub.punsubscribe(patterns);
        }
    }

    @Override
    protected void doSubscribe(byte[]... channels) {
        pubsub.subscribe(channels);
    }

    @Override
    protected void doUnsubscribe(boolean all, byte[]... channels) {
        if (all) {
            pubsub.unsubscribe();
        } else {
            pubsub.unsubscribe(channels);
        }
    }
}
