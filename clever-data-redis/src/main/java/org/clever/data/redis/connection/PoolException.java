package org.clever.data.redis.connection;

import org.clever.core.NestedRuntimeException;

/**
 * 资源池出现问题时引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:22 <br/>
 */
public class PoolException extends NestedRuntimeException {
    /**
     * 构造一个新的 <code>PoolException</code> 实例。
     *
     * @param msg 详细信息消息
     */
    public PoolException(String msg) {
        super(msg);
    }

    /**
     * 构造一个新的 <code>PoolException</code> 实例
     *
     * @param msg   详细信息消息
     * @param cause 嵌套异常
     */
    public PoolException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
