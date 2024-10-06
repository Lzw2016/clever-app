package org.clever.data.jdbc.support;

import lombok.Getter;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Objects;

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
     * 是否启用收集SQLWarning输出(支持Oracle的dbms_output输出)
     */
    private final ThreadLocal<Boolean> enableSqlWarning;
    /**
     * 收集SQLWarning输出的数据缓冲区
     */
    private final ThreadLocal<StringBuilder> sqlWarningBuffer;

    /**
     * 被包装的 JdbcTemplate 对象
     */
    @Getter
    private JdbcTemplate target = this;

    public JdbcTemplateWrapper(ThreadLocal<Boolean> enableSqlWarning, ThreadLocal<StringBuilder> sqlWarningBuffer) {
        super();
        Assert.notNull(enableSqlWarning, "参数 enableSqlWarning 不能为空");
        Assert.notNull(sqlWarningBuffer, "参数 sqlWarningBuffer 不能为空");
        this.enableSqlWarning = enableSqlWarning;
        this.sqlWarningBuffer = sqlWarningBuffer;
    }

    public JdbcTemplateWrapper(DataSource dataSource, ThreadLocal<Boolean> enableSqlWarning, ThreadLocal<StringBuilder> sqlWarningBuffer) {
        super(dataSource);
        Assert.notNull(enableSqlWarning, "参数 enableSqlWarning 不能为空");
        Assert.notNull(sqlWarningBuffer, "参数 sqlWarningBuffer 不能为空");
        this.enableSqlWarning = enableSqlWarning;
        this.sqlWarningBuffer = sqlWarningBuffer;
    }

    public JdbcTemplateWrapper(DataSource dataSource, boolean lazyInit, ThreadLocal<Boolean> enableSqlWarning, ThreadLocal<StringBuilder> sqlWarningBuffer) {
        super(dataSource, lazyInit);
        Assert.notNull(enableSqlWarning, "参数 enableSqlWarning 不能为空");
        Assert.notNull(sqlWarningBuffer, "参数 sqlWarningBuffer 不能为空");
        this.enableSqlWarning = enableSqlWarning;
        this.sqlWarningBuffer = sqlWarningBuffer;
    }

    public JdbcTemplateWrapper(JdbcTemplate jdbcTemplate, ThreadLocal<Boolean> enableSqlWarning, ThreadLocal<StringBuilder> sqlWarningBuffer) {
        super();
        Assert.notNull(enableSqlWarning, "参数 enableSqlWarning 不能为空");
        Assert.notNull(sqlWarningBuffer, "参数 sqlWarningBuffer 不能为空");
        this.enableSqlWarning = enableSqlWarning;
        this.sqlWarningBuffer = sqlWarningBuffer;
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
        SQLWarning warningToLog = stmt.getWarnings();
        if (isIgnoreWarnings()) {
            // 自定义处理 SQLWarning
            if (Objects.equals(enableSqlWarning.get(), true)) {
                StringBuilder output = sqlWarningBuffer.get();
                if (output == null) {
                    synchronized (sqlWarningBuffer) {
                        output = sqlWarningBuffer.get();
                        if (output == null) {
                            output = new StringBuilder();
                            sqlWarningBuffer.set(output);
                        }
                    }
                }
                while (warningToLog != null) {
                    String message = warningToLog.getMessage();
                    if (message != null) {
                        output.append(message);
                        if (!message.endsWith("\n")) {
                            output.append("\n");
                        }
                    }
                    warningToLog = warningToLog.getNextWarning();
                }
            } else if (logger.isInfoEnabled()) {
                while (warningToLog != null) {
                    logger.info("SQLWarning: SQL state '" +
                        warningToLog.getSQLState() + "', error code '" +
                        warningToLog.getErrorCode() + "', message [" +
                        warningToLog.getMessage() + "]"
                    );
                    warningToLog = warningToLog.getNextWarning();
                }
            }
        } else {
            handleWarnings(warningToLog);
        }
    }

    @Override
    protected void handleWarnings(SQLWarning warning) throws SQLWarningException {
        if (warning != null) {
            throw new SQLWarningException("Warning not ignored", warning);
        }
    }
}
