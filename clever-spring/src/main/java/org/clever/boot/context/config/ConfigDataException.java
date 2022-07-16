package org.clever.boot.context.config;

/**
 * 配置数据异常的抽象基类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:19 <br/>
 */
public abstract class ConfigDataException extends RuntimeException {
    /**
     * 创建一个新的 {@link ConfigDataException} 实例。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    protected ConfigDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
