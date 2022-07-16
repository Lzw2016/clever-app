package org.clever.jdbc.support;

import org.clever.dao.DataAccessException;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * {@link SQLExceptionTranslator}实现的基类，允许回退到其他{@link SQLExceptionTranslator}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 21:44 <br/>
 */
public abstract class AbstractFallbackSQLExceptionTranslator implements SQLExceptionTranslator {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private SQLExceptionTranslator fallbackTranslator;

    /**
     * 重写默认SQL状态回退转换器 (通常为 {@link SQLStateSQLExceptionTranslator}).
     */
    public void setFallbackTranslator(SQLExceptionTranslator fallback) {
        this.fallbackTranslator = fallback;
    }

    /**
     * 返回回退异常转换器（如果有）
     */
    public SQLExceptionTranslator getFallbackTranslator() {
        return this.fallbackTranslator;
    }

    /**
     * 预先检查参数，调用{@link #doTranslate}，并在必要时调用{@link #getFallbackTranslator() 回退转换器}。
     */
    @Override
    public DataAccessException translate(String task, String sql, SQLException ex) {
        Assert.notNull(ex, "Cannot translate a null SQLException");
        DataAccessException dae = doTranslate(task, sql, ex);
        if (dae != null) {
            // Specific exception match found.
            return dae;
        }
        // Looking for a fallback...
        SQLExceptionTranslator fallback = getFallbackTranslator();
        if (fallback != null) {
            return fallback.translate(task, sql, ex);
        }
        return null;
    }

    /**
     * 用于实际转换给定异常的模板方法。
     * <p>传入的参数将被预先检查。此外，允许此方法返回null，以指示未找到异常匹配，并且应启动回退转换。
     *
     * @param task 描述正在尝试的任务的可读文本
     * @param sql  导致问题的SQL查询或更新（如果已知）
     * @param ex   {@code SQLException}
     * @return DataAccessException，包装{@code SQLException}；如果未找到异常匹配项，则为null
     */
    protected abstract DataAccessException doTranslate(String task, String sql, SQLException ex);

    /**
     * 为给定的{@link java.sql.SQLException}生成消息字符串。
     * 在创建泛型{@link org.clever.dao.DataAccessException}类的实例时由translator子类调用。
     *
     * @param task 描述正在尝试的任务的可读文本
     * @param sql  导致问题的SQL语句
     * @param ex   {@code SQLException}
     * @return 要使用的消息字符串
     */
    protected String buildMessage(String task, String sql, SQLException ex) {
        return task + "; " + (sql != null ? ("SQL [" + sql + "]; ") : "") + ex.getMessage();
    }
}
