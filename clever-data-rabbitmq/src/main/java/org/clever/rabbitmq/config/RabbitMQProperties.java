package org.clever.rabbitmq.config;

import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/08 21:09 <br/>
 */
@Data
public class RabbitMQProperties {
    /**
     * 服务 host/ip
     */
    private String host = ConnectionFactory.DEFAULT_HOST;
    /**
     * 服务端口
     */
    private int port = ConnectionFactory.USE_DEFAULT_PORT;
    /**
     * 虚拟主机 virtual_host
     */
    private String virtualHost = ConnectionFactory.DEFAULT_VHOST;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 最大通道数
     */
    private int requestedChannelMax = ConnectionFactory.DEFAULT_CHANNEL_MAX;
    /**
     * 单次请求的最大数量量，0代表无限
     */
    private int requestedFrameMax = ConnectionFactory.DEFAULT_FRAME_MAX;
    /**
     * 设置请求的心跳超时。心跳帧将在大约 1/2 个超时间隔发送。
     * 如果服务器心跳超时配置为非零值，则只能使用此方法降低该值；否则将使用客户提供的任何值。
     */
    private int requestedHeartbeat = ConnectionFactory.DEFAULT_HEARTBEAT;
    /**
     * 设置 TCP 连接超时
     */
    private int connectionTimeout = ConnectionFactory.DEFAULT_CONNECTION_TIMEOUT;
    /**
     * 设置 AMQP0-9-1 协议握手超时
     */
    private int handshakeTimeout = ConnectionFactory.DEFAULT_HANDSHAKE_TIMEOUT;
    /**
     * 设置 shutdown 超时
     */
    private int shutdownTimeout = ConnectionFactory.DEFAULT_SHUTDOWN_TIMEOUT;
    /**
     * 自定义客户端属性
     */
    private Map<String, Object> clientProperties = new HashMap<>();
    /**
     * 启用或禁用自动连接恢复
     */
    private boolean automaticRecovery = true;
    /**
     * 启用或禁用拓扑恢复
     */
    private boolean topologyRecovery = true;
    /**
     * 设置连接恢复间隔。默认值为 5000 毫秒
     */
    private long networkRecoveryInterval = ConnectionFactory.DEFAULT_NETWORK_RECOVERY_INTERVAL;
    /**
     * 使用非阻塞 IO (NIO) 与服务器通信。
     * 使用 NIO，从同一个 ConnectionFactory 创建的多个连接可以使用同一个 IO 线程。
     * 使用大量不那么活跃的连接的客户端进程可以从 NIO 中受益，因为它使用的线程比传统的阻塞 IO 模式更少。
     */
    private boolean nio = false;
    /**
     * 设置通道中 RPC 调用的持续超时(单位: 毫秒)。默认为 10 分钟。 0 表示没有超时。
     */
    private int channelRpcTimeout = ConnectionFactory.DEFAULT_CHANNEL_RPC_TIMEOUT;
    /**
     * 通道是否检查 RPC 调用的回复类型。默认为 false
     */
    private boolean channelShouldCheckRpcResponseType = false;
    /**
     * 工作池排队的超时时间（以毫秒为单位）
     */
    private int workPoolTimeout = ConnectionFactory.DEFAULT_WORK_POOL_TIMEOUT;
}
