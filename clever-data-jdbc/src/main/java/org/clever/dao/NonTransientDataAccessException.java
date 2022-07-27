package org.clever.dao;

/**
 * 被视为非暂时性的数据访问异常层次结构的根，其中，除非纠正异常原因，否则重试相同操作将失败
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:49 <br/>
 *
 * @see java.sql.SQLNonTransientException
 */
public abstract class NonTransientDataAccessException extends DataAccessException {
    public NonTransientDataAccessException(String msg) {
        super(msg);
    }

    public NonTransientDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
