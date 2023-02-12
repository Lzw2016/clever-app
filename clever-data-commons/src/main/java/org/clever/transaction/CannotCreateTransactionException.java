package org.clever.transaction;

/**
 * 无法使用底层事务API（如JTA）创建事务时引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:28 <br/>
 */
public class CannotCreateTransactionException extends TransactionException {
    public CannotCreateTransactionException(String msg) {
        super(msg);
    }

    public CannotCreateTransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
