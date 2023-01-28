package org.clever.dao;

/**
 * 被认为是暂时性的数据访问异常层次结构的根，其中以前失败的操作在无需应用程序级功能干预的情况下重试时可能会成功。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:09 <br/>
 *
 * @see java.sql.SQLTransientException
 */
public abstract class TransientDataAccessException extends DataAccessException {
    public TransientDataAccessException(String msg) {
        super(msg);
    }

    public TransientDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
