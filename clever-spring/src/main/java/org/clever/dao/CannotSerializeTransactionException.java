package org.clever.dao;

/**
 * 由于更新冲突，无法以序列化模式完成事务时引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:27 <br/>
 */
public class CannotSerializeTransactionException extends PessimisticLockingFailureException {
    public CannotSerializeTransactionException(String msg) {
        super(msg);
    }

    public CannotSerializeTransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
