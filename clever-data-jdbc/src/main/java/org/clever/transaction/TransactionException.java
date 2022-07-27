package org.clever.transaction;

import org.clever.core.NestedRuntimeException;

/**
 * 所有事务异常的超类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:14 <br/>
 */
public abstract class TransactionException extends NestedRuntimeException {
    /**
     * 事务异常的构造函数。
     *
     * @param msg 详细信息
     */
    public TransactionException(String msg) {
        super(msg);
    }

    /**
     * 事务异常的构造函数。
     *
     * @param msg   详细信息
     * @param cause 使用中的事务API的根本原因
     */
    public TransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
