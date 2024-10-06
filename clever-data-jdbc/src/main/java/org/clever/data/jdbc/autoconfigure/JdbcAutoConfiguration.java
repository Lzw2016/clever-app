package org.clever.data.jdbc.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.data.jdbc.config.MybatisConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/21 13:07 <br/>
 */
@EnableConfigurationProperties({JdbcConfig.class, MybatisConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class JdbcAutoConfiguration {
    private final JdbcConfig jdbcConfig;
    private final MybatisConfig mybatisConfig;
}
