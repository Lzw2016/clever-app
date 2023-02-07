package org.clever.data.redis.connection;

import org.clever.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 与 Redis 连接设置相关的配置类的标记接口。
 * 由于设置方案非常多样化，而不是为统一这些方案而苦苦挣扎，因此 {@link RedisConfiguration} 提供了识别个人用途配置的方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:25 <br/>
 */
public interface RedisConfiguration {
    /**
     * 如果当前 {@link RedisConfiguration} 是 {@link #isDatabaseIndexAware(RedisConfiguration) database aware} 或计算并返回给定 {@link Supplier} 的值，则获取配置的数据库索引。
     *
     * @param other {@code Supplier}，如果给定 {@link RedisConfiguration} 则返回其结果不是 {@link #isDatabaseIndexAware(RedisConfiguration) database aware}。
     * @return 从不 {@literal null}
     * @throws IllegalArgumentException 如果 {@code other} 为 {@literal null}
     */
    default Integer getDatabaseOrElse(Supplier<Integer> other) {
        return getDatabaseOrElse(this, other);
    }

    /**
     * 如果当前 {@link RedisConfiguration} 是 {@link #isAuthenticationAware(RedisConfiguration) password aware}，则获取配置的 {@link RedisPassword}，或者计算并返回给定 {@link Supplier} 的值。
     *
     * @param other 一个 {@code Supplier}，如果给定 {@link RedisConfiguration} 则返回其结果不是 {@link #isAuthenticationAware(RedisConfiguration) password aware}
     * @return 从不 {@literal null}
     * @throws IllegalArgumentException 如果 {@code other} 为 {@literal null}
     */
    default RedisPassword getPasswordOrElse(Supplier<RedisPassword> other) {
        return getPasswordOrElse(this, other);
    }

    /**
     * @param configuration 可以是 {@literal null}
     * @return {@code true} 如果给定的 {@link RedisConfiguration} 是 {@link WithPassword} 的实例
     */
    static boolean isAuthenticationAware(RedisConfiguration configuration) {
        return configuration instanceof WithAuthentication;
    }

    /**
     * @param configuration 可以是 {@literal null}
     * @return {@code true} 如果给定的 {@link RedisConfiguration} 是 {@link WithDatabaseIndex} 的实例
     */
    static boolean isDatabaseIndexAware(RedisConfiguration configuration) {
        return configuration instanceof WithDatabaseIndex;
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @return {@code true} 如果给定的 {@link RedisConfiguration} 是 {@link SentinelConfiguration} 的实例
     */
    static boolean isSentinelConfiguration(RedisConfiguration configuration) {
        return configuration instanceof SentinelConfiguration;
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @return {@code true} 如果给定的 {@link RedisConfiguration} 是 {@link WithHostAndPort} 的实例
     */
    static boolean isHostAndPortAware(RedisConfiguration configuration) {
        return configuration instanceof WithHostAndPort;
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @return {@code true} 如果给定的 {@link RedisConfiguration} 是 {@link ClusterConfiguration} 的实例
     */
    static boolean isClusterConfiguration(RedisConfiguration configuration) {
        return configuration instanceof ClusterConfiguration;
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @return {@code true} 如果给定的 {@link RedisConfiguration} 是 {@link StaticMasterReplicaConfiguration} 的实例
     */
    static boolean isStaticMasterReplicaConfiguration(RedisConfiguration configuration) {
        return configuration instanceof StaticMasterReplicaConfiguration;
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @return {@code true} 如果给定的 {@link RedisConfiguration} 是 {@link DomainSocketConfiguration} 的实例
     */
    static boolean isDomainSocketConfiguration(RedisConfiguration configuration) {
        return configuration instanceof DomainSocketConfiguration;
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @param other         {@code Supplier}，如果给定 {@link RedisConfiguration} 则返回其结果不是 {@link #isDatabaseIndexAware(RedisConfiguration) database aware}
     * @return 从不 {@literal null}
     * @throws IllegalArgumentException 如果 {@code other} 为 {@literal null}
     */
    static Integer getDatabaseOrElse(RedisConfiguration configuration, Supplier<Integer> other) {
        Assert.notNull(other, "Other must not be null!");
        return isDatabaseIndexAware(configuration) ? ((WithDatabaseIndex) configuration).getDatabase() : other.get();
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @param other         一个 {@code Supplier}，如果给定 {@link RedisConfiguration} 则返回其结果不是 {@link #isAuthenticationAware(RedisConfiguration) password aware}.
     * @return 可以是 {@literal null}。
     * @throws IllegalArgumentException 如果 {@code other} 为 {@literal null}
     */
    static String getUsernameOrElse(RedisConfiguration configuration, Supplier<String> other) {
        Assert.notNull(other, "Other must not be null!");
        return isAuthenticationAware(configuration) ? ((WithAuthentication) configuration).getUsername() : other.get();
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @param other         一个 {@code Supplier}，如果给定 {@link RedisConfiguration} 则返回其结果不是 {@link #isAuthenticationAware(RedisConfiguration) password aware}.
     * @return 从不 {@literal null}
     * @throws IllegalArgumentException 如果 {@code other} 为 {@literal null}
     */
    static RedisPassword getPasswordOrElse(RedisConfiguration configuration, Supplier<RedisPassword> other) {
        Assert.notNull(other, "Other must not be null!");
        return isAuthenticationAware(configuration) ? ((WithAuthentication) configuration).getPassword() : other.get();
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @param other         {@code Supplier}，如果给定 {@link RedisConfiguration} 则返回结果不是 {@link #isHostAndPortAware(RedisConfiguration) port aware}
     * @return 从不 {@literal null}
     * @throws IllegalArgumentException 如果 {@code other} 为 {@literal null}
     */
    static int getPortOrElse(RedisConfiguration configuration, IntSupplier other) {
        Assert.notNull(other, "Other must not be null!");
        return isHostAndPortAware(configuration) ? ((WithHostAndPort) configuration).getPort() : other.getAsInt();
    }

    /**
     * @param configuration 可以是 {@literal null}。
     * @param other         {@code Supplier}，如果给定 {@link RedisConfiguration} 则返回其结果不是 {@link #isHostAndPortAware(RedisConfiguration) host aware}.
     * @return 从不 {@literal null}
     * @throws IllegalArgumentException 如果 {@code other} 为 {@literal null}
     */
    static String getHostOrElse(RedisConfiguration configuration, Supplier<String> other) {
        Assert.notNull(other, "Other must not be null!");
        return isHostAndPortAware(configuration) ? ((WithHostAndPort) configuration).getHostName() : other.get();
    }

    /**
     * {@link RedisConfiguration} 部分适用于连接时可能使用身份验证的配置
     */
    interface WithAuthentication {
        /**
         * 使用给定的 {@link String} 创建并设置用户名。需要 Redis 6 或更高版本
         *
         * @param username 用户名
         */
        void setUsername(String username);

        /**
         * 为给定的 {@link String} 创建并设置 {@link RedisPassword}
         *
         * @param password 可以是 {@literal null}。
         */
        default void setPassword(String password) {
            setPassword(RedisPassword.of(password));
        }

        /**
         * 为给定的 {@link String} 创建并设置 {@link RedisPassword}
         *
         * @param password 可以是 {@literal null}。
         */
        default void setPassword(char[] password) {
            setPassword(RedisPassword.of(password));
        }

        /**
         * 为给定的 {@link String} 创建并设置 {@link RedisPassword}
         *
         * @param password 不得为 {@literal null} 使用 {@link RedisPassword#none()} 代替。
         */
        void setPassword(RedisPassword password);

        /**
         * 获取连接时要使用的用户名
         *
         * @return {@literal null} 如果未设置
         */
        String getUsername();

        /**
         * 获取连接时要使用的 RedisPassword
         *
         * @return {@link RedisPassword#none()} 如果未设置
         */
        RedisPassword getPassword();
    }

    /**
     * {@link RedisConfiguration} 部分适用于连接时可能使用身份验证的配置
     */
    interface WithPassword extends WithAuthentication {
    }

    /**
     * {@link RedisConfiguration} 部分适用于使用特定数据库的配置
     */
    interface WithDatabaseIndex {
        /**
         * 设置要使用的数据库索引
         */
        void setDatabase(int dbIndex);

        /**
         * 获取要使用的数据库索引
         *
         * @return {@code 0} 默认
         */
        int getDatabase();
    }

    /**
     * {@link RedisConfiguration} 部分适用于使用主机端口组合进行连接的配置
     */
    interface WithHostAndPort {
        /**
         * 设置 Redis 服务器主机名
         *
         * @param hostName 不得为 {@literal null}
         */
        void setHostName(String hostName);

        /**
         * @return 从不 {@literal null}
         */
        String getHostName();

        /**
         * 设置 Redis 服务器端口
         */
        void setPort(int port);

        /**
         * 获取 Redis 服务器端口
         */
        int getPort();
    }

    /**
     * {@link RedisConfiguration} 部分适用于使用本机域套接字进行连接的配置
     */
    interface WithDomainSocket {
        /**
         * 设置 socket
         *
         * @param socket Redis socket的路径。不得为 {@literal null}
         */
        void setSocket(String socket);

        /**
         * 获取域 socket
         *
         * @return Redis socket 的路径
         */
        String getSocket();
    }

    /**
     * 适用于 Redis 哨兵环境的配置界面
     */
    interface SentinelConfiguration extends WithDatabaseIndex, WithPassword {
        /**
         * 设置主节点的名称
         *
         * @param name 不得为 {@literal null}
         */
        default void setMaster(String name) {
            Assert.notNull(name, "Name of sentinel master must not be null.");
            setMaster(new SentinelMasterId(name));
        }

        /**
         * 设置主节点
         *
         * @param master 不得为 {@literal null}
         */
        void setMaster(NamedNode master);

        /**
         * 获取 {@literal Sentinel} 主节点
         *
         * @return 获取主节点，如果未设置，则获取 {@literal null}
         */
        NamedNode getMaster();

        /**
         * 返回 {@link Collections#unmodifiableSet(Set)} of {@literal Sentinels}
         *
         * @return {@link Set} 的哨兵。从不 {@literal null}
         */
        Set<RedisNode> getSentinels();

        /**
         * 获取使用 Redis 服务器进行身份验证时使用的用户名。
         *
         * @return 如果未设置，可以为 {@literal null}
         */
        default String getDataNodeUsername() {
            return getUsername();
        }

        /**
         * 获取使用 Redis 服务器进行身份验证时使用的 {@link RedisPassword}
         *
         * @return 从不 {@literal null}
         */
        default RedisPassword getDataNodePassword() {
            return getPassword();
        }

        /**
         * 创建并设置一个 {@link RedisPassword}，以便在从给定的 {@link String} 使用 Redis Sentinel 进行身份验证时使用
         *
         * @param password 可以是 {@literal null}。
         */
        default void setSentinelPassword(String password) {
            setSentinelPassword(RedisPassword.of(password));
        }

        /**
         * 创建并设置一个 {@link RedisPassword}，以便在从给定的 {@link Character} 序列中使用 Redis Sentinel 进行身份验证
         *
         * @param password 可以是 {@literal null}。
         */
        default void setSentinelPassword(char[] password) {
            setSentinelPassword(RedisPassword.of(password));
        }

        /**
         * 设置使用 Redis Sentinel 进行身份验证时要使用的 {@link RedisPassword}
         *
         * @param password 不得为 {@literal null} 使用 {@link RedisPassword#none()} 代替
         */
        void setSentinelPassword(RedisPassword password);

        /**
         * 返回连接到 Redis Sentinel 时要使用的 {@link RedisPassword}。 <br />
         * 可以通过 {@link #setSentinelPassword(RedisPassword)} 或 {@link RedisPassword#none（）} 设置（如果未设置密码）。
         *
         * @return 用于使用 Redis Sentinel 进行身份验证的 {@link RedisPassword}
         */
        RedisPassword getSentinelPassword();
    }

    /**
     * 适用于 Redis 集群环境的配置界面。
     */
    interface ClusterConfiguration extends WithPassword {
        /**
         * 返回 {@link Collections#unmodifiableSet(Set)} 的 {@literal cluster nodes}
         *
         * @return {@link Set} 的节点。从不 {@literal null}
         */
        Set<RedisNode> getClusterNodes();

        /**
         * @return 要关注的最大重定向数或 {@literal null}（如果未设置）
         */
        Integer getMaxRedirects();
    }

    /**
     * 配置界面适用于固定主机的 Redis 主从环境。 <br/>
     * Redis 正在经历命名法更改，其中术语副本同义地用于奴隶。
     */
    interface StaticMasterReplicaConfiguration extends WithDatabaseIndex, WithPassword {
        /**
         * @return 不可修改 {@link RedisStandaloneConfiguration nodes} {@link List}.
         */
        List<RedisStandaloneConfiguration> getNodes();
    }

    /**
     * 适用于使用本地 unix 域套接字的单节点 Redis 连接的配置接口
     */
    interface DomainSocketConfiguration extends WithDomainSocket, WithDatabaseIndex, WithPassword {
    }
}
