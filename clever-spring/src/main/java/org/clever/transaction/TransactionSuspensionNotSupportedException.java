package org.clever.transaction;

/**
 * 尝试挂起现有事务时引发异常，但基础后端不支持事务挂起。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:44 <br/>
 */
public class TransactionSuspensionNotSupportedException extends CannotCreateTransactionException {
    public TransactionSuspensionNotSupportedException(String msg) {
        super(msg);
    }

    public TransactionSuspensionNotSupportedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
