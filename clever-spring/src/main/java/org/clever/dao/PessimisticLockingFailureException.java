package org.clever.dao;

/**
 * 悲观锁定冲突引发异常。如果遇到相应的数据库错误，由clever的SQLException转换机制引发。
 *
 * <p>用作更具体异常的超类，如CannotAcquireLockException和DeadlockLoserDataAccessException。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:26 <br/>
 *
 * @see CannotAcquireLockException
 * @see DeadlockLoserDataAccessException
 */
public class PessimisticLockingFailureException extends ConcurrencyFailureException {
    public PessimisticLockingFailureException(String msg) {
        super(msg);
    }

    public PessimisticLockingFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
