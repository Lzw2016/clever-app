package org.clever.data.redis.config;

import lombok.Data;

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
}
