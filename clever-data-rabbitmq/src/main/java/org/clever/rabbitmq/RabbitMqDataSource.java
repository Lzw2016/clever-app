//package org.clever.rabbitmq;
//
//
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.ShutdownSignalException;
//import lombok.Getter;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import java.util.Objects;
//import java.util.Properties;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Consumer;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/02/14 11:48 <br/>
// */
//@SuppressWarnings({"UnusedReturnValue", "unused"})
//@Slf4j
//public class RabbitMqDataSource extends AbstractDataSource {
//    /**
//     * DLX
//     */
//    public static final String DEAD_LETTER_QUEUE_KEY = "x-dead-letter-exchange";
//    /**
//     * DLK
//     */
//    public static final String DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
//    /**
//     * 数据源配置
//     */
//    private final AmqpConfig amqpConfig;
//    @Getter
//    private final CachingConnectionFactory cachingConnectionFactory;
//    // private final ConnectionFactory connectionFactory;
//    @Getter
//    private final RabbitAdmin rabbitAdmin;
//    private final RabbitTemplate rabbitTemplate;
//    private final ThreadPoolExecutor executorService;
//    private final Gauge gaugeCoreSize;
//    private final Gauge gaugeLargestSize;
//    private final Gauge gaugeMaxSize;
//    private final Gauge gaugeActiveSize;
//    private final Gauge gaugeThreadCount;
//    private final Gauge gaugeQueueSize;
//
//    public RabbitMqDataSource(AmqpConfig amqpConfig) {
//        this.amqpConfig = CopyConfigUtils.copyConfig(amqpConfig);
//        // 初始化 ConnectionFactory
//        cachingConnectionFactory = new CachingConnectionFactory();
//        cachingConnectionFactory.setHost(amqpConfig.getHost());
//        cachingConnectionFactory.setPort(amqpConfig.getPort());
//        cachingConnectionFactory.setUsername(amqpConfig.getUsername());
//        cachingConnectionFactory.setPassword(amqpConfig.getPassword());
//        if (StringUtils.isNotBlank(amqpConfig.getVhost())) {
//            cachingConnectionFactory.setVirtualHost(amqpConfig.getVhost());
//        }
//        Integer poolSize = amqpConfig.getThreadPoolSize();
//        if (poolSize == null || poolSize <= 0) {
//            poolSize = 30;
//        }
//        executorService = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
//        cachingConnectionFactory.setExecutor(executorService);
//        // 设置缓存模式 channel
//        cachingConnectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
//        // 设置缓存配置
//        cachingConnectionFactory.setChannelCacheSize(8192);
//        cachingConnectionFactory.setConnectionCacheSize(8);
//        cachingConnectionFactory.setConnectionLimit(64);
//        cachingConnectionFactory.setChannelCheckoutTimeout(0);
//        // 启用confirm保证达到交换机
//        cachingConnectionFactory.setPublisherConfirms(true);
//        // 启用return保证交换机到达队列, 设置PublisherReturns状态为true, 那么需要设置rabbitTemplate.setMandatory(true)
//        cachingConnectionFactory.setPublisherReturns(true);
//        // 设置最大的Channel数量
//        cachingConnectionFactory.getRabbitConnectionFactory().setRequestedChannelMax(8192);
//        // 监听 Connection
//        cachingConnectionFactory.addConnectionListener(new ConnectionListener() {
//            @SuppressWarnings("NullableProblems")
//            @Override
//            public void onCreate(Connection connection) {
//                log.info("Create Connection: LocalPort=[{}]", connection.getLocalPort());
//            }
//
//            @SuppressWarnings("NullableProblems")
//            @Override
//            public void onClose(Connection connection) {
//                log.info("Close Connection: LocalPort=[{}]", connection.getLocalPort());
//            }
//
//            @SuppressWarnings("NullableProblems")
//            @Override
//            public void onShutDown(ShutdownSignalException signal) {
//                String message = signal.getMessage();
//                if (message.contains("reply-code=200")) {
//                    log.info("ShutDown Connection: Message=[{}]", message);
//                } else {
//                    log.info("ShutDown Connection", signal);
//                }
//            }
//        });
//        // 监听 Channel
//        cachingConnectionFactory.addChannelListener(new ChannelListener() {
//            @SuppressWarnings("NullableProblems")
//            @Override
//            public void onCreate(Channel channel, boolean transactional) {
//                log.info("Create Channel: ChannelNumber=[{}], 事务性=[{}]", channel.getChannelNumber(), transactional);
//            }
//
//            @SuppressWarnings("NullableProblems")
//            @Override
//            public void onShutDown(ShutdownSignalException signal) {
//                String message = signal.getMessage();
//                if (message.contains("reply-code=200")) {
//                    log.info("ShutDown Channel: Message=[{}]", message);
//                } else {
//                    log.info("ShutDown Channel", signal);
//                }
//            }
//        });
//        // Rabbit原生ConnectionFactory
//        ConnectionFactory connectionFactory = cachingConnectionFactory.getRabbitConnectionFactory();
//        // 设置自动恢复启用
//        connectionFactory.setAutomaticRecoveryEnabled(true);
//        // 设置拓扑恢复启用
//        // connectionFactory.setTopologyRecoveryEnabled(false);
//        // 设置网络恢复间隔(毫秒)
//        connectionFactory.setNetworkRecoveryInterval(1000 * 10);
//        int multiple = 5;
//        // 设置请求的心跳(秒) 60 | 60 * 60 * 10 * multiple
//        connectionFactory.setRequestedHeartbeat(60 * 60 * 10 * multiple);
//        // 设置连接超时(毫秒) 60000 | 1000 * 60 * 5 * multiple
//        connectionFactory.setConnectionTimeout(1000 * 60 * 5 * multiple);
//        // 设置握手超时(毫秒) 10000 | 1000 * 60 * 5 * multiple
//        connectionFactory.setHandshakeTimeout(1000 * 60 * 5 * multiple);
//        // 初始化 RabbitAdmin
//        rabbitAdmin = new RabbitAdmin(cachingConnectionFactory);
//        // 初始化 RabbitTemplate
//        rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
//        rabbitTemplate.setMandatory(true);
//        // 消费发送和接收使用不同的Connection
//        rabbitTemplate.setUsePublisherConnection(true);
//        // 创建连接
//        cachingConnectionFactory.createConnection();
//        // 加入监控数据源代码
//        final String type = "datasource";
//        final String name = "rabbitmq@" + this.amqpConfig.getHost() + ":" + this.amqpConfig.getPort();
//        gaugeCoreSize = MetersManager.threadPoolCoreSize(type, name, executorService);
//        gaugeLargestSize = MetersManager.threadPoolLargestSize(type, name, executorService);
//        gaugeMaxSize = MetersManager.threadPoolMaxSize(type, name, executorService);
//        gaugeActiveSize = MetersManager.threadPoolActiveSize(type, name, executorService);
//        gaugeThreadCount = MetersManager.threadPoolThreadCount(type, name, executorService);
//        gaugeQueueSize = MetersManager.threadPoolQueueSize(type, name, executorService);
//    }
//
//    public AmqpConfig getAmqpConfig() {
//        return CopyConfigUtils.copyConfig(amqpConfig);
//    }
//
//    /**
//     * 发送消息
//     */
//    public void send(String exchange, String routingKey, Message message) {
//        rabbitTemplate.send(exchange, routingKey, message);
//    }
//
//    /**
//     * 发送消息
//     */
//    public void convertAndSend(String exchange, String routingKey, Object message) {
//        rabbitTemplate.convertAndSend(exchange, routingKey, message);
//    }
//
//    /**
//     * 发送消息
//     */
//    public void convertAndSend(String exchange, String routingKey, final Object message, final MessagePostProcessor messagePostProcessor) {
//        rabbitTemplate.convertAndSend(exchange, routingKey, message, messagePostProcessor);
//    }
//
//    /**
//     * 清除队列数据
//     */
//    public void queuePurge(String queue) {
//        try {
//            int count = rabbitAdmin.purgeQueue(queue);
//            log.info("[RabbitMqDataSource] purgeQueue {} count: {}", queue, count);
//        } catch (Exception e) {
//            log.warn("[RabbitMqDataSource] purgeQueue {} error", queue, e);
//        }
//    }
//
//    public Properties getQueueProperties(String queue) {
//        return rabbitAdmin.getQueueProperties(queue);
//    }
//
//    /**
//     * 获取Channel处理数据，能保证Channel会被close <br />
//     * <b>注意：execute会在当前调用线程上执行，阻塞当前调用线程</b>
//     */
//    public <T> T execute(ChannelCallback<T> action) {
//        return rabbitTemplate.execute(action);
//    }
//
//    /**
//     * 消费队列消息(会自动关闭连接)<br />
//     * <b>注意：consumer会在后台线程上执行，不会阻塞当前调用线程</b>
//     */
//    public CanInterruptConsumer consumer(
//            String queue,
//            boolean autoAck,
//            int prefetchCount,
//            String consumerTag,
//            ConsumerMessages consumerMessages,
//            Consumer<Throwable> onStopConsumer) {
//        CanInterruptConsumer consumer;
//        Channel channel = null;
//        try {
//            channel = cachingConnectionFactory.createConnection().createChannel(false);
//            consumer = new CanInterruptConsumer(this.getDataSourceId(), channel, queue, autoAck, prefetchCount, consumerMessages, onStopConsumer);
//            channel.basicQos(prefetchCount);
//            if (StringUtils.isBlank(consumerTag)) {
//                channel.basicConsume(queue, autoAck, consumer);
//            } else {
//                channel.basicConsume(queue, autoAck, consumerTag, consumer);
//            }
//        } catch (Exception e) {
//            if (channel != null && channel.isOpen()) {
//                try {
//                    channel.close();
//                } catch (Exception ex) {
//                    log.warn("[RabbitMqDataSource] channel.close  error", ex);
//                }
//            }
//            throw ExceptionUtils.unchecked(e);
//        }
//        return consumer;
//    }
//
//    /**
//     * 消费队列消息(会自动关闭连接)<br />
//     * <b>注意：consumer会在后台线程上执行，不会阻塞当前调用线程</b>
//     */
//    public CanInterruptConsumer consumer(String queue, boolean autoAck, int prefetchCount, String consumerTag, ConsumerMessages consumerMessages) {
//        return consumer(queue, autoAck, prefetchCount, consumerTag, consumerMessages, null);
//    }
//
//    /**
//     * 消费队列消息(会自动关闭连接)<br />
//     * <b>注意：consumer会在后台线程上执行，不会阻塞当前调用线程</b>
//     */
//    public void retryConsumer(CanInterruptConsumer canInterruptConsumer) {
//        Assert.isTrue(Objects.equals(canInterruptConsumer.getDataSourceId(), this.getDataSourceId()), "CanInterruptConsumer 不能更换数据源消费数据");
//        final String queue = canInterruptConsumer.getQueue();
//        final boolean autoAck = canInterruptConsumer.isAutoAck();
//        final int prefetchCount = canInterruptConsumer.getPrefetchCount();
//        final String consumerTag = canInterruptConsumer.getConsumerTag();
//        Channel channel = null;
//        try {
//            channel = cachingConnectionFactory.createConnection().createChannel(false);
//            // 关闭之前的Channel
//            canInterruptConsumer.interrupt();
//            // 清除中断状态
//            canInterruptConsumer.clearInterrupt();
//            // 设置新的Channel
//            canInterruptConsumer.setChannel(channel);
//            // 开始重新消费
//            channel.basicQos(prefetchCount);
//            channel.basicConsume(queue, autoAck, consumerTag, canInterruptConsumer);
//        } catch (Exception e) {
//            if (channel != null && channel.isOpen()) {
//                try {
//                    channel.close();
//                } catch (Exception ex) {
//                    log.warn("[RabbitMqDataSource] channel.close  error", ex);
//                }
//            }
//            throw ExceptionUtils.unchecked(e);
//        }
//    }
//
//    /**
//     * 消费队列消息(会自动关闭连接)<br />
//     * <b>注意：consumer会在后台线程上执行，不会阻塞当前调用线程</b>
//     */
//    public CanInterruptConsumer consumer(String queue, boolean autoAck, int prefetchCount, ConsumerMessages consumerMessages) {
//        return consumer(queue, autoAck, prefetchCount, null, consumerMessages);
//    }
//
//    /**
//     * 声明 Exchange
//     */
//    public void declareExchange(final Exchange exchange) {
//        rabbitAdmin.declareExchange(exchange);
//    }
//
//    /**
//     * 声明 Queue
//     */
//    public String declareQueue(final Queue queue) {
//        return rabbitAdmin.declareQueue(queue);
//    }
//
//    /**
//     * 声明 绑定关系
//     */
//    public void declareBinding(final Binding binding) {
//        rabbitAdmin.declareBinding(binding);
//    }
//
//    /**
//     * 删除 Queue
//     *
//     * @param queueName 队列的名称
//     * @param unused    如果仅应在不使用时删除队列，则为true
//     * @param empty     如果队列只有在为空时才应删除，则为true
//     */
//    public void deleteQueue(String queueName, boolean unused, boolean empty) {
//        rabbitAdmin.deleteQueue(queueName, unused, empty);
//    }
//
//    /**
//     * 删除 Queue
//     */
//    public boolean deleteQueue(final String queueName) {
//        return rabbitAdmin.deleteQueue(queueName);
//    }
//
//    /**
//     * 删除 Exchange
//     */
//    public boolean deleteExchange(final String exchangeName) {
//        return rabbitAdmin.deleteExchange(exchangeName);
//    }
//
//    /**
//     * 移除 绑定关系
//     */
//    public void removeBinding(final Binding binding) {
//        rabbitAdmin.removeBinding(binding);
//    }
//
//    /**
//     * 释放RabbitMq数据源
//     */
//    @Override
//    public void close() throws Exception {
//        super.close();
//        MetersManager.remove(gaugeCoreSize.getId());
//        MetersManager.remove(gaugeLargestSize.getId());
//        MetersManager.remove(gaugeMaxSize.getId());
//        MetersManager.remove(gaugeActiveSize.getId());
//        MetersManager.remove(gaugeThreadCount.getId());
//        MetersManager.remove(gaugeQueueSize.getId());
//        if (cachingConnectionFactory != null) {
//            cachingConnectionFactory.destroy();
//        }
//        if (executorService != null) {
//            executorService.shutdown();
//        }
//    }
//
//    @Override
//    public String getDataSourceId() {
//        return amqpConfig.getDataSourceId();
//    }
//}
