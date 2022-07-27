package org.clever.jdbc;

import org.clever.dao.UncategorizedDataAccessException;

import java.sql.SQLWarning;

/**
 * 当我们不忽略{@link SQLWarning}时引发异常。
 *
 * <p>如果报告了SQLWarning，则操作已完成，因此，如果我们在查看警告时不满意，则需要显式回滚它。
 * 我们可以选择忽略（并记录）警告，或者将其包装并以这个SQLWarningException的形式抛出。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:11 <br/>
 *
 * @see org.clever.jdbc.core.JdbcTemplate#setIgnoreWarnings
 */
public class SQLWarningException extends UncategorizedDataAccessException {
    public SQLWarningException(String msg, SQLWarning ex) {
        super(msg, ex);
    }

    /**
     * 返回基础 SQLWarning
     */
    public SQLWarning SQLWarning() {
        return (SQLWarning) getCause();
    }
}
