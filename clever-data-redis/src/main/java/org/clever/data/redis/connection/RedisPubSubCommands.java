package org.clever.data.redis.connection;

/**
 * PubSub 特定的 Redis 命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:20 <br/>
 */
public interface RedisPubSubCommands {
    /**
     * 指示当前连接是否已订阅（至少订阅一个频道）
     *
     * @return 如果连接已订阅，则为 true，否则为 false
     */
    boolean isSubscribed();

    /**
     * 返回此连接的当前订阅；如果未订阅连接，则返回 null
     *
     * @return 当前订阅，{@literal null} 如果没有可用
     */
    Subscription getSubscription();

    /**
     * 将给定的消息发布到给定的频道
     *
     * @param channel 要发布到的频道。 不能是 {@literal null}
     * @param message 要发布的消息。 不能是 {@literal null}
     * @return 收到消息的客户端数量或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/publish">Redis 文档: PUBLISH</a>
     */
    Long publish(byte[] channel, byte[] message);

    /**
     * 订阅给定频道的连接。
     * 订阅后，连接进入监听模式，只能订阅其他频道或取消订阅。
     * 在取消订阅连接之前，不会接受其他命令。
     * <p>
     * 请注意，此操作是阻塞的，当前线程立即开始等待新消息。
     *
     * @param listener 消息监听器，不能是 {@literal null}
     * @param channels 频道名称，不能是 {@literal null}
     * @see <a href="https://redis.io/commands/subscribe">Redis 文档: SUBSCRIBE</a>
     */
    void subscribe(MessageListener listener, byte[]... channels);

    /**
     * 订阅与给定模式匹配的所有频道的连接。
     * 订阅后，连接进入监听模式，只能订阅其他频道或取消订阅。
     * 在取消订阅连接之前，不会接受其他命令。
     * <p>
     * 请注意，此操作是阻塞的，当前线程立即开始等待新消息。
     *
     * @param listener 消息监听器, 不能是 {@literal null}
     * @param patterns 频道名称模式, 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/psubscribe">Redis 文档: PSUBSCRIBE</a>
     */
    void pSubscribe(MessageListener listener, byte[]... patterns);
}
