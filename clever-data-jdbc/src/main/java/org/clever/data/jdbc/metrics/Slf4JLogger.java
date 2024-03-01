package org.clever.data.jdbc.metrics;

import com.p6spy.engine.logging.Category;
import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.data.jdbc.p6spy.P6SpyFormatter;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * 自定义 p6spy 日志输出
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/03/17 16:39 <br/>
 */
public class Slf4JLogger extends com.p6spy.engine.spy.appender.Slf4JLogger {
    private static final Logger log = LoggerFactory.getLogger("p6spy.ignore");
    private static final Set<String> FORCE_LOG_CATEGORY = new HashSet<>();
    private static volatile JdbcConfig.P6SpyLog LOG_CONFIG;
    private static volatile JdbcConfig.JdbcMetrics METRICS_CONFIG;
    private static volatile JdbcMetrics JDBC_METRICS;
    private static volatile boolean ENABLE_JDBC_METRICS = false;

    static {
        FORCE_LOG_CATEGORY.add(Category.ERROR.getName());
        FORCE_LOG_CATEGORY.add(Category.WARN.getName());
        FORCE_LOG_CATEGORY.add(Category.COMMIT.getName());
        FORCE_LOG_CATEGORY.add(Category.ROLLBACK.getName());
        FORCE_LOG_CATEGORY.add(Category.OUTAGE.getName());
    }

    /**
     * 配置初始化
     */
    public static void init(JdbcConfig.P6SpyLog logConfig, JdbcConfig.JdbcMetrics metricsConfig) {
        Assert.notNull(logConfig, "参数 logConfig 不能为 null");
        Assert.notNull(metricsConfig, "参数 metricsConfig 不能为 null");
        LOG_CONFIG = logConfig;
        METRICS_CONFIG = metricsConfig;
        JDBC_METRICS = new JdbcMetrics(metricsConfig);
        ENABLE_JDBC_METRICS = JDBC_METRICS != null
            && JDBC_METRICS.getConfig() != null
            && JDBC_METRICS.getConfig().isEnable();
    }

    private volatile boolean formatterInitialized = false;

    private void initFormatter() {
        if (formatterInitialized) {
            return;
        }
        formatterInitialized = true;
        if (LOG_CONFIG != null) {
            if (strategy instanceof P6SpyFormatter) {
                P6SpyFormatter formatter = (P6SpyFormatter) strategy;
                formatter.setSlow(LOG_CONFIG.getSlow());
            }
        }
    }

    /**
     * @param connectionId 连接数据库的id
     * @param now          当前time的毫秒数
     * @param elapsed      操作完成所需的时间(以毫秒为单位)
     * @param category     操作的类别
     * @param prepared     预编译的sql语句
     * @param sql          执行的sql语句
     * @param url          执行sql语句的数据库url
     */
    @Override
    public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql, String url) {
        // 初始化 Formatter
        initFormatter();
        // 统计 sql 执行性能
        if (ENABLE_JDBC_METRICS && StringUtils.isNotBlank(prepared)) {
            SqlExecEvent sqlExecEvent = new SqlExecEvent(url, prepared, sql, elapsed);
            JDBC_METRICS.addSqlExecEvent(sqlExecEvent);
        }
        // 不启用 sql 日志
        if (!LOG_CONFIG.isEnable()) {
            return;
        }
        // 强制打印 sql 日志
        if (FORCE_LOG_CATEGORY.contains(category.getName())) {
            super.logSQL(connectionId, now, elapsed, category, prepared, sql, url);
            return;
        }
        // 判断是否需要忽略打印SQL
        boolean ignore = false;
        if (LOG_CONFIG != null && StringUtils.isNotBlank(prepared)) {
            String preparedSql = SqlLoggerUtils.deleteWhitespace(prepared);
            if (LOG_CONFIG.getIgnoreSql() != null && LOG_CONFIG.getIgnoreSql().contains(preparedSql)) {
                // 忽略的SQL语句(完整匹配，大小写敏感)
                ignore = true;
            } else if (LOG_CONFIG.getIgnoreContainsSql() != null && LOG_CONFIG.getIgnoreContainsSql().stream().anyMatch(preparedSql::contains)) {
                // 忽略的SQL语句(包含匹配，大小写敏感)
                ignore = true;
            }
        }
        // 忽略打印SQL，输出DEBUG级别日志
        if (ignore) {
            if (log.isDebugEnabled()) {
                String msg = this.strategy.formatMessage(connectionId, now, elapsed, category.toString(), prepared, sql, url);
                log.debug(msg);
            }
            return;
        }
        // 打印 sql 日志
        super.logSQL(connectionId, now, elapsed, category, prepared, sql, url);
    }
}
