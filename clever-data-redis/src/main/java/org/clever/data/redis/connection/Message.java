package org.clever.data.redis.connection;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:40 <br/>
 */
public interface Message extends Serializable {
    /**
     * 返回消息的主体（或负载）
     *
     * @return 消息正文。 从不为 {@literal null}
     */
    byte[] getBody();

    /**
     * 返回与消息关联的频道
     *
     * @return 消息通道。 从不为 {@literal null}
     */
    byte[] getChannel();
}
