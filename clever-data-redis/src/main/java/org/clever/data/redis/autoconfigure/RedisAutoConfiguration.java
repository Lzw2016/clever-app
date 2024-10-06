package org.clever.data.redis.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.clever.data.redis.config.RedisConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/21 13:07 <br/>
 */
@EnableConfigurationProperties({RedisConfig.class})
@Configuration
@Getter
@AllArgsConstructor
public class RedisAutoConfiguration {
    private final RedisConfig redisConfig;
}
