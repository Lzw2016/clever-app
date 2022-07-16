package org.clever.transaction;

/**
 * 当尝试提交事务导致意外回滚时引发。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:43 <br/>
 */
public class UnexpectedRollbackException extends TransactionException {
    public UnexpectedRollbackException(String msg) {
        super(msg);
    }

    public UnexpectedRollbackException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
