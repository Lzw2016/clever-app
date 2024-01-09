package org.clever.core.exception;

/**
 * 断言类型异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/09 10:19 <br/>
 */
public class AssertException extends RuntimeException {
    public AssertException() {
        super();
    }

    public AssertException(String s) {
        super(s);
    }

    public AssertException(Throwable cause) {
        super(cause);
    }

    public AssertException(String message, Throwable cause) {
        super(message, cause);
    }
}
