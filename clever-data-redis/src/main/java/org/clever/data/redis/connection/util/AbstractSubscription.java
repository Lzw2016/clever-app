package org.clever.data.redis.connection.util;

import org.clever.data.redis.connection.MessageListener;
import org.clever.data.redis.connection.RedisInvalidSubscriptionException;
import org.clever.data.redis.connection.Subscription;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 处理通道模式注册的订阅的基本实现，因此子类只需要处理实际的注册注销
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:01 <br/>
 */
public abstract class AbstractSubscription implements Subscription {
    private final Collection<ByteArrayWrapper> channels = new ArrayList<>(2);
    private final Collection<ByteArrayWrapper> patterns = new ArrayList<>(2);
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final MessageListener listener;

    protected AbstractSubscription(MessageListener listener) {
        this(listener, null, null);
    }

    /**
     * 构造一个新的 <code>AbstractSubscription</code> 实例。
     * 允许将频道和模式添加到订阅中以触发订阅操作（因为某些客户端（Jedis）需要在进入收听模式之前进行初始调用）。
     *
     * @param listener 不得为 {@literal null}
     * @param channels 可以是 {@literal null}。
     * @param patterns 可以是 {@literal null}。
     */
    protected AbstractSubscription(MessageListener listener, byte[][] channels, byte[][] patterns) {
        Assert.notNull(listener, "MessageListener must not be null!");
        this.listener = listener;
        synchronized (this.channels) {
            add(this.channels, channels);
        }
        synchronized (this.patterns) {
            add(this.patterns, patterns);
        }
    }

    /**
     * 订阅给定的频道
     *
     * @param channels 要订阅的频道
     */
    protected abstract void doSubscribe(byte[]... channels);

    /**
     * 频道退订
     *
     * @param all      如果所有频道都取消订阅（用作底层实现的提示），则为真
     * @param channels 要取消订阅的频道
     */
    protected abstract void doUnsubscribe(boolean all, byte[]... channels);

    /**
     * 订阅给定的模式
     *
     * @param patterns 要订阅的模式
     */
    protected abstract void doPsubscribe(byte[]... patterns);

    /**
     * 模式退订
     *
     * @param all      如果取消订阅所有模式（用作底层实现的提示），则为真
     * @param patterns 要取消订阅的模式
     */
    protected abstract void doPUnsubscribe(boolean all, byte[]... patterns);

    @Override
    public void close() {
        doClose();
    }

    /**
     * 关闭订阅并释放所有持有的资源
     */
    protected abstract void doClose();

    @Override
    public MessageListener getListener() {
        return listener;
    }

    @Override
    public Collection<byte[]> getChannels() {
        synchronized (channels) {
            return clone(channels);
        }
    }

    @Override
    public Collection<byte[]> getPatterns() {
        synchronized (patterns) {
            return clone(patterns);
        }
    }

    @Override
    public void pSubscribe(byte[]... patterns) {
        checkPulse();
        Assert.notEmpty(patterns, "at least one pattern required");
        synchronized (this.patterns) {
            add(this.patterns, patterns);
        }
        doPsubscribe(patterns);
    }

    @Override
    public void pUnsubscribe() {
        pUnsubscribe((byte[][]) null);
    }

    @Override
    public void subscribe(byte[]... channels) {
        checkPulse();
        Assert.notEmpty(channels, "at least one channel required");
        synchronized (this.channels) {
            add(this.channels, channels);
        }
        doSubscribe(channels);
    }

    @Override
    public void unsubscribe() {
        unsubscribe((byte[][]) null);
    }

    @Override
    public void pUnsubscribe(byte[]... patts) {
        if (!isAlive()) {
            return;
        }
        // shortcut for unsubscribing all patterns
        if (ObjectUtils.isEmpty(patts)) {
            if (!this.patterns.isEmpty()) {
                synchronized (this.patterns) {
                    patts = getPatterns().toArray(new byte[this.patterns.size()][]);
                    doPUnsubscribe(true, patts);
                    this.patterns.clear();
                }
            } else {
                // nothing to unsubscribe from
                return;
            }
        } else {
            doPUnsubscribe(false, patts);
            synchronized (this.patterns) {
                remove(this.patterns, patts);
            }
        }
        closeIfUnsubscribed();
    }

    @Override
    public void unsubscribe(byte[]... chans) {
        if (!isAlive()) {
            return;
        }
        // shortcut for unsubscribing all channels
        if (ObjectUtils.isEmpty(chans)) {
            if (!this.channels.isEmpty()) {
                synchronized (this.channels) {
                    chans = getChannels().toArray(new byte[this.channels.size()][]);
                    doUnsubscribe(true, chans);
                    this.channels.clear();
                }
            } else {
                // nothing to unsubscribe from
                return;
            }
        } else {
            doUnsubscribe(false, chans);
            synchronized (this.channels) {
                remove(this.channels, chans);
            }
        }
        closeIfUnsubscribed();
    }

    @Override
    public boolean isAlive() {
        return alive.get();
    }

    private void checkPulse() {
        if (!isAlive()) {
            throw new RedisInvalidSubscriptionException("Subscription has been unsubscribed and cannot be used anymore");
        }
    }

    private void closeIfUnsubscribed() {
        if (channels.isEmpty() && patterns.isEmpty()) {
            alive.set(false);
            doClose();
        }
    }

    private static Collection<byte[]> clone(Collection<ByteArrayWrapper> col) {
        Collection<byte[]> list = new ArrayList<>(col.size());
        for (ByteArrayWrapper wrapper : col) {
            list.add(wrapper.getArray().clone());
        }
        return list;
    }

    private static void add(Collection<ByteArrayWrapper> col, byte[]... bytes) {
        if (!ObjectUtils.isEmpty(bytes)) {
            for (byte[] bs : bytes) {
                col.add(new ByteArrayWrapper(bs));
            }
        }
    }

    private static void remove(Collection<ByteArrayWrapper> col, byte[]... bytes) {
        if (!ObjectUtils.isEmpty(bytes)) {
            for (byte[] bs : bytes) {
                col.remove(new ByteArrayWrapper(bs));
            }
        }
    }
}
