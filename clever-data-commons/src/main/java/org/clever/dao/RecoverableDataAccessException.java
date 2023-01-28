package org.clever.dao;

/**
 * 如果应用程序执行一些恢复步骤并重试整个事务，或者在分布式事务的情况下，事务分支，
 * 则当以前失败的操作可能成功时引发数据访问异常。恢复操作至少必须包括关闭当前连接和获取新连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:13 <br/>
 *
 * @see java.sql.SQLRecoverableException
 */
public class RecoverableDataAccessException extends DataAccessException {
    public RecoverableDataAccessException(String msg) {
        super(msg);
    }

    public RecoverableDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
