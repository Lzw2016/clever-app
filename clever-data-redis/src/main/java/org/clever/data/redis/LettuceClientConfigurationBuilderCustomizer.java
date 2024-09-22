package org.clever.data.redis;


import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

/**
 * 同时保留默认的自动配置。
 * 可以由希望通过 {@link LettuceClientConfiguration.LettuceClientConfigurationBuilder} 定制 {@link LettuceClientConfiguration} 的实现的回调接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/01 15:19 <br/>
 */
@FunctionalInterface
public interface LettuceClientConfigurationBuilderCustomizer {
    /**
     * 自定义 {@link LettuceClientConfiguration.LettuceClientConfigurationBuilder}
     *
     * @param clientConfigurationBuilder 要定制的构建器
     */
    void customize(LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder);
}
