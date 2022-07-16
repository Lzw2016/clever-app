package org.clever.jdbc;

import org.clever.dao.UncategorizedDataAccessException;

import java.sql.SQLException;

/**
 * 当我们无法将SQLException分类为通用数据访问异常时引发异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:13 <br/>
 */
public class UncategorizedSQLException extends UncategorizedDataAccessException {
    /**
     * 导致问题的SQL
     */
    private final String sql;

    public UncategorizedSQLException(String task, String sql, SQLException ex) {
        super(
                task + "; uncategorized SQLException" +
                        (sql != null ? " for SQL [" + sql + "]" : "") +
                        "; SQL state [" + ex.getSQLState() + "]; error code ["
                        + ex.getErrorCode() + "]; " + ex.getMessage(),
                ex
        );
        this.sql = sql;
    }

    public SQLException getSQLException() {
        return (SQLException) getCause();
    }

    /**
     * 返回导致问题的SQL（如果已知）
     */
    public String getSql() {
        return this.sql;
    }
}
