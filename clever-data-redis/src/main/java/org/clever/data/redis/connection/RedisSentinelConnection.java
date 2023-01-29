package org.clever.data.redis.connection;

import java.io.Closeable;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:33 <br/>
 */
public interface RedisSentinelConnection extends RedisSentinelCommands, Closeable {
    /**
     * @return 如果连接到服务器则为真
     */
    boolean isOpen();
}
