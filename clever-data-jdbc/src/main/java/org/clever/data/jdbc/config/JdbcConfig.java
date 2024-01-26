package org.clever.data.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;

import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 13:39 <br/>
 */
@Data
public class JdbcConfig {
    public static final String PREFIX = "jdbc";

    /**
     * 是否启用JDBC配置
     */
    private boolean enable = false;
    /**
     * 默认的数据源名称
     */
    private String defaultName = "default";
    /**
     * p6spy 打印SQL日志配置 <br />
     * 实现类: org.clever.data.jdbc.metrics.Slf4JLogger
     */
    private P6SpyLog p6spylog = new P6SpyLog();
    /**
     * jdbc性能监控配置
     */
    private JdbcMetrics metrics = new JdbcMetrics();
    /**
     * JDBC数据源全局配置
     */
    private HikariConfig global = new HikariConfig();
    /**
     * JDBC数据源集合(数据源名称 --> 数据源配置)
     */
    private Map<String, HikariConfig> dataSource = Collections.emptyMap();

    @Data
    public static class P6SpyLog {
        /**
         * 标记慢SQL的执行时间(毫秒)
         */
        private int slow = 800;
        /**
         * 忽略的SQL语句(完整匹配，大小写敏感)
         */
        private Set<String> ignoreSql = new HashSet<>();
        /**
         * 忽略的SQL语句(包含匹配，大小写敏感)
         */
        private Set<String> ignoreContainsSql = new HashSet<>();
    }

    @Data
    public static class JdbcMetrics {
        /**
         * 是否启用监控jdbc性能
         */
        private boolean enable = false;
        /**
         * 最大监控的SQL数量
         */
        private int maxSqlCount = 2000;
        /**
         * 执行直方图配置
         */
        private List<Integer> histogram = new ArrayList<Integer>() {{
            add(20);
            add(50);
            add(100);
            add(200);
            add(500);
            add(1000);
            add(2000);
            add(5000);
        }};
        /**
         * 直方图区间最耗时的TopN
         */
        private int histogramTopN = 3;
    }
}
