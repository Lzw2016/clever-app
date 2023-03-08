package org.clever.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import org.clever.rabbitmq.config.RabbitMQProperties;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/08 21:05 <br/>
 */
public class ConnectionFactoryCreator {
    public static ConnectionFactory createConnectionFactory(RabbitMQProperties config, ConnectionFactoryCustomizer customizer) {
        Assert.notNull(config, "参数  不能为null");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getHost());
        factory.setPort(config.getPort());
        factory.setVirtualHost(config.getVirtualHost());
        factory.setUsername(config.getUsername());
        factory.setPassword(config.getPassword());
        factory.setRequestedChannelMax(config.getRequestedChannelMax());
        factory.setRequestedFrameMax(config.getRequestedFrameMax());
        factory.setRequestedHeartbeat(config.getRequestedHeartbeat());
        factory.setConnectionTimeout(config.getConnectionTimeout());
        factory.setHandshakeTimeout(config.getHandshakeTimeout());
        factory.setShutdownTimeout(config.getShutdownTimeout());
        if (config.getClientProperties() != null) {
            factory.getClientProperties().putAll(config.getClientProperties());
        }
        factory.setAutomaticRecoveryEnabled(config.isAutomaticRecovery());
        factory.setTopologyRecoveryEnabled(config.isTopologyRecovery());
        factory.setNetworkRecoveryInterval(config.getNetworkRecoveryInterval());
        if (config.isNio()) {
            factory.useNio();
        } else {
            factory.useBlockingIo();
        }
        factory.setChannelRpcTimeout(config.getChannelRpcTimeout());
        factory.setChannelShouldCheckRpcResponseType(config.isChannelShouldCheckRpcResponseType());
        factory.setWorkPoolTimeout(config.getWorkPoolTimeout());
        if (customizer != null) {
            customizer.customize(factory, config);
        }
        return factory;
    }

    public static ConnectionFactory createConnectionFactory(RabbitMQProperties config) {
        return createConnectionFactory(config, null);
    }
}
