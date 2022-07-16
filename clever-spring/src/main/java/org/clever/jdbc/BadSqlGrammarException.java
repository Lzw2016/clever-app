package org.clever.jdbc;

import org.clever.dao.InvalidDataAccessResourceUsageException;

import java.sql.SQLException;

/**
 * 指定的SQL无效时引发异常。这样的异常总是有一个{@code java.sql.SQLException}根本原因。
 *
 * <p>可能没有这样的表、列等的子类。自定义SQLExceptionTranslator可以创建这样更具体的异常，
 * 而不会影响使用此类的代码。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:07 <br/>
 *
 * @see InvalidResultSetAccessException
 */
public class BadSqlGrammarException extends InvalidDataAccessResourceUsageException {
    private final String sql;

    public BadSqlGrammarException(String task, String sql, SQLException ex) {
        super(task + "; bad SQL grammar [" + sql + "]", ex);
        this.sql = sql;
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
