package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link LettucePoolingClientConfiguration} 的默认实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:20 <br/>
 */
class DefaultLettucePoolingClientConfiguration implements LettucePoolingClientConfiguration {
    private final LettuceClientConfiguration clientConfiguration;
    @SuppressWarnings("rawtypes")
    private final GenericObjectPoolConfig poolConfig;

    @SuppressWarnings("rawtypes")
    DefaultLettucePoolingClientConfiguration(LettuceClientConfiguration clientConfiguration, GenericObjectPoolConfig poolConfig) {
        this.clientConfiguration = clientConfiguration;
        this.poolConfig = poolConfig;
    }

    @Override
    public boolean isUseSsl() {
        return clientConfiguration.isUseSsl();
    }

    @Override
    public boolean isVerifyPeer() {
        return clientConfiguration.isVerifyPeer();
    }

    @Override
    public boolean isStartTls() {
        return clientConfiguration.isStartTls();
    }

    @Override
    public Optional<ClientResources> getClientResources() {
        return clientConfiguration.getClientResources();
    }

    @Override
    public Optional<ClientOptions> getClientOptions() {
        return clientConfiguration.getClientOptions();
    }

    @Override
    public Optional<String> getClientName() {
        return clientConfiguration.getClientName();
    }

    @Override
    public Optional<ReadFrom> getReadFrom() {
        return clientConfiguration.getReadFrom();
    }

    @Override
    public Duration getCommandTimeout() {
        return clientConfiguration.getCommandTimeout();
    }

    @Override
    public Duration getShutdownTimeout() {
        return clientConfiguration.getShutdownTimeout();
    }

    @Override
    public Duration getShutdownQuietPeriod() {
        return clientConfiguration.getShutdownQuietPeriod();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public GenericObjectPoolConfig getPoolConfig() {
        return poolConfig;
    }
}
