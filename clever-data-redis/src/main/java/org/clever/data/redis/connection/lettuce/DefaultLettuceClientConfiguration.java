package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.resource.ClientResources;

import java.time.Duration;
import java.util.Optional;

/**
 * {@literal LettuceClientConfiguration} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:12 <br/>
 */
class DefaultLettuceClientConfiguration implements LettuceClientConfiguration {
    private final boolean useSsl;
    private final boolean verifyPeer;
    private final boolean startTls;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<ClientResources> clientResources;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<ClientOptions> clientOptions;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> clientName;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<ReadFrom> readFrom;
    private final Duration timeout;
    private final Duration shutdownTimeout;
    private final Duration shutdownQuietPeriod;

    DefaultLettuceClientConfiguration(boolean useSsl,
                                      boolean verifyPeer,
                                      boolean startTls,
                                      ClientResources clientResources,
                                      ClientOptions clientOptions,
                                      String clientName,
                                      ReadFrom readFrom,
                                      Duration timeout,
                                      Duration shutdownTimeout,
                                      Duration shutdownQuietPeriod) {
        this.useSsl = useSsl;
        this.verifyPeer = verifyPeer;
        this.startTls = startTls;
        this.clientResources = Optional.ofNullable(clientResources);
        this.clientOptions = Optional.ofNullable(clientOptions);
        this.clientName = Optional.ofNullable(clientName);
        this.readFrom = Optional.ofNullable(readFrom);
        this.timeout = timeout;
        this.shutdownTimeout = shutdownTimeout;
        this.shutdownQuietPeriod = shutdownQuietPeriod != null ? shutdownQuietPeriod : shutdownTimeout;
    }

    @Override
    public boolean isUseSsl() {
        return useSsl;
    }

    @Override
    public boolean isVerifyPeer() {
        return verifyPeer;
    }

    @Override
    public boolean isStartTls() {
        return startTls;
    }

    @Override
    public Optional<ClientResources> getClientResources() {
        return clientResources;
    }

    @Override
    public Optional<ClientOptions> getClientOptions() {
        return clientOptions;
    }

    @Override
    public Optional<String> getClientName() {
        return clientName;
    }

    @Override
    public Optional<ReadFrom> getReadFrom() {
        return readFrom;
    }

    @Override
    public Duration getCommandTimeout() {
        return timeout;
    }

    @Override
    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    @Override
    public Duration getShutdownQuietPeriod() {
        return shutdownQuietPeriod;
    }
}
