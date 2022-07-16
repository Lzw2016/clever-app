package org.clever.core.convert;

import org.clever.core.NestedRuntimeException;

/**
 * 转换系统引发的异常的基类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 15:18 <br/>
 */
public abstract class ConversionException extends NestedRuntimeException {
    /**
     * @param message 异常消息
     */
    public ConversionException(String message) {
        super(message);
    }

    /**
     * @param message 异常消息
     * @param cause   原始异常
     */
    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}