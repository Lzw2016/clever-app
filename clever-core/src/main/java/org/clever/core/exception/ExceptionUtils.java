package org.clever.core.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/30 19:38 <br/>
 */
public class ExceptionUtils {
    /**
     * 将CheckedException转换为UncheckedException.<br/>
     *
     * @param e 需要try...catch...的异常
     * @return 不需要try...catch...的异常
     */
    public static RuntimeException unchecked(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e);
        }
    }

    /**
     * 将ErrorStack转化为String(获取异常的堆栈信息)<br/>
     *
     * @param e 异常对象
     * @return 异常的堆栈信息
     */
    public static String getStackTraceAsString(Throwable e) {
        if (e == null) {
            return "";
        }
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    /**
     * 判断异常是否由某些底层的异常引起.<br/>
     *
     * @param ex                    异常对象
     * @param causeExceptionClasses 异常类型数组
     * @return 如果异常对象(ex)的内部异常含有异常类型集合(causeExceptionClasses)中的异常类型返回true，否则返回false
     */
    public static boolean isCausedBy(Throwable ex, Collection<Class<?>> causeExceptionClasses) {
        if (causeExceptionClasses == null || causeExceptionClasses.isEmpty()) {
            return false;
        }
        final int maxDepth = 256;
        int depth = 0;
        Throwable cause = ex.getCause();
        while (cause != null) {
            for (Class<?> causeClass : causeExceptionClasses) {
                if (causeClass.isInstance(cause)) {
                    return true;
                }
            }
            if (cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
            depth++;
            if (depth > maxDepth) {
                break;
            }
        }
        return false;
    }

    /**
     * 判断异常是否由某些底层的异常引起.<br/>
     *
     * @param ex                    异常对象
     * @param causeExceptionClasses 异常类型
     * @return 如果异常对象(ex)的内部异常含有异常类型集合(causeExceptionClasses)中的异常类型返回true，否则返回false
     */
    public static boolean isCausedBy(Throwable ex, Class<?>... causeExceptionClasses) {
        if (causeExceptionClasses == null || causeExceptionClasses.length == 0) {
            return false;
        }
        return isCausedBy(ex, Arrays.asList(causeExceptionClasses));
    }

    /**
     * 如果异常是由某些底层的异常引起，返回这个底层异常<br/>
     *
     * @param ex    异常对象
     * @param clazz 异常类型
     * @return 如果异常对象(ex)的内部异常含有异常类型(clazz)则返回这个异常对象，否则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T getCause(Throwable ex, Class<T> clazz) {
        final int maxDepth = 256;
        int depth = 0;
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (clazz.isInstance(cause)) {
                return (T) cause;
            }
            if (cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
            depth++;
            if (depth > maxDepth) {
                break;
            }
        }
        return null;
    }
}
