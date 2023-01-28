package org.clever.dao;

/**
 * 当前进程是死锁失败进程，其事务回滚时引发的一般异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:26 <br/>
 */
public class DeadlockLoserDataAccessException extends PessimisticLockingFailureException {
    public DeadlockLoserDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
