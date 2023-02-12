package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.pubsub.RedisPubSubListener;
import org.clever.data.redis.connection.DefaultMessage;
import org.clever.data.redis.connection.MessageListener;
import org.clever.data.redis.connection.SubscriptionListener;
import org.clever.util.Assert;

/**
 * Lettuce 周围的 MessageListener 包装器 {@link RedisPubSubListener}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:05 <br/>
 */
class LettuceMessageListener implements RedisPubSubListener<byte[], byte[]> {
    private final MessageListener listener;
    private final SubscriptionListener subscriptionListener;

    LettuceMessageListener(MessageListener listener, SubscriptionListener subscriptionListener) {
        Assert.notNull(listener, "MessageListener must not be null!");
        Assert.notNull(subscriptionListener, "SubscriptionListener must not be null!");
        this.listener = listener;
        this.subscriptionListener = subscriptionListener;
    }

    public void message(byte[] channel, byte[] message) {
        listener.onMessage(new DefaultMessage(channel, message), null);
    }

    public void message(byte[] pattern, byte[] channel, byte[] message) {
        listener.onMessage(new DefaultMessage(channel, message), pattern);
    }

    public void subscribed(byte[] channel, long count) {
        subscriptionListener.onChannelSubscribed(channel, count);
    }

    public void psubscribed(byte[] pattern, long count) {
        subscriptionListener.onPatternSubscribed(pattern, count);
    }

    public void unsubscribed(byte[] channel, long count) {
        subscriptionListener.onChannelUnsubscribed(channel, count);
    }

    public void punsubscribed(byte[] pattern, long count) {
        subscriptionListener.onPatternUnsubscribed(pattern, count);
    }
}
