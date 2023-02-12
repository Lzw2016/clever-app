package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Lettuce的 Redis 客户端配置。此配置提供可选的配置元素，例如特定于 Lettuce 客户端功能的 {@link ClientResources} 和 {@link ClientOptions}
 * <p>
 * 提供可选元素允许对客户端进行更具体的配置：
 * <ul>
 * <li>是否使用 SSL</li>
 * <li>是否使用 SSL 验证对等点</li>
 * <li>是否使用 StartTLS</li>
 * <li>可选的 {@link ClientResources}</li>
 * <li>可选的 {@link ClientOptions}，默认为启用 {@link TimeoutOptions} 的 {@link ClientOptions}</li>
 * <li>可选的客户名称</li>
 * <li>可选的 {@link ReadFrom}。如果已配置，则启用 MasterReplica 操作</li>
 * <li>客户端 {@link Duration timeout}</li>
 * <li>关机 {@link Duration timeout}</li>
 * <li>关机安静 {@link Duration period}</li>
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:11 <br/>
 *
 * @see org.clever.data.redis.connection.RedisStandaloneConfiguration
 * @see org.clever.data.redis.connection.RedisSentinelConfiguration
 * @see org.clever.data.redis.connection.RedisClusterConfiguration
 */
public interface LettuceClientConfiguration {
    /**
     * @return {@literal true} 使用 SSL，{@literal false} 使用未加密的连接
     */
    boolean isUseSsl();

    /**
     * @return {@literal true} 在使用 {@link #isUseSsl() SSL} 时验证对等点
     */
    boolean isVerifyPeer();

    /**
     * @return {@literal true} 使用 Start TLS（{@code true} 如果第一个写入请求不应该被加密）
     */
    boolean isStartTls();

    /**
     * @return 可选的 {@link ClientResources}
     */
    Optional<ClientResources> getClientResources();

    /**
     * @return 可选的 {@link io.lettuce.core.ClientOptions}.
     */
    Optional<ClientOptions> getClientOptions();

    /**
     * @return 使用 {@code CLIENT SETNAME} 设置的可选客户端名称
     */
    Optional<String> getClientName();

    /**
     * 注意：Redis 正在进行术语更改，其中术语副本与从属同义
     *
     * @return 可选的 {@link io.lettuce.core.ReadFrom} 设置
     */
    Optional<ReadFrom> getReadFrom();

    /**
     * @return 超时
     */
    Duration getCommandTimeout();

    /**
     * @return 用于关闭客户端的关闭超时
     * @see io.lettuce.core.AbstractRedisClient#shutdown(long, long, TimeUnit)
     */
    Duration getShutdownTimeout();

    /**
     * @return 用于关闭客户端的关闭静默期
     * @see io.lettuce.core.AbstractRedisClient#shutdown(long, long, TimeUnit)
     */
    Duration getShutdownQuietPeriod();

    /**
     * 创建一个新的 {@link LettuceClientConfigurationBuilder} 来构建 {@link LettuceClientConfiguration} 以与 Lettuce 客户端一起使用
     *
     * @return 一个新的 {@link LettuceClientConfigurationBuilder} 来构建 {@link LettuceClientConfiguration}
     */
    static LettuceClientConfigurationBuilder builder() {
        return new LettuceClientConfigurationBuilder();
    }

    /**
     * 使用以下内容创建默认的 {@link LettuceClientConfiguration}：
     * <dl>
     * <dt>SSL</dt>
     * <dd>no</dd>
     * <dt>对等验证</dt>
     * <dd>yes</dd>
     * <dt>启动 TLS</dt>
     * <dd>no</dd>
     * <dt>客户端选项</dt>
     * <dd>{@link ClientOptions} with enabled {@link io.lettuce.core.TimeoutOptions}</dd>
     * <dt>客户资源</dt>
     * <dd>none</dd>
     * <dt>客户名称</dt>
     * <dd>none</dd>
     * <dt>读自</dt>
     * <dd>none</dd>
     * <dt>连接超时</dt>
     * <dd>60 秒</dd>
     * <dt>关机超时</dt>
     * <dd>100 毫秒</dd>
     * <dt>关断静默期</dt>
     * <dd>100 毫秒</dd>
     * </dl>
     *
     * @return 具有默认值的 {@link LettuceClientConfiguration}
     */
    static LettuceClientConfiguration defaultConfiguration() {
        return builder().build();
    }

    class LettuceClientConfigurationBuilder {
        boolean useSsl;
        boolean verifyPeer = true;
        boolean startTls;
        ClientResources clientResources;
        ClientOptions clientOptions = ClientOptions.builder().timeoutOptions(TimeoutOptions.enabled()).build();
        String clientName;
        ReadFrom readFrom;
        Duration timeout = Duration.ofSeconds(RedisURI.DEFAULT_TIMEOUT);
        Duration shutdownTimeout = Duration.ofMillis(100);
        Duration shutdownQuietPeriod;

        LettuceClientConfigurationBuilder() {
        }

        /**
         * 从 {@link RedisURI} 应用 SSL 设置、命令超时和客户端名称
         *
         * @param redisUri 连接 URI
         * @return {@literal this} builder
         */
        public LettuceClientConfigurationBuilder apply(RedisURI redisUri) {
            this.useSsl = redisUri.isSsl();
            this.verifyPeer = redisUri.isVerifyPeer();
            this.startTls = redisUri.isStartTls();
            if (!redisUri.getTimeout().equals(RedisURI.DEFAULT_TIMEOUT_DURATION)) {
                this.timeout = redisUri.getTimeout();
            }
            if (!ObjectUtils.isEmpty(redisUri.getClientName())) {
                this.clientName = redisUri.getClientName();
            }
            return this;
        }

        /**
         * 启用 SSL 连接
         *
         * @return {@link LettuceSslClientConfigurationBuilder}.
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceSslClientConfigurationBuilder useSsl() {
            this.useSsl = true;
            return new LettuceSslClientConfigurationBuilder(this);
        }

        /**
         * 配置 {@link ClientResources}
         *
         * @param clientResources 不得为 {@literal null}
         * @return {@literal this} builder
         * @throws IllegalArgumentException 如果 clientResources 是 {@literal null}
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceClientConfigurationBuilder clientResources(ClientResources clientResources) {
            Assert.notNull(clientResources, "ClientResources must not be null!");
            this.clientResources = clientResources;
            return this;
        }

        /**
         * 配置 {@link ClientOptions}
         *
         * @param clientOptions 不得为 {@literal null}
         * @return {@literal this} builder
         * @throws IllegalArgumentException 如果 clientOptions 是 {@literal null}
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceClientConfigurationBuilder clientOptions(ClientOptions clientOptions) {
            Assert.notNull(clientOptions, "ClientOptions must not be null!");
            this.clientOptions = clientOptions;
            return this;
        }

        /**
         * 配置{@link ReadFrom}。如果已配置，则启用 MasterReplica 操作。 <br/>
         * 注意：Redis 正在进行术语更改，其中术语副本与从属同义。
         *
         * @param readFrom 不得为 {@literal null}
         * @return {@literal this} builder
         * @throws IllegalArgumentException 如果 clientOptions 是 {@literal null}
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceClientConfigurationBuilder readFrom(ReadFrom readFrom) {
            Assert.notNull(readFrom, "ReadFrom must not be null!");
            this.readFrom = readFrom;
            return this;
        }

        /**
         * 配置 {@code clientName} 以使用 {@code CLIENT SETNAME} 进行设置
         *
         * @param clientName 不得为 {@literal null} 或为空
         * @return {@literal this} builder
         * @throws IllegalArgumentException 如果 clientName 是 {@literal null} 或为空
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceClientConfigurationBuilder clientName(String clientName) {
            Assert.hasText(clientName, "Client name must not be null or empty!");
            this.clientName = clientName;
            return this;
        }

        /**
         * 配置命令超时
         *
         * @param timeout 不得为 {@literal null}
         * @return {@literal this} builder
         * @throws IllegalArgumentException 如果超时是 {@literal null}
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceClientConfigurationBuilder commandTimeout(Duration timeout) {
            Assert.notNull(timeout, "Duration must not be null!");
            this.timeout = timeout;
            return this;
        }

        /**
         * 配置关机超时
         *
         * @param shutdownTimeout 不得为 {@literal null}
         * @return {@literal this} builder
         * @throws IllegalArgumentException 如果 shutdownTimeout 是 {@literal null}
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceClientConfigurationBuilder shutdownTimeout(Duration shutdownTimeout) {
            Assert.notNull(shutdownTimeout, "Duration must not be null!");
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        /**
         * 配置关机静默期
         *
         * @param shutdownQuietPeriod 不得为 {@literal null}
         * @return {@literal this} builder
         * @throws IllegalArgumentException 如果 shutdownQuietPeriod 是 {@literal null}
         */
        @SuppressWarnings("UnusedReturnValue")
        public LettuceClientConfigurationBuilder shutdownQuietPeriod(Duration shutdownQuietPeriod) {
            Assert.notNull(shutdownQuietPeriod, "Duration must not be null!");
            this.shutdownQuietPeriod = shutdownQuietPeriod;
            return this;
        }

        /**
         * 使用从此构建器应用的配置构建 {@link LettuceClientConfiguration}
         *
         * @return 一个新的 {@link LettuceClientConfiguration} 对象
         */
        public LettuceClientConfiguration build() {
            return new DefaultLettuceClientConfiguration(
                    useSsl,
                    verifyPeer,
                    startTls,
                    clientResources,
                    clientOptions,
                    clientName,
                    readFrom,
                    timeout,
                    shutdownTimeout,
                    shutdownQuietPeriod
            );
        }
    }

    /**
     * SSL 相关 {@link LettuceClientConfiguration} 的构建器
     */
    @SuppressWarnings("UnusedReturnValue")
    class LettuceSslClientConfigurationBuilder {
        private final LettuceClientConfigurationBuilder delegate;

        LettuceSslClientConfigurationBuilder(LettuceClientConfigurationBuilder delegate) {
            Assert.notNull(delegate, "Delegate client configuration builder must not be null!");
            this.delegate = delegate;
        }

        /**
         * 禁用对等验证
         *
         * @return {@literal this} builder
         */
        public LettuceSslClientConfigurationBuilder disablePeerVerification() {
            delegate.verifyPeer = false;
            return this;
        }

        /**
         * 启用 Start TLS 以发送未加密的第一个字节
         *
         * @return {@literal this} builder
         */
        public LettuceSslClientConfigurationBuilder startTls() {
            delegate.startTls = true;
            return this;
        }

        /**
         * 返回 {@link LettuceClientConfigurationBuilder}
         *
         * @return {@link LettuceClientConfigurationBuilder}
         */
        public LettuceClientConfigurationBuilder and() {
            return delegate;
        }

        /**
         * 使用从此构建器应用的配置构建 {@link LettuceClientConfiguration}
         *
         * @return 一个新的 {@link LettuceClientConfiguration} 对象
         */
        public LettuceClientConfiguration build() {
            return delegate.build();
        }
    }
}
