package org.clever.task.ext.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.task.ext.config.JsEngineConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/26 22:35 <br/>
 */
@EnableConfigurationProperties({JsEngineConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class TaskExtAutoConfiguration {
    private final JsEngineConfig jsEngineConfig;
}
