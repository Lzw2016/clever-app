package org.clever.data.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 13:39 <br/>
 */
@Data
public class MultipleJdbcConfig {
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
     * JDBC数据源全局配置
     */
    private HikariConfig global = new HikariConfig();

    /**
     * JDBC数据源集合(数据源名称 --> 数据源配置)
     */
    private Map<String, HikariConfig> jdbcMap = Collections.emptyMap();
}
