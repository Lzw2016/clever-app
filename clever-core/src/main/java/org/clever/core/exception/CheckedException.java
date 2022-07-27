package org.clever.core.exception;

/**
 * 受检查异常
 */
public class CheckedException extends Exception {
    private final transient Object[] parameters;

    /**
     * 构造器
     *
     * @param message    异常信息
     * @param parameters parameters
     */
    public CheckedException(String message, Object... parameters) {
        super(message);
        this.parameters = parameters;
    }

    public CheckedException(Throwable cause, Object... parameters) {
        super(cause);
        this.parameters = parameters;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
