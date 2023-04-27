package org.clever.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.clever.core.mapper.JacksonMapper;
import org.clever.data.AbstractDataSource;
import org.clever.rabbitmq.config.RabbitMQProperties;
import org.clever.rabbitmq.pool.ConnectionFactoryWrapper;
import org.clever.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * RabbitMQ 连接池
     */
    private final GenericObjectPool<Connection> connectionPool;
    // private final Connection sharedConnection;
    /**
     * 所有的连接 {@code Map<Connection, 连接序列号(SerialNO)>}
     */
    private final Map<Connection, Long> connections = new ConcurrentHashMap<>();

    /**
     * @param config       RabbitMQ 配置
     * @param name         数据源名称
     * @param objectMapper jackson ObjectMapper
     * @param customizer   ConnectionFactory自定义构建器
     */
    public RabbitMQ(RabbitMQProperties config, String name, ObjectMapper objectMapper, ConnectionFactoryCustomizer customizer) {
        Assert.notNull(config, "参数 config 不能为null");
        Assert.notNull(config.getPool(), "参数 config.getPool() 不能为null");
        Assert.isNotBlank(name, "参数 name 不能为空");
        Assert.notNull(objectMapper, "参数 objectMapper 不能为null");
        this.config = config;
        this.name = name;
        this.connectionFactory = ConnectionFactoryCreator.createConnectionFactory(config, customizer);
        this.jacksonMapper = new JacksonMapper(objectMapper);
        this.connectionPool = new GenericObjectPool<>(new ConnectionFactoryWrapper(name, connectionFactory), config.getPool().toGenericObjectPoolConfig());
        initConnectionFactory();
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
        // 创建一个连接 发送一个 ping 请求
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        connectionPool.close();
        // 	this.channelsExecutor.shutdown();
        // Close the connection(s).
        // 这将影响任何进程中的操作。在此方法返回后，将按需创建新连接。这可能用于在故障转移到辅助代理后强制重新连接到主代理。
        // 参考 CachingConnectionFactory.shutdown();
    }

    // 交换器、队列 - 创建/删除
    // 交换器-队列 - 绑定/解绑
    // 发送消息 (支持各种确认策略)
    // 消费消息 (支持各种确认策略)
    // 延时队列
    // 死信队列

    // --------------------------------------------------------------------------------------------
    //  内部函数
    // --------------------------------------------------------------------------------------------

    public void initConnectionFactory() {
//        connectionFactory.
    }
//
//    private Connection createConnection() {
//        connectionFactory.newConnection(createConnectionName());
//    }
}
