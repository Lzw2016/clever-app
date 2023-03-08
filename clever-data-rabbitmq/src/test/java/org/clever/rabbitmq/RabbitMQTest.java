package org.clever.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/03 22:51 <br/>
 */
@Slf4j
public class RabbitMQTest {
    // 192.168.1.201:30012
    final static String HOST = "192.168.1.201";
    final static Integer PORT = 30012;
    final static String USERNAME = "admin";
    final static String PASSWORD = "admin123456";
    final static String VIRTUALHOST = "/";

    @Test
    public void t01() throws IOException, TimeoutException, InterruptedException {
        //创建连接工厂，设置连接rabbitmq的参数
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        factory.setVirtualHost(VIRTUALHOST);
        //获取连接
        Connection connection = factory.newConnection();
        //通过连接创建信道
        Channel channel = connection.createChannel();

        String queue = "test_0";
        // 创建一个type="direct"、持久化的、非自动删除的交换器
        // channel.exchangeDeclare(EXCHANGE_NAME, "direct", true, false, null);
        //创建队列，设置队列名、不持久化、不排他、不自动删除、参数为空
        channel.queueDeclare(queue, true, false, false, null);
        //将交换器与队列通过路由键绑定
        // channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
        int count = 100_0000;
        final long startTime = System.currentTimeMillis();

        //发送消息，指定发送交换器（""则为自带默认交换器）、队列、消息基本属性集为空，发送内容为字节数组
        for (int i = 0; i < count; i++) {
            String message = "Hello World!_" + i;
            channel.basicPublish("", queue, null, message.getBytes(StandardCharsets.UTF_8));
            if (i % 1000 == 0) {
                log.info("--> {}", i);
            }
        }
        // channel.waitForConfirms();

        // 消费消息
//        AtomicInteger idx = new AtomicInteger(0);
//        Consumer consumer = new DefaultConsumer(channel) {
//            @Override
//            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                String message = new String(body, StandardCharsets.UTF_8);
//                channel.basicAck(envelope.getDeliveryTag(), true);
//                int i = idx.incrementAndGet();
//                if (i % 1000 == 0) {
//                    log.info("--> {}", i);
//                }
//            }
//        };
//        channel.basicConsume(queue, false, consumer);
//        while (idx.get() < count) {
//            // noinspection BusyWait
//            Thread.sleep(100);
//        }

        final long endTime = System.currentTimeMillis();
        log.info("耗时: {}ms | {}个/ms", (endTime - startTime), count / (endTime - startTime));
        Thread.sleep(1000);
        //关闭信道和连接
        channel.close();
        connection.close();
    }
}
