package org.clever.data.redis;

import org.redisson.config.Config;

/**
 * 回调接口，可由希望自定义 {@link org.redisson.api.RedissonClient} 配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/16 11:21 <br/>
 */
public interface RedissonClientConfigurationCustomizer {
    /**
     * 自定义 RedissonClient 配置
     *
     * @param configuration 需要自定义的 {@link Config}
     */
    void customize(final Config configuration);
}
