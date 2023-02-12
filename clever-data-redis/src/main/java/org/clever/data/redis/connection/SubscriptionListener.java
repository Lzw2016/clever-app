package org.clever.data.redis.connection;

/**
 * 订阅通知的侦听器。
 * <p>
 * Redis 报告订阅通知，作为对频道和模式的订阅和取消订阅操作的确认。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:04 <br/>
 */
public interface SubscriptionListener {
    /**
     * 空 {@link SubscriptionListener}
     */
    SubscriptionListener NO_OP_SUBSCRIPTION_LISTENER = new SubscriptionListener() {
    };

    /**
     * Redis 确认频道订阅时的通知
     *
     * @param channel 频道名称
     * @param count   订户数
     */
    default void onChannelSubscribed(byte[] channel, long count) {
    }

    /**
     * Redis 确认频道取消订阅时的通知
     *
     * @param channel 频道名称
     * @param count   订户数
     */
    default void onChannelUnsubscribed(byte[] channel, long count) {
    }

    /**
     * Redis 确认模式订阅时的通知
     *
     * @param pattern 模式
     * @param count   订户数
     */
    default void onPatternSubscribed(byte[] pattern, long count) {
    }

    /**
     * Redis 确认模式取消订阅时的通知
     *
     * @param pattern 模式
     * @param count   订户数
     */
    default void onPatternUnsubscribed(byte[] pattern, long count) {
    }
}
