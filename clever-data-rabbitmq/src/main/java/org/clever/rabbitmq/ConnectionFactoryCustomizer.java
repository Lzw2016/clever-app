package org.clever.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import org.clever.rabbitmq.config.RabbitMQProperties;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/08 21:03 <br/>
 */
public interface ConnectionFactoryCustomizer {
    /**
     * @param connectionFactory 需要自定义的 ConnectionFactory
     * @param config            RabbitMQ配置
     */
    void customize(final ConnectionFactory connectionFactory, RabbitMQProperties config);
}
