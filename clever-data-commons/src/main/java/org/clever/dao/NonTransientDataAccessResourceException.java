package org.clever.dao;

/**
 * 当资源完全失败且失败是永久性的时引发数据访问异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:49 <br/>
 *
 * @see java.sql.SQLNonTransientConnectionException
 */
public class NonTransientDataAccessResourceException extends NonTransientDataAccessException {
    public NonTransientDataAccessResourceException(String msg) {
        super(msg);
    }

    public NonTransientDataAccessResourceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
