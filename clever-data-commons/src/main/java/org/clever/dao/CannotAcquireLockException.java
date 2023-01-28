package org.clever.dao;

/**
 * 在更新期间，例如在“select for update”语句期间，未能获取锁时引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:25 <br/>
 */
public class CannotAcquireLockException extends PessimisticLockingFailureException {
    public CannotAcquireLockException(String msg) {
        super(msg);
    }

    public CannotAcquireLockException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
