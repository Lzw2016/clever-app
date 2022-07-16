package org.clever.dao;

/**
 * 资源暂时失败并且可以重试操作时引发数据访问异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:09 <br/>
 *
 * @see java.sql.SQLTransientConnectionException
 */
public class TransientDataAccessResourceException extends TransientDataAccessException {
    public TransientDataAccessResourceException(String msg) {
        super(msg);
    }

    public TransientDataAccessResourceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
