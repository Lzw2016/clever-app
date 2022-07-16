package org.clever.core;

/**
 * 获取异常的内嵌信息工具类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:43 <br/>
 */
public abstract class NestedExceptionUtils {
    /**
     * 根据当前异常消息和内部异常对象构建异常信息
     *
     * @param message 当前异常消息
     * @param cause   内部异常对象
     */
    public static String buildMessage(String message, Throwable cause) {
        if (cause == null) {
            return message;
        }
        StringBuilder sb = new StringBuilder(64);
        if (message != null) {
            sb.append(message).append("; ");
        }
        sb.append("nested exception is ").append(cause);
        return sb.toString();
    }

    /**
     * 获取异常的最内层异常(根本原因)，可能返回null
     */
    public static Throwable getRootCause(Throwable original) {
        if (original == null) {
            return null;
        }
        Throwable rootCause = null;
        Throwable cause = original.getCause();
        while (cause != null && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause;
    }

    /**
     * 获取异常的最内层异常(根本原因)，不存在就返回原始异常
     */
    public static Throwable getMostSpecificCause(Throwable original) {
        Throwable rootCause = getRootCause(original);
        return (rootCause != null ? rootCause : original);
    }
}
