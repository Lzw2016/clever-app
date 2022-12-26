package org.clever.data.jdbc.metrics;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.data.jdbc.support.P6SpyLogger;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/03/14 19:48 <br/>
 */
public class P6SpyMeter implements MessageFormattingStrategy {
    private static final P6SpyLogger P6SPY_LOGGER = new P6SpyLogger();

    private static volatile JdbcMetrics JDBC_METRICS;
    private static volatile boolean ENABLE_JDBC_METRICS = false;

    /**
     * 初始化 JdbcMetrics 配置
     */
    public static void init(JdbcConfig.JdbcMetrics config) {
        Assert.notNull(config, "参数 config 不能为 null");
        JDBC_METRICS = new JdbcMetrics(config);
        ENABLE_JDBC_METRICS = JDBC_METRICS != null
                && JDBC_METRICS.getConfig() != null
                && JDBC_METRICS.getConfig().isEnable();
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (ENABLE_JDBC_METRICS && StringUtils.isNotBlank(prepared)) {
            SqlExecEvent sqlExecEvent = new SqlExecEvent(url, prepared, sql, elapsed);
            JDBC_METRICS.addSqlExecEvent(sqlExecEvent);
        }
        return P6SPY_LOGGER.formatMessage(connectionId, now, elapsed, category, prepared, sql, url);
    }
}
