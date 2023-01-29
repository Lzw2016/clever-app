package org.clever.data.redis.connection;

/**
 * 在 Redis 中发布的消息的侦听器。
 * MessageListener 可以实现 {@link SubscriptionListener} 来接收订阅状态的通知。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:43 <br/>
 */
@FunctionalInterface
public interface MessageListener {
    /**
     * 通过 Redis 处理接收到的对象的回调。
     *
     * @param message message 不能是 {@literal null}
     * @param pattern 与通道匹配的模式（如果指定）- 可以是 {@literal null}。
     */
    void onMessage(Message message, byte[] pattern);
}
