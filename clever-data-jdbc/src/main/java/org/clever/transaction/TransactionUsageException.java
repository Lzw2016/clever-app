package org.clever.transaction;

/**
 * 由事务API的不当使用导致的异常的超类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:24 <br/>
 */
public class TransactionUsageException extends TransactionException {
    /**
     * TransactionUsageException的构造函数。
     *
     * @param msg 详细信息
     */
    public TransactionUsageException(String msg) {
        super(msg);
    }

    /**
     * TransactionUsageException的构造函数。
     *
     * @param msg   详细信息
     * @param cause 使用中的事务API的根本原因
     */
    public TransactionUsageException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
