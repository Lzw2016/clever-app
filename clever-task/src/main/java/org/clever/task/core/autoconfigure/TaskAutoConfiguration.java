package org.clever.task.core.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.task.core.config.SchedulerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/26 22:35 <br/>
 */
@EnableConfigurationProperties({SchedulerConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class TaskAutoConfiguration {
    private final SchedulerConfig schedulerConfig;
}
