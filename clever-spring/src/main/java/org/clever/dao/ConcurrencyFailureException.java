package org.clever.dao;

/**
 * 并发失败时引发异常。
 *
 * <p>应将此异常子类化以指示失败的类型：乐观锁定、获取锁失败等。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:11 <br/>
 *
 * @see PessimisticLockingFailureException
 * @see CannotAcquireLockException
 * @see DeadlockLoserDataAccessException
 */
public class ConcurrencyFailureException extends TransientDataAccessException {
    public ConcurrencyFailureException(String msg) {
        super(msg);
    }

    public ConcurrencyFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
