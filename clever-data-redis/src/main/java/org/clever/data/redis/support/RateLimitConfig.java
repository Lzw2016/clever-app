package org.clever.data.redis.support;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/20 14:34 <br/>
 */
@Data
public class RateLimitConfig {
    /**
     * 在times秒内
     */
    private final long times;
    /**
     * 最多请求limit次
     */
    private final long limit;

    /**
     * 在times秒内,最多请求limit次
     */
    public RateLimitConfig(long times, long limit) {
        this.times = times;
        this.limit = limit;
    }
}
