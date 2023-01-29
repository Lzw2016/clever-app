package org.clever.data.redis.connection;

import java.util.Collection;

/**
 * 订阅Redis频道。就像底层的 {@link RedisConnection} 一样，它不应该被多个线程使用。
 * 请注意，一旦订阅失效，它就不能再接受任何订阅。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:37 <br/>
 */
public interface Subscription {
    /**
     * 将给定频道添加到当前订阅
     *
     * @param channels 频道名称。不得为空
     */
    void subscribe(byte[]... channels) throws RedisInvalidSubscriptionException;

    /**
     * 将给定的频道模式添加到当前订阅
     *
     * @param patterns 信道模式。不得为空
     */
    void pSubscribe(byte[]... patterns) throws RedisInvalidSubscriptionException;

    /**
     * 取消按名称指定的所有频道的当前订阅
     */
    void unsubscribe();

    /**
     * 取消所有给定频道的当前订阅
     *
     * @param channels 频道名称。不得为空
     */
    void unsubscribe(byte[]... channels);

    /**
     * 取消模式匹配的所有频道的订阅
     */
    void pUnsubscribe();

    /**
     * 取消与给定模式匹配的所有频道的订阅
     *
     * @param patterns 不能为空
     */
    void pUnsubscribe(byte[]... patterns);

    /**
     * 返回此订阅的（已命名）频道
     *
     * @return 命名频道集合
     */
    Collection<byte[]> getChannels();

    /**
     * 返回此订阅的频道模式
     *
     * @return 频道模式集合
     */
    Collection<byte[]> getPatterns();

    /**
     * 返回用于此订阅的侦听器
     *
     * @return 用于此订阅的侦听器
     */
    MessageListener getListener();

    /**
     * 指示此订阅是否仍然“活动”
     *
     * @return 如果订阅仍然适用，则为true，否则为false
     */
    boolean isAlive();

    /**
     * 关闭订阅并释放所有保留的资源
     */
    void close();
}
