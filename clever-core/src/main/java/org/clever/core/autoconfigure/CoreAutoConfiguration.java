package org.clever.core.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.core.AppBasicsConfig;
import org.clever.core.task.StartupTaskConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/20 22:18 <br/>
 */
@EnableConfigurationProperties({AppBasicsConfig.class, StartupTaskConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class CoreAutoConfiguration {
    private final AppBasicsConfig appBasicsConfig;
    private final StartupTaskConfig startupTaskConfig;
}
