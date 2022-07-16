package org.clever.core;

/**
 * 内嵌的运行时异常，包装真实的异常信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:42 <br/>
 */
public abstract class NestedRuntimeException extends RuntimeException {
    static {
        // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
        // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
        // noinspection ResultOfMethodCallIgnored
        NestedExceptionUtils.class.getName();
    }

    /**
     * 创建一个 NestedRuntimeException
     *
     * @param msg 异常信息
     */
    public NestedRuntimeException(String msg) {
        super(msg);
    }

    /**
     * 创建一个 NestedRuntimeException
     *
     * @param msg   异常信息
     * @param cause 内部异常对象
     */
    public NestedRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public String getMessage() {
        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
    }

    /**
     * 获取异常的最内层异常(根本原因)
     */
    public Throwable getRootCause() {
        return NestedExceptionUtils.getRootCause(this);
    }

    /**
     * 获取异常的最内层异常(根本原因)，不存在就返回原始异常
     */
    public Throwable getMostSpecificCause() {
        Throwable rootCause = getRootCause();
        return (rootCause != null ? rootCause : this);
    }

    /**
     * 检查当前异常是否包含给定的异常类型
     *
     * @param exType 给定的异常类型
     */
    public boolean contains(Class<?> exType) {
        if (exType == null) {
            return false;
        }
        if (exType.isInstance(this)) {
            return true;
        }
        Throwable cause = getCause();
        if (cause == this) {
            return false;
        }
        if (cause instanceof NestedRuntimeException) {
            return ((NestedRuntimeException) cause).contains(exType);
        } else {
            while (cause != null) {
                if (exType.isInstance(cause)) {
                    return true;
                }
                if (cause.getCause() == cause) {
                    break;
                }
                cause = cause.getCause();
            }
            return false;
        }
    }
}
