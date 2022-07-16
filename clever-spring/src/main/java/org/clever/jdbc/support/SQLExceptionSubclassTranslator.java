package org.clever.jdbc.support;

import org.clever.dao.*;
import org.clever.jdbc.BadSqlGrammarException;

import java.sql.*;

/**
 * {@link SQLExceptionTranslator}实现，分析JDBC驱动程序抛出的特定{@link java.sql.SQLException}子类。
 *
 * <p>如果JDBC驱动程序实际上没有公开符合JDBC 4的{@code SQLException}子类，
 * 则返回到标准的{@link SQLStateSQLExceptionTranslator}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:05 <br/>
 *
 * @see java.sql.SQLTransientException
 * @see java.sql.SQLTransientException
 * @see java.sql.SQLRecoverableException
 */
public class SQLExceptionSubclassTranslator extends AbstractFallbackSQLExceptionTranslator {
    public SQLExceptionSubclassTranslator() {
        setFallbackTranslator(new SQLStateSQLExceptionTranslator());
    }

    @Override
    protected DataAccessException doTranslate(String task, String sql, SQLException ex) {
        if (ex instanceof SQLTransientException) {
            if (ex instanceof SQLTransientConnectionException) {
                return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
            } else if (ex instanceof SQLTransactionRollbackException) {
                return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
            } else if (ex instanceof SQLTimeoutException) {
                return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
            }
        } else if (ex instanceof SQLNonTransientException) {
            if (ex instanceof SQLNonTransientConnectionException) {
                return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
            } else if (ex instanceof SQLDataException) {
                return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
            } else if (ex instanceof SQLIntegrityConstraintViolationException) {
                return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
            } else if (ex instanceof SQLInvalidAuthorizationSpecException) {
                return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
            } else if (ex instanceof SQLSyntaxErrorException) {
                return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
            } else if (ex instanceof SQLFeatureNotSupportedException) {
                return new InvalidDataAccessApiUsageException(buildMessage(task, sql, ex), ex);
            }
        } else if (ex instanceof SQLRecoverableException) {
            return new RecoverableDataAccessException(buildMessage(task, sql, ex), ex);
        }
        // Fallback to clever's own SQL state translation...
        return null;
    }
}
