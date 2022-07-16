package org.clever.jdbc;

import org.clever.dao.DataAccessResourceFailureException;

import java.sql.SQLException;

/**
 * 无法使用JDBC连接到RDBMS时引发致命异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:51 <br/>
 */
public class CannotGetJdbcConnectionException extends DataAccessResourceFailureException {
    public CannotGetJdbcConnectionException(String msg) {
        super(msg);
    }

    public CannotGetJdbcConnectionException(String msg, SQLException ex) {
        super(msg, ex);
    }
}
