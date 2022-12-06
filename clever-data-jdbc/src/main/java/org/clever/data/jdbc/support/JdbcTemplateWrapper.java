package org.clever.data.jdbc.support;

import lombok.Getter;
import org.clever.jdbc.SQLWarningException;
import org.clever.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * 自定义JdbcTemplate<br/>
 * 1.自定义处理 SQLWarning<br/>
 * 2.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/30 23:58 <br/>
 */
public class JdbcTemplateWrapper extends JdbcTemplate {
    /**
     * 被包装的 JdbcTemplate 对象
     */
    @Getter
    private JdbcTemplate target = this;

    public JdbcTemplateWrapper() {
        super();
    }

    public JdbcTemplateWrapper(DataSource dataSource) {
        super(dataSource);
    }

    public JdbcTemplateWrapper(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }

    public JdbcTemplateWrapper(JdbcTemplate jdbcTemplate) {
        super();
        this.target = jdbcTemplate;
        // 克隆参数 JdbcTemplate 对象的状态信息
        this.setIgnoreWarnings(jdbcTemplate.isIgnoreWarnings());
        this.setFetchSize(jdbcTemplate.getFetchSize());
        this.setMaxRows(jdbcTemplate.getMaxRows());
        this.setQueryTimeout(jdbcTemplate.getQueryTimeout());
        this.setSkipResultsProcessing(jdbcTemplate.isSkipResultsProcessing());
        this.setSkipUndeclaredResults(jdbcTemplate.isSkipUndeclaredResults());
        this.setResultsMapCaseInsensitive(jdbcTemplate.isResultsMapCaseInsensitive());
        this.setDataSource(jdbcTemplate.getDataSource());
        this.setExceptionTranslator(jdbcTemplate.getExceptionTranslator());
        this.setLazyInit(jdbcTemplate.isLazyInit());
    }

    @Override
    protected void handleWarnings(Statement stmt) throws SQLException {
        // TODO SQLWarning
        if (isIgnoreWarnings()) {
            SQLWarning warningToLog = stmt.getWarnings();
            while (warningToLog != null) {
                logger.debug("SQLWarning ignored: SQL state '" +
                        warningToLog.getSQLState() + "', error code '" +
                        warningToLog.getErrorCode() + "', message [" +
                        warningToLog.getMessage() + "]"
                );
                warningToLog = warningToLog.getNextWarning();
            }
        } else {
            handleWarnings(stmt.getWarnings());
        }
    }

    @Override
    protected void handleWarnings(SQLWarning warning) throws SQLWarningException {
        if (warning != null) {
            throw new SQLWarningException("Warning not ignored", warning);
        }
    }
}
