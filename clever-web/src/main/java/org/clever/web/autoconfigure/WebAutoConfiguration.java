package org.clever.web.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.web.config.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/22 13:31 <br/>
 */
@EnableConfigurationProperties({WebConfig.class, StaticResourceConfig.class, EchoConfig.class, CorsConfig.class, MvcConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class WebAutoConfiguration {
    private final WebConfig webConfig;
    private final StaticResourceConfig staticResourceConfig;
    private final EchoConfig echoConfig;
    private final CorsConfig config;
    private final MvcConfig mvcConfig;
}
