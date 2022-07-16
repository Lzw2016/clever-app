package org.clever.core;

/**
 * 便捷类，用于将已检查的异常包装为根本原因
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:27 <br/>
 *
 * @see NestedRuntimeException
 */
public abstract class NestedCheckedException extends Exception {
    static {
        // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
        // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
        // noinspection ResultOfMethodCallIgnored
        NestedExceptionUtils.class.getName();
    }

    public NestedCheckedException(String msg) {
        super(msg);
    }

    public NestedCheckedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public String getMessage() {
        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
    }

    /**
     * 获取异常的最内部原因（如果有）。
     */
    public Throwable getRootCause() {
        return NestedExceptionUtils.getRootCause(this);
    }

    /**
     * 检索此异常的最具体原因，即最内部原因（根本原因）或此异常本身
     */
    public Throwable getMostSpecificCause() {
        Throwable rootCause = getRootCause();
        return (rootCause != null ? rootCause : this);
    }

    /**
     * 检查此异常是否包含给定类型的异常：要么属于给定类本身，要么包含给定类型的嵌套原因
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
        if (cause instanceof NestedCheckedException) {
            return ((NestedCheckedException) cause).contains(exType);
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
