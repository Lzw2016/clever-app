package org.clever.data.redis.config;

import lombok.Data;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.redisson.config.TransportMode;

import java.time.Duration;
import java.util.List;

/**
 * Redis 的配置属性
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 13:28 <br/>
 */
@Data
public class RedisProperties {
    /**
     * Redis 连接模式
     */
    private Mode mode = Mode.Standalone;
    /**
     * Redis 单节点配置
     */
    private Standalone standalone = new Standalone();
    /**
     * Redis 哨兵配置
     */
    private Sentinel sentinel = new Sentinel();
    /**
     * Redis 群集配置
     */
    private Cluster cluster = new Cluster();
    /**
     * 是否启用 SSL 支持
     */
    private boolean ssl = false;
    /**
     * 读取超时
     */
    private Duration readTimeout;
    /**
     * 连接超时
     */
    private Duration connectTimeout;
    /**
     * 关机超时
     */
    private Duration shutdownTimeout = Duration.ofMillis(100);
    /**
     * 要在与 CLIENT SETNAME 的连接上设置的客户端名称
     */
    private String clientName;
    /**
     * Lettuce 连接池配置
     */
    private Pool pool = new Pool();
    /**
     * Redisson 配置
     */
    private RedissonConfig redisson = new RedissonConfig();

    /**
     * Redis 连接模式
     */
    public enum Mode {
        /**
         * 单节点
         */
        Standalone,
        /**
         * 哨兵
         */
        Sentinel,
        /**
         * 群集
         */
        Cluster,
    }

    /**
     * Redis 单节点配置
     */
    @Data
    public static class Standalone {
        /**
         * Redis 服务器 host
         */
        private String host = "localhost";
        /**
         * Redis 服务器端口
         */
        private int port = 6379;
        /**
         * 使用的Redis库(一般 0~15)
         */
        private int database = 0;
        /**
         * Redis 服务器的登录用户名
         */
        private String username;
        /**
         * Redis 服务器的登录密码
         */
        private String password;
    }

    /**
     * Redis 哨兵配置
     */
    @Data
    public static class Sentinel {
        /**
         * Redis 服务器的名称
         */
        private String master;
        /**
         * “host:port”对的逗号分隔列表
         */
        private List<String> nodes;
        /**
         * 使用的Redis库(一般 0~15)
         */
        private int database = 0;
        /**
         * Redis 服务器的登录用户名
         */
        private String username;
        /**
         * redis服务器的登录密码
         */
        private String password;
    }

    /**
     * Redis 群集配置
     */
    @Data
    public static class Cluster {
        /**
         * 以逗号分隔的“host:port”对列表，用于引导。<br/>
         * 这表示集群节点的“initial”列表，并且需要至少有一个条目。
         */
        private List<String> nodes;
        /**
         * 在集群中执行命令时要遵循的最大重定向数。
         */
        private Integer maxRedirects;
        /**
         * Redis 服务器的登录用户名
         */
        private String username;
        /**
         * redis服务器的登录密码
         */
        private String password;
        /**
         * Redis 集群节点刷新配置
         */
        private Refresh refresh = new Refresh();

        @Data
        public static class Refresh {
            /**
             * 是否发现并查询所有集群节点以获取集群拓扑。<br/>
             * 设置为 false 时，只有初始种子节点用作拓扑发现的源。
             */
            private boolean dynamicRefreshSources = true;
            /**
             * 集群拓扑刷新周期
             */
            private Duration period;
            /**
             * 是否应使用所有可用刷新触发器的自适应拓扑刷新
             */
            private boolean adaptive = false;
        }
    }

    /**
     * 连接池配置
     */
    @Data
    public static class Pool {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        /**
         * 池中“空闲”连接的最大数量。使用负值表示空闲连接数不受限制。
         */
        private int maxIdle = 8;
        /**
         * 池中要维护的最小空闲连接数的目标。<br/>
         * 此设置仅在它和逐出运行之间的时间均为正时才有效。
         */
        private int minIdle = 0;
        /**
         * 池在给定时间可以分配的最大连接数。<br/>
         * 使用负值表示没有限制。
         */
        private int maxActive = 8;
        /**
         * 连接分配在池耗尽时抛出异常之前应该阻塞的最长时间。<br/>
         * 使用负值无限期阻塞。
         */
        private Duration maxWait = Duration.ofMillis(-1);
        /**
         * 空闲对象驱逐器线程运行之间的时间。当为正时，空闲对象驱逐器线程启动，否则不执行空闲对象驱逐。
         */
        private Duration timeBetweenEvictionRuns;
    }

    /**
     * Redisson 配置
     */
    @Data
    public static class RedissonConfig {
        /**
         * 线程数量在 RTopic 对象的所有侦听器、RRemoteService 对象的调用处理程序和 RExecutorService 任务之间共享。
         * 默认值为 16。 0 表示 current_processors_amount 2
         */
        private Integer threads;
        /**
         * Redisson 使用的所有 Redis 客户端之间共享的线程数。
         * 默认值为 32。0 表示 current_processors_amount 2
         */
        private Integer nettyThreads;
        /**
         * 用于启用 Redisson 参考功能的配置选项。默认是 true
         */
        private Boolean redissonReferenceEnabled;
        /**
         * 网路传输方式。默认是 TransportMode.NIO
         */
        private TransportMode transportMode;
        /**
         * 只有在没有定义 leaseTimeout 参数的情况下获取锁时才使用此参数。如果看门狗没有将其延长到下一个 lockWatchdogTimeout 时间间隔，则锁在 lockWatchdogTimeout 后过期。
         * 这可以防止由于 Redisson 客户端崩溃或任何其他原因无法以正确方式释放锁而导致的无限锁定。
         * 默认为 30000 毫秒
         */
        private Long lockWatchdogTimeout;
        /**
         * 定义是否在获取锁后检查同步从数量与实际从数量。默认为 true
         */
        private Boolean checkLockSyncedSlaves;
        /**
         * 如果看门狗没有将其延长到下一个超时时间间隔，则可靠主题订阅者在超时后过期。
         * 这可以防止由于 Redisson 客户端崩溃或订阅者无法再使用消息时的任何其他原因导致主题中存储的消息无限增长。
         * 默认为 600000 毫秒
         */
        private Long reliableTopicWatchdogTimeout;
        /**
         * 如果看门狗没有将其延长到下一个超时时间间隔，则可靠主题订阅者在超时后过期。
         * 这可以防止由于 Redisson 客户端崩溃或订阅者无法再使用消息时的任何其他原因导致主题中存储的消息无限增长。
         * 默认为 600000 毫秒
         */
        private Boolean keepPubSubOrder;
        /**
         * 定义是否在 Redis 端使用 Lua 脚本缓存。大多数 Redisson 方法都是基于 Lua 脚本的，启用此设置可以提高此类方法的执行速度并节省网络流量。
         * 默认为 false
         */
        private Boolean useScriptCache;
        /**
         * 定义过期条目清理过程的最小延迟（以秒为单位）。
         * 应用于 JCache、RSetCache、RMapCache、RListMultimapCache、RSetMultimapCache 对象。
         * 默认为 5 秒
         */
        private Integer minCleanUpDelay;
        /**
         * 定义过期条目清理过程的最大延迟秒数。
         * 应用于 JCache、RSetCache、RMapCache、RListMultimapCache、RSetMultimapCache 对象。
         * 默认为 1800 秒
         */
        private Integer maxCleanUpDelay;
        /**
         * 定义过期条目清理过程中每次操作删除的过期密钥数量。
         * 应用于 JCache、RSetCache、RMapCache、RListMultimapCache、RSetMultimapCache 对象。
         * 默认为 100
         */
        private Integer cleanUpKeysAmount;
        /**
         * 定义是否向 Codec 提供 Thread ContextClassLoader。使用 Thread.getContextClassLoader() 可以解决 ClassNotFoundException 错误。
         * 例如，如果在 Tomcat 和已部署的应用程序中都使用 Redisson，则会出现此错误。
         * 默认为 true
         */
        private Boolean useThreadClassLoader;
        /**
         * 如果池连接在超时时间内未使用并且当前连接数量大于最小空闲连接池大小，则它将关闭并从池中删除(毫秒)
         */
        private Integer idleConnectionTimeout;
        /**
         * 重试间隔(毫秒)
         */
        private Integer retryInterval;
        /**
         * 重试尝试。默认为 3 次尝试
         */
        private Integer retryAttempts;
        /**
         * 每个 Redis 连接的订阅限制
         */
        private Integer subscriptionsPerConnection;
        /**
         * 启用 SSL 端点识别。默认为 true
         */
        private Boolean sslEnableEndpointIdentification;
        /**
         * 定义每个连接到 Redis 的 PING 命令发送间隔。 0 表示禁用。默认值为 30000毫秒
         */
        private Integer pingConnectionInterval;
        /**
         * 为连接启用 TCP keepAlive。默认是 false
         */
        private Boolean keepAlive;
        /**
         * 为连接启用 TCP noDelay。默认是 true
         */
        private Boolean tcpNoDelay;
        /**
         * single 模式配置
         */
        private SingleConfig single = new SingleConfig();
        /**
         * slave 模式配置
         */
        private SlaveConfig slave = new SlaveConfig();
        /**
         * cluster 模式配置
         */
        private ClusterConfig cluster = new ClusterConfig();
        /**
         * sentinel 模式配置
         */
        private SentinelConfig sentinel = new SentinelConfig();

        /**
         * single 模式配置
         */
        @Data
        public static class SingleConfig {
            /**
             * 检查端点的 DNS 应用程序的时间间隔（以毫秒为单位）必须确保 JVM DNS 缓存 TTL 足够低以支持此操作。
             * 设置 -1 以禁用。默认值为 5000。
             */
            private Long dnsMonitoringInterval;
            /**
             * Redis连接池大小。默认为 64
             */
            private Integer connectionPoolSize;
            /**
             * 最小空闲 Redis 连接量。默认为 24
             */
            private Integer connectionMinimumIdleSize;
            /**
             * Redis订阅-连接池大小限制。默认为 50
             */
            private Integer subscriptionConnectionPoolSize;
            /**
             * 最小空闲订阅连接量。默认为 1
             */
            private Integer subscriptionConnectionMinimumIdleSize;
        }

        /**
         * slave 模式配置
         */
        @Data
        public static class SlaveConfig {
            /**
             * <b>每个</b>从节点的Redis“从”节点最大连接池大小
             */
            private Integer slaveConnectionPoolSize;
            /**
             * 订阅 (pub/sub) 频道的最小空闲连接池大小。默认为 24
             */
            private Integer slaveConnectionMinimumIdleSize;
            /**
             * Redis Slave节点执行命令失败的时间间隔达到slaveFailsInterval值时，从可用节点内部列表中排除。默认为 180000毫秒
             */
            private Integer slaveFailsInterval;
            /**
             * Redis Slave 被排除在可用服务器的内部列表之外时尝试重新连接的间隔。
             * 在每个这样的超时事件中，Redisson 都会尝试连接到断开连接的 Redis 服务器。
             * 默认为 3000 毫秒
             */
            private Integer failedSlavesReconnectionTimeout;
            /**
             * Redis“master”服务器连接池大小。默认为 64
             */
            private Integer masterConnectionPoolSize;
            /**
             * Redis 'master' 节点每个从节点的最小空闲连接量。默认为 24
             */
            private Integer masterConnectionMinimumIdleSize;
            /**
             * 设置用于读取操作的节点类型。默认是 SLAVE
             */
            private ReadMode readMode;
            /**
             * 设置用于订阅操作的节点类型。默认是 MASTER
             */
            private SubscriptionMode subscriptionMode;
            /**
             * 订阅 (pub/sub) 频道的最大连接池大小。默认是 50
             */
            private Integer subscriptionConnectionPoolSize;
            /**
             * 每个从节点的 Redis“slave”节点最小空闲订阅 (pub/sub) 连接量。默认为 1
             */
            private Integer subscriptionConnectionMinimumIdleSize;
            /**
             * 检查端点 DNS 的时间间隔（以毫秒为单位）应用程序必须确保 JVM DNS 缓存 TTL 足够低以支持此操作。
             * 设置 -1 以禁用。默认值为 5000。
             */
            private Long dnsMonitoringInterval;
        }

        /**
         * cluster 模式配置
         */
        @Data
        public static class ClusterConfig {
            /**
             * Redis 集群扫描间隔，以毫秒为单位。默认为 5000毫秒
             */
            private Integer scanInterval;
            /**
             * 在 Redisson 启动期间启用集群插槽检查。默认为 true
             */
            private Boolean checkSlotsCoverage;
        }

        /**
         * sentinel 模式配置
         */
        @Data
        public static class SentinelConfig {
            /**
             * Sentinel 扫描间隔（以毫秒为单位）。默认为 1000毫秒
             */
            private Integer scanInterval;
            /**
             * 在 Redisson 启动期间启用哨兵列表检查。默认为 true
             */
            private Boolean checkSentinelsList;
            /**
             * 使用“master-link-status”标志检查哨兵的节点状态。默认为 true
             */
            private Boolean checkSlaveStatusWithSyncing;
            /**
             * 启用哨兵发现。默认为 true
             */
            private Boolean sentinelsDiscovery;
        }
    }
}
