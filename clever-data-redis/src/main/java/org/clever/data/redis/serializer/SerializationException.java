package org.clever.data.redis.serializer;

import org.clever.core.NestedRuntimeException;

/**
 * 指示序列化反序列化错误的泛型异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:00 <br/>
 */
public class SerializationException extends NestedRuntimeException {
    /**
     * 构造新的 {@link SerializationException} 实例
     */
    public SerializationException(String msg) {
        super(msg);
    }

    /**
     * 构造新的 {@link SerializationException} 实例
     *
     * @param msg   详细信息
     * @param cause 嵌套异常
     */
    public SerializationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
