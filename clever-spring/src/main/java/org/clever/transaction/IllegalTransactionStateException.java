package org.clever.transaction;

/**
 * 根据应用的事务传播行为，当事务的存在或不存在达到非法状态时引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:25 <br/>
 */
public class IllegalTransactionStateException extends TransactionUsageException {
    /**
     * IllegalTransactionStateException的构造函数。
     *
     * @param msg 详细信息
     */
    public IllegalTransactionStateException(String msg) {
        super(msg);
    }

    /**
     * IllegalTransactionStateException的构造函数。
     *
     * @param msg   详细信息
     * @param cause 使用中的事务API的根本原因
     */
    public IllegalTransactionStateException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
