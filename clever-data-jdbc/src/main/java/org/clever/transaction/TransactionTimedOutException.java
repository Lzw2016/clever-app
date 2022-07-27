package org.clever.transaction;

/**
 * 事务超时时引发的异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:54 <br/>
 *
 * @see org.clever.transaction.support.ResourceHolderSupport#getTimeToLiveInMillis
 * @see java.sql.Statement#setQueryTimeout
 * @see java.sql.SQLException
 */
public class TransactionTimedOutException extends TransactionException {
    public TransactionTimedOutException(String msg) {
        super(msg);
    }

    public TransactionTimedOutException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
