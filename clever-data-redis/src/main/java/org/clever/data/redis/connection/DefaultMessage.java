package org.clever.data.redis.connection;

import org.clever.util.Assert;

/**
 * 默认消息实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:06 <br/>
 */
public class DefaultMessage implements Message {
    private static final byte[] EMPTY = new byte[0];

    private final byte[] channel;
    private final byte[] body;
    private String toString;

    public DefaultMessage(byte[] channel, byte[] body) {
        Assert.notNull(channel, "Channel must not be null!");
        Assert.notNull(body, "Body must not be null!");
        this.body = body;
        this.channel = channel;
    }

    @Override
    public byte[] getChannel() {
        return channel.length == 0 ? EMPTY : channel.clone();
    }

    @Override
    public byte[] getBody() {
        return body.length == 0 ? EMPTY : body.clone();
    }

    @Override
    public String toString() {
        if (toString == null) {
            toString = new String(body);
        }
        return toString;
    }
}
