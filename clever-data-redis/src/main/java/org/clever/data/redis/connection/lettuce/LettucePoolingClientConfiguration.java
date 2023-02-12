package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.util.Assert;

import java.time.Duration;

/**
 * 通过向 {@link LettuceClientConfiguration} 添加池特定配置，使用驱动程序级池连接为 lettuce 重新分配客户端配置。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:19 <br/>
 */
public interface LettucePoolingClientConfiguration extends LettuceClientConfiguration {
    /**
     * @return {@link GenericObjectPoolConfig}。永不为 {@literal null}
     */
    GenericObjectPoolConfig<StatefulConnection<?, ?>> getPoolConfig();

    /**
     * 创建一个新的 {@link LettucePoolingClientConfigurationBuilder} 来构建 {@link LettucePoolingClientConfiguration} 以与 Lettuce 客户端一起使用
     *
     * @return 一个新的 {@link LettucePoolingClientConfigurationBuilder} 来构建 {@link LettucePoolingClientConfiguration}
     */
    static LettucePoolingClientConfigurationBuilder builder() {
        return new LettucePoolingClientConfigurationBuilder();
    }

    /**
     * 创建一个默认的 {@link LettucePoolingClientConfiguration}
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
     * <dd>60 Seconds</dd>
     * <dt>关机超时</dt>
     * <dd>100 毫秒</dd>
     * <dt>关断静默期</dt>
     * <dd>100 毫秒</dd>
     * <dt>连接池配置</dt>
     * <dd>默认 {@link GenericObjectPoolConfig}</dd>
     * </dl>
     *
     * @return 具有默认值的 {@link LettucePoolingClientConfiguration}
     */
    static LettucePoolingClientConfiguration defaultConfiguration() {
        return builder().build();
    }

    class LettucePoolingClientConfigurationBuilder extends LettuceClientConfigurationBuilder {
        @SuppressWarnings("rawtypes")
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

        LettucePoolingClientConfigurationBuilder() {
            super();
        }

        @Override
        public LettucePoolingSslClientConfigurationBuilder useSsl() {
            super.useSsl();
            return new LettucePoolingSslClientConfigurationBuilder(this);
        }

        @Override
        public LettucePoolingClientConfigurationBuilder clientResources(ClientResources clientResources) {
            super.clientResources(clientResources);
            return this;
        }

        @Override
        public LettucePoolingClientConfigurationBuilder clientOptions(ClientOptions clientOptions) {
            super.clientOptions(clientOptions);
            return this;
        }

        @Override
        public LettucePoolingClientConfigurationBuilder commandTimeout(Duration timeout) {
            super.commandTimeout(timeout);
            return this;
        }

        @Override
        public LettucePoolingClientConfigurationBuilder shutdownTimeout(Duration shutdownTimeout) {
            super.shutdownTimeout(shutdownTimeout);
            return this;
        }

        @Override
        public LettucePoolingClientConfigurationBuilder shutdownQuietPeriod(Duration shutdownQuietPeriod) {
            super.shutdownQuietPeriod(shutdownQuietPeriod);
            return this;
        }

        @Override
        public LettucePoolingClientConfigurationBuilder readFrom(ReadFrom readFrom) {
            super.readFrom(readFrom);
            return this;
        }

        @Override
        public LettucePoolingClientConfigurationBuilder clientName(String clientName) {
            super.clientName(clientName);
            return this;
        }

        /**
         * 设置驱动程序使用的 {@link GenericObjectPoolConfig}
         *
         * @param poolConfig 不得为 {@literal null}
         */
        @SuppressWarnings("rawtypes")
        public LettucePoolingClientConfigurationBuilder poolConfig(GenericObjectPoolConfig poolConfig) {
            Assert.notNull(poolConfig, "PoolConfig must not be null!");
            this.poolConfig = poolConfig;
            return this;
        }

        @Override
        public LettucePoolingClientConfiguration build() {
            return new DefaultLettucePoolingClientConfiguration(super.build(), poolConfig);
        }
    }

    class LettucePoolingSslClientConfigurationBuilder extends LettuceSslClientConfigurationBuilder {
        LettucePoolingSslClientConfigurationBuilder(LettucePoolingClientConfigurationBuilder delegate) {
            super(delegate);
        }

        @Override
        public LettucePoolingClientConfigurationBuilder and() {
            return (LettucePoolingClientConfigurationBuilder) super.and();
        }

        @Override
        public LettucePoolingSslClientConfigurationBuilder disablePeerVerification() {
            super.disablePeerVerification();
            return this;
        }

        @Override
        public LettucePoolingSslClientConfigurationBuilder startTls() {
            super.startTls();
            return this;
        }

        @Override
        public LettucePoolingClientConfiguration build() {
            return (LettucePoolingClientConfiguration) super.build();
        }
    }
}
