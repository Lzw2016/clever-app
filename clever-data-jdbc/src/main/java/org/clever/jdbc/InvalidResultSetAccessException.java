package org.clever.jdbc;

import org.clever.dao.InvalidDataAccessResourceUsageException;

import java.sql.SQLException;

/**
 * 以无效方式访问结果集时引发异常。这样的异常总是有一个{@code java.sql.SQLException}根本原因。
 *
 * <p>当指定的ResultSet列索引或名称无效时，通常会发生这种情况。也由断开连接的SqlRowSet引发。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:24 <br/>
 *
 * @see BadSqlGrammarException
 * @see org.clever.jdbc.support.rowset.SqlRowSet
 */
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {
    private final String sql;

    public InvalidResultSetAccessException(String task, String sql, SQLException ex) {
        super(task + "; invalid ResultSet access for SQL [" + sql + "]", ex);
        this.sql = sql;
    }

    public InvalidResultSetAccessException(SQLException ex) {
        super(ex.getMessage(), ex);
        this.sql = null;
    }

    public SQLException getSQLException() {
        return (SQLException) getCause();
    }

    /**
     * 返回导致问题的SQL
     */
    public String getSql() {
        return this.sql;
    }
}
