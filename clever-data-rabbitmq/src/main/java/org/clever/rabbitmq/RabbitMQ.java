package org.clever.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import org.clever.core.mapper.JacksonMapper;
import org.clever.data.AbstractDataSource;
import org.clever.rabbitmq.config.RabbitMQProperties;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/08 16:12 <br/>
 */
public class RabbitMQ extends AbstractDataSource {
    /**
     * RabbitMQ 连接配置
     */
    private final RabbitMQProperties config;
    /**
     * 数据源名称
     */
    @Getter
    private final String name;
    // @Getter
    private final ConnectionFactory connectionFactory;
    @Getter
    private final JacksonMapper jacksonMapper;

    /**
     * @param config       RabbitMQ 配置
     * @param name         数据源名称
     * @param objectMapper jackson ObjectMapper
     * @param customizer   ConnectionFactory自定义构建器
     */
    public RabbitMQ(RabbitMQProperties config, String name, ObjectMapper objectMapper, ConnectionFactoryCustomizer customizer) {
        Assert.notNull(config, "参数 config 不能为null");
        Assert.isNotBlank(name, "参数 name 不能为空");
        Assert.notNull(objectMapper, "参数 objectMapper 不能为null");
        this.config = config;
        this.name = name;
        this.connectionFactory = ConnectionFactoryCreator.createConnectionFactory(config, customizer);
        this.jacksonMapper = new JacksonMapper(objectMapper);
        initCheck();
    }

    public RabbitMQ(RabbitMQProperties config, String name, ConnectionFactoryCustomizer customizer) {
        this(config, name, JacksonMapper.getInstance().getMapper(), customizer);
    }

    public RabbitMQ(RabbitMQProperties config, String name) {
        this(config, name, JacksonMapper.getInstance().getMapper(), null);
    }

    @Override
    public void initCheck() {
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        // 	this.channelsExecutor.shutdown();
        // Close the connection(s).
        // 这将影响任何进程中的操作。在此方法返回后，将按需创建新连接。这可能用于在故障转移到辅助代理后强制重新连接到主代理。
        // 参考 CachingConnectionFactory.shutdown();
    }
}
