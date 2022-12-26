//package org.clever.data.jdbc.metrics;
//
//import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
//
//import org.apache.commons.lang3.StringUtils;
//import org.clever.data.jdbc.support.P6SpyLogger;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2022/03/14 19:48 <br/>
// */
//public class P6SpyMeter implements MessageFormattingStrategy {
//    private static final P6SpyLogger P6SPY_LOGGER = new P6SpyLogger();
//
//    private static volatile boolean initialized = false;
//    private static volatile JdbcMetrics JDBC_METRICS;
//    private static volatile RequestMetrics REQUEST_METRICS;
//    private static volatile boolean enableJdbcMetrics = false;
//    private static volatile boolean enableRequestMetrics = false;
//
//    public void init() {
//        if (!SpringContextHolder.isSpringInitialized()) {
//            return;
//        }
//        if (initialized) {
//            return;
//        }
//        JDBC_METRICS = SpringContextHolder.getBean(JdbcMetrics.class, false);
//        REQUEST_METRICS = SpringContextHolder.getBean(RequestMetrics.class, false);
//        enableJdbcMetrics = JDBC_METRICS != null && JDBC_METRICS.getJdbcMetricsConfig().isEnable();
//        enableRequestMetrics = REQUEST_METRICS != null && REQUEST_METRICS.getRequestMetricsConfig().isEnable();
//        initialized = true;
//    }
//
//    @Override
//    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
//        init();
//        if ((enableJdbcMetrics || enableRequestMetrics) && StringUtils.isNotBlank(prepared)) {
//            SqlExecEvent sqlExecEvent = new SqlExecEvent(url, prepared, sql, elapsed);
//            if (enableJdbcMetrics) {
//                JDBC_METRICS.addSqlExecEvent(sqlExecEvent);
//            }
//            if (enableRequestMetrics) {
//                REQUEST_METRICS.addUrlSqlMapping(RequestMetrics.getCurrentUri(), prepared);
//            }
//        }
//        return P6SPY_LOGGER.formatMessage(connectionId, now, elapsed, category, prepared, sql, url);
//    }
//}
