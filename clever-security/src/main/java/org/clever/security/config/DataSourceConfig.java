package org.clever.security.config;

import lombok.Data;

import java.io.Serializable;

/**
 * security 使用的数据源配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/20 21:56 <br/>
 */
@Data
public class DataSourceConfig implements Serializable {
    /**
     * jdbc 数据源名称
     */
    private String jdbcName = "";
    /**
     * 启用 redis 提高性能
     */
    private boolean enableRedis = false;
    /**
     * redis数据源名称
     */
    private String redisName = "";
    /**
     * Token Redis前缀
     */
    private String redisNamespace = "security";
}
