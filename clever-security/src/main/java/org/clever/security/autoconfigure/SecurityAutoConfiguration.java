package org.clever.security.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.security.config.SecurityConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/26 10:44 <br/>
 */
@EnableConfigurationProperties({SecurityConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class SecurityAutoConfiguration {
    private final SecurityConfig securityConfig;
}
