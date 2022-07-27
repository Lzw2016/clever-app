package org.clever.core.exception;

/**
 * 非法操作异常
 */
public class IllegalOperationException extends RuntimeException {
    public IllegalOperationException() {
    }

    public IllegalOperationException(String message) {
        super(message);
    }

    public IllegalOperationException(Throwable cause) {
        super(cause);
    }

    public IllegalOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
