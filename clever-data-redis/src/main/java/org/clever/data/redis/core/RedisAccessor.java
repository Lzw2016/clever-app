package org.clever.data.redis.core;

import org.clever.beans.factory.InitializingBean;
import org.clever.data.redis.connection.RedisConnectionFactory;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定义通用属性的 {@link RedisTemplate} 基类。不打算直接使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:13 <br/>
 */
public class RedisAccessor implements InitializingBean {
    /**
     * 子类可用的记录器
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private RedisConnectionFactory connectionFactory;

    @Override
    public void afterPropertiesSet() {
        Assert.state(getConnectionFactory() != null, "RedisConnectionFactory is required");
    }

    /**
     * 返回 connectionFactory
     *
     * @return 返回connectionFactory。可以是 {@literal null}
     */
    public RedisConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * 如果没有设置连接工厂，则返回所需的 {@link RedisConnectionFactory} 或抛出 {@link IllegalStateException}
     *
     * @return 关联的 {@link RedisConnectionFactory}
     * @throws IllegalStateException 如果未设置 connectionFactory
     */
    public RedisConnectionFactory getRequiredConnectionFactory() {
        RedisConnectionFactory connectionFactory = getConnectionFactory();
        if (connectionFactory == null) {
            throw new IllegalStateException("RedisConnectionFactory is required");
        }
        return connectionFactory;
    }

    /**
     * 设置 connectionFactory
     *
     * @param connectionFactory 要设置的 connectionFactory
     */
    public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
