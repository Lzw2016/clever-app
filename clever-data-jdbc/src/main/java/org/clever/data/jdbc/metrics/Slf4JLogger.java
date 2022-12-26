package org.clever.data.jdbc.metrics;

import com.p6spy.engine.logging.Category;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/03/17 16:39 <br/>
 */
public class Slf4JLogger extends com.p6spy.engine.spy.appender.Slf4JLogger {
    private final Logger log = LoggerFactory.getLogger("p6spy");
    private static volatile JdbcConfig.JdbcMetrics CONFIG;

    /**
     * 初始化
     */
    public static void init(JdbcConfig.JdbcMetrics config) {
        Assert.notNull(config, "参数 config 不能为 null");
        CONFIG = config;
    }

    @Override
    public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql, String url) {
        if (CONFIG != null && CONFIG.getIgnoreSql() != null && CONFIG.getIgnoreSql().contains(prepared)) {
            return;
        }
        if ("category".equals(category.getName())) {
            log.info("outage " + elapsed + "ms " + prepared);
        } else {
            super.logSQL(connectionId, now, elapsed, category, prepared, sql, url);
        }
    }
}
