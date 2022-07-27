package org.clever.core.exception;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/01 14:43 <br/>
 */
public class NotImplementedException extends RuntimeException {
    public NotImplementedException() {
        this("NotImplemented");
    }

    public NotImplementedException(String message) {
        super(message);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImplementedException(Throwable cause) {
        super(cause);
    }

    public NotImplementedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
