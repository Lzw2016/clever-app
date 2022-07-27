package org.clever.jdbc.support;

import org.clever.dao.*;
import org.clever.jdbc.BadSqlGrammarException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link SQLExceptionTranslator}实现，它基于前两位数字（SQL状态“类”）分析{@link SQLException}中的SQL状态。
 * 检测标准SQL状态值和已知的特定于供应商的SQL状态。
 *
 * <p>无法诊断所有问题，但可在数据库之间移植，不需要特殊初始化（无数据库供应商检测等）。
 * 要获得更精确的翻译，请考虑{@link SQLErrorCodeSQLExceptionTranslator}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:06 <br/>
 *
 * @see SQLException#getSQLState()
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class SQLStateSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {
    private static final Set<String> BAD_SQL_GRAMMAR_CODES = new HashSet<>(8);
    private static final Set<String> DATA_INTEGRITY_VIOLATION_CODES = new HashSet<>(8);
    private static final Set<String> DATA_ACCESS_RESOURCE_FAILURE_CODES = new HashSet<>(8);
    private static final Set<String> TRANSIENT_DATA_ACCESS_RESOURCE_CODES = new HashSet<>(8);
    private static final Set<String> CONCURRENCY_FAILURE_CODES = new HashSet<>(4);

    static {
        BAD_SQL_GRAMMAR_CODES.add("07");                // Dynamic SQL error
        BAD_SQL_GRAMMAR_CODES.add("21");                // Cardinality violation
        BAD_SQL_GRAMMAR_CODES.add("2A");                // Syntax error direct SQL
        BAD_SQL_GRAMMAR_CODES.add("37");                // Syntax error dynamic SQL
        BAD_SQL_GRAMMAR_CODES.add("42");                // General SQL syntax error
        BAD_SQL_GRAMMAR_CODES.add("65");                // Oracle: unknown identifier

        DATA_INTEGRITY_VIOLATION_CODES.add("01");       // Data truncation
        DATA_INTEGRITY_VIOLATION_CODES.add("02");       // No data found
        DATA_INTEGRITY_VIOLATION_CODES.add("22");       // Value out of range
        DATA_INTEGRITY_VIOLATION_CODES.add("23");       // Integrity constraint violation
        DATA_INTEGRITY_VIOLATION_CODES.add("27");       // Triggered data change violation
        DATA_INTEGRITY_VIOLATION_CODES.add("44");       // With check violation

        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("08");   // Connection exception
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("53");   // PostgreSQL: insufficient resources (e.g. disk full)
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("54");   // PostgreSQL: program limit exceeded (e.g. statement too complex)
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("57");   // DB2: out-of-memory exception / database not started
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("58");   // DB2: unexpected system error

        TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JW"); // Sybase: internal I/O error
        TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JZ"); // Sybase: unexpected I/O error
        TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("S1"); // DB2: communication failure

        CONCURRENCY_FAILURE_CODES.add("40");            // Transaction rollback
        CONCURRENCY_FAILURE_CODES.add("61");            // Oracle: deadlock
    }

    @Override
    protected DataAccessException doTranslate(String task, String sql, SQLException ex) {
        // First, the getSQLState check...
        String sqlState = getSqlState(ex);
        if (sqlState != null && sqlState.length() >= 2) {
            String classCode = sqlState.substring(0, 2);
            if (logger.isDebugEnabled()) {
                logger.debug("Extracted SQL state class '" + classCode + "' from value '" + sqlState + "'");
            }
            if (BAD_SQL_GRAMMAR_CODES.contains(classCode)) {
                return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
            } else if (DATA_INTEGRITY_VIOLATION_CODES.contains(classCode)) {
                return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
            } else if (DATA_ACCESS_RESOURCE_FAILURE_CODES.contains(classCode)) {
                return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
            } else if (TRANSIENT_DATA_ACCESS_RESOURCE_CODES.contains(classCode)) {
                return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
            } else if (CONCURRENCY_FAILURE_CODES.contains(classCode)) {
                return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
            }
        }
        // For MySQL: exception class name indicating a timeout?
        // (since MySQL doesn't throw the JDBC 4 SQLTimeoutException)
        if (ex.getClass().getName().contains("Timeout")) {
            return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
        }
        // Couldn't resolve anything proper - resort to UncategorizedSQLException.
        return null;
    }

    /**
     * 从提供的{@link SQLException 异常}中获取SQL状态代码。
     * <p>一些JDBC驱动程序嵌套了成批更新中的实际异常，因此我们可能需要深入研究嵌套的异常。
     *
     * @param ex 要从中提取{@link SQLException#getSQLState() SQL状态}的异常
     * @return SQL状态代码
     */
    private String getSqlState(SQLException ex) {
        String sqlState = ex.getSQLState();
        if (sqlState == null) {
            SQLException nestedEx = ex.getNextException();
            if (nestedEx != null) {
                sqlState = nestedEx.getSQLState();
            }
        }
        return sqlState;
    }
}
