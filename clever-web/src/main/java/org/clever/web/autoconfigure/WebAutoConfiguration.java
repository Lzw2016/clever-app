package org.clever.web.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.web.config.CorsConfig;
import org.clever.web.config.EchoConfig;
import org.clever.web.config.MvcConfig;
import org.clever.web.config.WebConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/22 13:31 <br/>
 */
@EnableConfigurationProperties({WebConfig.class, EchoConfig.class, CorsConfig.class, MvcConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class WebAutoConfiguration {
    private final WebConfig webConfig;
}
