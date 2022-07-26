package org.clever.core;

/**
 * 组件初始化异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/26 18:00 <br/>
 */
public class InitializerException extends RuntimeException {
    public InitializerException() {
        super();
    }

    public InitializerException(String message) {
        super(message);
    }

    public InitializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InitializerException(Throwable cause) {
        super(cause);
    }

    protected InitializerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
