package org.clever.data.redis.support;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/20 14:37 <br/>
 */
@Data
public class RateLimitState {
    /**
     * 限流配置
     */
    private RateLimitConfig config;
    /**
     * 是否被限流。true: 被限流，应阻止请求继续访问；false: 未被限流
     */
    private boolean limited;
    /**
     * 达到限流条件的剩余请求数
     */
    private long left;
}
