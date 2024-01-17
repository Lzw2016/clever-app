package org.clever.groovy.rule;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/06/26 19:41 <br/>
 */
public class RuleException extends RuntimeException {
    public RuleException() {
        super();
    }

    /**
     * @param message 异常信息
     */
    public RuleException(String message) {
        super(message);
    }

    /**
     * @param message 异常信息
     * @param cause   异常
     */
    public RuleException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause 异常信息
     */
    public RuleException(Throwable cause) {
        super(cause);
    }
}
