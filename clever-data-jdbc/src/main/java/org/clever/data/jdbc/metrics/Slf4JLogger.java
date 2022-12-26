//package org.clever.data.jdbc.metrics;
//
//import com.p6spy.engine.logging.Category;
//import com.yvan.core.spring.SpringContextHolder;
//import com.yvan.groovy.config.JdbcMetricsConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2022/03/17 16:39 <br/>
// */
//public class Slf4JLogger extends com.p6spy.engine.spy.appender.Slf4JLogger {
//    private static volatile boolean initialized = false;
//    private static volatile JdbcMetricsConfig JDBC_METRICS_Config;
//    private final Logger log = LoggerFactory.getLogger("p6spy");
//
//    public void init() {
//        if (initialized) {
//            return;
//        }
//        JDBC_METRICS_Config = SpringContextHolder.getBean(JdbcMetricsConfig.class, false);
//        initialized = true;
//    }
//
//    @Override
//    public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql, String url) {
//        init();
//        if (JDBC_METRICS_Config != null && JDBC_METRICS_Config.getIgnoreSqls().contains(prepared)) {
//            return;
//        }
//        if ("category".equals(category.getName())) {
//            log.info("outage " + elapsed + "ms " + prepared);
//        } else {
//            super.logSQL(connectionId, now, elapsed, category, prepared, sql, url);
//        }
//    }
//}
