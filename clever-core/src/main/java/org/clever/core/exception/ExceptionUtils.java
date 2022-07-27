package org.clever.core.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

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
     * @return 如果异常对象(ex)的内部异常含有异常类型数组(causeExceptionClasses)中的异常类型返回true，否则返回false
     */
    @SuppressWarnings("unchecked")
    public static boolean isCausedBy(Throwable ex, Class<? extends Throwable>... causeExceptionClasses) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            for (Class<? extends Throwable> causeClass : causeExceptionClasses) {
                if (causeClass.isInstance(cause)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
