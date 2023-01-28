package org.clever.core.serializer.support;

import org.clever.core.NestedRuntimeException;
import org.clever.core.serializer.Deserializer;
import org.clever.core.serializer.Serializer;

/**
 * 当 {@link Serializer} 或 {@link Deserializer} 失败时，本机IOException（或类似）的包装器。
 * 由 {@link SerializingConverter} 和 {@link DeserializingConverter} 引发
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:49 <br/>
 */
public class SerializationFailedException extends NestedRuntimeException {
    /**
     * 使用指定的详细信息消息构造 {@code SerializationException}
     *
     * @param message 详细信息
     */
    public SerializationFailedException(String message) {
        super(message);
    }

    /**
     * 使用指定的详细信息消息和嵌套异常构造 {@code SerializationException}
     *
     * @param message 详细信息
     * @param cause   嵌套异常
     */
    public SerializationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
