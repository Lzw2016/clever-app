package org.clever.data.redis.config;

import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/16 21:21 <br/>
 */
@Data
public class RedisConfig {
    public static final String PREFIX = "redis";

    /**
     * 是否启用Redis配置
     */
    private boolean enable = false;
    /**
     * 默认的数据源名称
     */
    private String defaultName = "default";

    /**
     * Redis全局配置
     */
    private RedisProperties global = new RedisProperties();
    /**
     * Redis数据源集合(数据源名称 --> 数据源配置)
     */
    private Map<String, RedisProperties> dataSource = Collections.emptyMap();
}
