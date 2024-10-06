package org.clever.task.ext.config;

import lombok.Data;
import org.clever.js.api.pool.EnginePoolConfig;
import org.clever.task.core.config.SchedulerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/07 10:48 <br/>
 */
@ConfigurationProperties(prefix = JsEngineConfig.PREFIX)
@Data
public class JsEngineConfig implements Serializable {
    public static final String PREFIX = SchedulerConfig.PREFIX + ".js-executor";
    /**
     * JS引擎池配置
     */
    @NestedConfigurationProperty
    private EnginePoolConfig enginePool = new EnginePoolConfig();
}
