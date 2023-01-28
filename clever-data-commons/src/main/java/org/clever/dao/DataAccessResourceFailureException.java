package org.clever.dao;

/**
 * 资源完全失败时引发的数据访问异常：例如，如果我们无法使用JDBC连接到数据库。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:50 <br/>
 */
public class DataAccessResourceFailureException extends NonTransientDataAccessResourceException {
    public DataAccessResourceFailureException(String msg) {
        super(msg);
    }

    public DataAccessResourceFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
