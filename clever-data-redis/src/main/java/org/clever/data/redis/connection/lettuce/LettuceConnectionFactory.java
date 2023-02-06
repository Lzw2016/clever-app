package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.resource.ClientResources;
import org.clever.beans.factory.DisposableBean;
import org.clever.beans.factory.InitializingBean;
import org.clever.dao.DataAccessException;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.data.redis.ExceptionTranslationStrategy;
import org.clever.data.redis.PassThroughExceptionTranslationStrategy;
import org.clever.data.redis.RedisConnectionFailureException;
import org.clever.data.redis.connection.*;
import org.clever.data.redis.connection.RedisConfiguration.ClusterConfiguration;
import org.clever.data.redis.connection.RedisConfiguration.DomainSocketConfiguration;
import org.clever.data.redis.connection.RedisConfiguration.WithDatabaseIndex;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.clever.data.redis.connection.lettuce.LettuceConnection.CODEC;
import static org.clever.data.redis.connection.lettuce.LettuceConnection.PipeliningFlushPolicy;

/**
 * 连接工厂创建基于 <a href="https://github.com/mp911de/lettuce">Lettuce</a> 的连接。
 * <p>
 * 该工厂在每次调用 {@link #getConnection()} 时创建一个新的 {@link LettuceConnection}。
 * 默认情况下，多个 {@link LettuceConnection} 共享一个线程安全的本机连接。
 * <p>
 * {@link LettuceConnection} 永远不会关闭共享本机连接，因此默认情况下不会在 {@link #getConnection()} 上对其进行验证。
 * 如有必要，请使用 {@link #setValidateConnection(boolean)} 更改此行为。
 * 注入一个 {@link Pool} 来池化专用连接。如果 shareNativeConnection 为 true，则池将用于选择一个连接以仅用于阻塞和 tx 操作，不应共享连接。
 * 如果禁用本机连接共享，则所选连接将用于所有操作。
 * <p>
 * {@link LettuceConnectionFactory} 应该使用环境配置和 {@link LettuceConnectionFactory 客户端配置} 进行配置。
 * Lettuce 支持以下环境配置：
 * <ul>
 * <li>{@link RedisStandaloneConfiguration}</li>
 * <li>{@link RedisStaticMasterReplicaConfiguration}</li>
 * <li>{@link RedisSocketConfiguration}</li>
 * <li>{@link RedisSentinelConfiguration}</li>
 * <li>{@link RedisClusterConfiguration}</li>
 * </ul>
 * <p>
 * 此连接工厂必须在 {@link #getConnection() 获取连接} 之前 {@link #afterPropertiesSet() 已初始化}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:13 <br/>
 */
public class LettuceConnectionFactory implements InitializingBean, DisposableBean, RedisConnectionFactory {
    private static final ExceptionTranslationStrategy EXCEPTION_TRANSLATION = new PassThroughExceptionTranslationStrategy(LettuceConverters.exceptionConverter());

    /**
     * 根据以下内容基于 {@link String URI} 创建 {@link RedisConfiguration}：
     * <ul>
     * <li>如果 {@code redisUri} 包含哨兵，则返回 {@link RedisSentinelConfiguration}</li>
     * <li>如果 {@code redisUri} 有配置的套接字，则返回 {@link RedisSocketConfiguration}</li>
     * <li>否则返回 {@link RedisStandaloneConfiguration}</li>
     * </ul>
     *
     * @param redisUri {@link RedisURI} 格式的连接 URI
     * @return 表示 Redis URI 的适当 {@link RedisConfiguration} 实例
     * @see RedisURI
     */
    public static RedisConfiguration createRedisConfiguration(String redisUri) {
        Assert.hasText(redisUri, "RedisURI must not be null and not empty");
        return createRedisConfiguration(RedisURI.create(redisUri));
    }

    /**
     * 根据以下内容基于 {@link RedisURI} 创建 {@link RedisConfiguration}：
     * <ul>
     * <li>如果 {@link RedisURI} 包含哨兵，则返回 {@link RedisSentinelConfiguration}</li>
     * <li>如果 {@link RedisURI} 有配置的套接字，则返回 {@link RedisSocketConfiguration}</li>
     * <li>否则返回 {@link RedisStandaloneConfiguration}</li>
     * </ul>
     *
     * @param redisUri 连接 URI
     * @return 表示 Redis URI 的适当 {@link RedisConfiguration} 实例
     * @see RedisURI
     */
    public static RedisConfiguration createRedisConfiguration(RedisURI redisUri) {
        Assert.notNull(redisUri, "RedisURI must not be null");
        if (!ObjectUtils.isEmpty(redisUri.getSentinels())) {
            return LettuceConverters.createRedisSentinelConfiguration(redisUri);
        }
        if (!ObjectUtils.isEmpty(redisUri.getSocket())) {
            return LettuceConverters.createRedisSocketConfiguration(redisUri);
        }
        return LettuceConverters.createRedisStandaloneConfiguration(redisUri);
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean validateConnection = false;
    private boolean shareNativeConnection = true;
    private boolean eagerInitialization = false;
    private boolean convertPipelineAndTxResults = true;
    private PipeliningFlushPolicy pipeliningFlushPolicy = PipeliningFlushPolicy.flushEachCommand();
    private final LettuceClientConfiguration clientConfiguration;
    private RedisConfiguration configuration;
    private RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration("localhost", 6379);
    private AbstractRedisClient client;
    private LettuceConnectionProvider connectionProvider;
    private ClusterCommandExecutor clusterCommandExecutor;
    // 共享连接
    private SharedConnection<byte[]> connection;
    // 共享连接的同步监视器
    private final Object connectionMonitor = new Object();
    private boolean initialized;
    private boolean destroyed;

    /**
     * 给定 {@link LettuceClientConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param clientConfig must not be {@literal null}
     */
    private LettuceConnectionFactory(LettuceClientConfiguration clientConfig) {
        Assert.notNull(clientConfig, "LettuceClientConfiguration must not be null!");
        this.clientConfiguration = clientConfig;
        this.configuration = this.standaloneConfig;
    }

    /**
     * 使用默认设置构造一个新的 {@link LettuceConnectionFactory} 实例
     */
    public LettuceConnectionFactory() {
        this(new MutableLettuceClientConfiguration());
    }

    /**
     * 使用给定的 {@link RedisStaticMasterReplicaConfiguration} 和 {@link LettuceClientConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param redisConfiguration 不得为 {@literal null}
     * @param clientConfig       不得为 {@literal null}
     */
    public LettuceConnectionFactory(RedisConfiguration redisConfiguration, LettuceClientConfiguration clientConfig) {
        this(clientConfig);
        Assert.notNull(redisConfiguration, "RedisConfiguration must not be null!");
        this.configuration = redisConfiguration;
    }

    /**
     * 使用给定的 {@link RedisSocketConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param redisConfiguration 不得为 {@literal null}
     */
    public LettuceConnectionFactory(RedisConfiguration redisConfiguration) {
        this(redisConfiguration, new MutableLettuceClientConfiguration());
    }

    /**
     * 使用给定的 {@link RedisStandaloneConfiguration} 和 {@link LettuceClientConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param standaloneConfig 不得为 {@literal null}
     * @param clientConfig     不得为 {@literal null}
     */
    public LettuceConnectionFactory(RedisStandaloneConfiguration standaloneConfig, LettuceClientConfiguration clientConfig) {
        this(clientConfig);
        Assert.notNull(standaloneConfig, "RedisStandaloneConfiguration must not be null!");
        this.standaloneConfig = standaloneConfig;
        this.configuration = this.standaloneConfig;
    }

    /**
     * 使用默认设置构造一个新的 {@link LettuceConnectionFactory} 实例
     */
    public LettuceConnectionFactory(RedisStandaloneConfiguration configuration) {
        this(configuration, new MutableLettuceClientConfiguration());
    }

    /**
     * 使用默认设置构造一个新的 {@link LettuceConnectionFactory} 实例
     */
    public LettuceConnectionFactory(String host, int port) {
        this(new RedisStandaloneConfiguration(host, port), new MutableLettuceClientConfiguration());
    }

    /**
     * 使用给定的 {@link RedisSentinelConfiguration} 和 {@link LettuceClientConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param sentinelConfiguration 不得为 {@literal null}
     * @param clientConfig          不得为 {@literal null}
     */
    public LettuceConnectionFactory(RedisSentinelConfiguration sentinelConfiguration, LettuceClientConfiguration clientConfig) {
        this(clientConfig);
        Assert.notNull(sentinelConfiguration, "RedisSentinelConfiguration must not be null!");
        this.configuration = sentinelConfiguration;
    }

    /**
     * 使用给定的 {@link RedisSentinelConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param sentinelConfiguration 不得为 {@literal null}
     */
    public LettuceConnectionFactory(RedisSentinelConfiguration sentinelConfiguration) {
        this(sentinelConfiguration, new MutableLettuceClientConfiguration());
    }

    /**
     * 使用给定的 {@link RedisClusterConfiguration} 和 {@link LettuceClientConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param clusterConfiguration 不得为 {@literal null}
     * @param clientConfig         不得为 {@literal null}
     */
    public LettuceConnectionFactory(RedisClusterConfiguration clusterConfiguration, LettuceClientConfiguration clientConfig) {
        this(clientConfig);
        Assert.notNull(clusterConfiguration, "RedisClusterConfiguration must not be null!");
        this.configuration = clusterConfiguration;
    }

    /**
     * 使用应用于创建 {@link RedisClusterClient} 的给定 {@link RedisClusterConfiguration} 构造一个新的 {@link LettuceConnectionFactory} 实例
     *
     * @param clusterConfiguration 不得为 {@literal null}
     */
    public LettuceConnectionFactory(RedisClusterConfiguration clusterConfiguration) {
        this(clusterConfiguration, new MutableLettuceClientConfiguration());
    }

    /**
     * 初始化 LettuceConnectionFactory
     */
    @Override
    public void afterPropertiesSet() {
        this.client = createClient();
        this.connectionProvider = new ExceptionTranslatingConnectionProvider(createConnectionProvider(client, CODEC));
        if (isClusterAware()) {
            this.clusterCommandExecutor = new ClusterCommandExecutor(
                    new LettuceClusterTopologyProvider((RedisClusterClient) client),
                    new LettuceClusterConnection.LettuceClusterNodeResourceProvider(this.connectionProvider),
                    EXCEPTION_TRANSLATION
            );
        }
        this.initialized = true;
        if (getEagerInitialization() && getShareNativeConnection()) {
            initConnection();
        }
    }

    /**
     * 销毁当前对象，释放资源
     */
    @Override
    public void destroy() {
        resetConnection();
        if (clusterCommandExecutor != null) {
            try {
                clusterCommandExecutor.destroy();
            } catch (Exception ex) {
                log.warn("Cannot properly close cluster command executor", ex);
            }
        }
        dispose(connectionProvider);
        try {
            Duration quietPeriod = clientConfiguration.getShutdownQuietPeriod();
            Duration timeout = clientConfiguration.getShutdownTimeout();
            client.shutdown(quietPeriod.toMillis(), timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn((client != null ? ClassUtils.getShortName(client.getClass()) : "LettuceClient") + " did not shut down gracefully.", e);
            }
        }
        this.destroyed = true;
    }

    // --------------------------------------------------------------------------------------------
    // 配置
    // --------------------------------------------------------------------------------------------

    /**
     * 在调用 {@link #getConnection()} 时启用对共享本地 Lettuce 连接的验证。
     * 如果验证失败，将创建并使用一个新连接。
     * <p>
     * Lettuce 将自动重新连接，直到关闭被调用，如果使用共享本机连接，这永远不会通过 {@link LettuceConnection} 发生，因此默认值为 {@literal false}。
     * <p>
     * 将此设置为 {@literal true} 将导致在每个新连接上对服务器进行往返调用，因此仅当启用连接共享并且有代码主动关闭本机 Lettuce 连接时才应使用此设置。
     *
     * @param validateConnection 启用连接验证
     */
    public void setValidateConnection(boolean validateConnection) {
        this.validateConnection = validateConnection;
    }

    /**
     * 指示是否启用了本机 Lettuce 连接的验证
     */
    public boolean getValidateConnection() {
        return validateConnection;
    }

    /**
     * 使多个 {@link LettuceConnection} 共享一个本地连接。
     * 如果设置为 {@literal false}，{@link LettuceConnection} 上的每个操作都将打开和关闭套接字。
     *
     * @param shareNativeConnection 启用连接共享
     */
    public void setShareNativeConnection(boolean shareNativeConnection) {
        this.shareNativeConnection = shareNativeConnection;
    }

    /**
     * 指示多个 {@link LettuceConnection} 是否应该共享一个本地连接
     *
     * @return 本机连接共享
     */
    public boolean getShareNativeConnection() {
        return shareNativeConnection;
    }

    /**
     * 允许立即初始化 {@link #setShareNativeConnection(boolean) 共享连接}
     *
     * @param eagerInitialization 在 {@link #afterPropertiesSet()} 上启用热切连接共享连接初始化
     */
    public void setEagerInitialization(boolean eagerInitialization) {
        this.eagerInitialization = eagerInitialization;
    }

    /**
     * 表示 {@link #setShareNativeConnection(boolean) 共享连接} 应该被急切地初始化。
     * 主动初始化需要在应用程序启动期间运行Redis实例，以便及早验证连接工厂配置。
     * 主动初始化还可以防止在使用响应式API时阻塞连接，建议用于响应式API使用。
     *
     * @return 如果共享连接是在 {@link #afterPropertiesSet()} 上初始化的，则为 {@link true}
     */
    public boolean getEagerInitialization() {
        return eagerInitialization;
    }

    /**
     * 指定是否应将管道和事务结果转换为预期的数据类型。
     * 如果为false, 则 {@link LettuceConnection#closePipeline()} 和 {@link LettuceConnection#exec()} 的结果将是Lettuce驱动程序返回的类型。
     *
     * @param convertPipelineAndTxResults 是否转换管道和 tx 结果。
     */
    public void setConvertPipelineAndTxResults(boolean convertPipelineAndTxResults) {
        this.convertPipelineAndTxResults = convertPipelineAndTxResults;
    }

    /**
     * 指定是否应将流水线处理的结果转换为预期的数据类型。
     * 如果为false，则 {@link LettuceConnection#closePipeline()} 和 {@link LettuceConnection#exec()} 的结果将是Lettuce驱动程序返回的类型。
     *
     * @return 是否转换管道和 tx 结果
     */
    @Override
    public boolean getConvertPipelineAndTxResults() {
        return convertPipelineAndTxResults;
    }

    /**
     * 使用流水线时配置刷新策略。如果未设置，默认为 {@link PipeliningFlushPolicy#flushEachCommand() flush on each command}。
     *
     * @param pipeliningFlushPolicy 控制命令何时写入 Redis 连接的刷新策略
     * @see LettuceConnection#openPipeline()
     * @see StatefulRedisConnection#flushCommands()
     */
    public void setPipeliningFlushPolicy(PipeliningFlushPolicy pipeliningFlushPolicy) {
        Assert.notNull(pipeliningFlushPolicy, "PipeliningFlushingPolicy must not be null!");
        this.pipeliningFlushPolicy = pipeliningFlushPolicy;
    }

//    /**
//     * 设置 hostname
//     *
//     * @param hostName 要设置的 hostname
//     * @deprecated since 2.0, configure the hostname using {@link RedisStandaloneConfiguration}.
//     */
//    public void setHostName(String hostName) {
//        standaloneConfig.setHostName(hostName);
//    }

    /**
     * 返回当前主机
     */
    public String getHostName() {
        return RedisConfiguration.getHostOrElse(configuration, standaloneConfig::getHostName);
    }

//    /**
//     * 设置端口
//     *
//     * @param port port
//     * @deprecated since 2.0, configure the port using {@link RedisStandaloneConfiguration}.
//     */
//    @Deprecated
//    public void setPort(int port) {
//        standaloneConfig.setPort(port);
//    }

    /**
     * 返回当前端口
     */
    public int getPort() {
        return RedisConfiguration.getPortOrElse(configuration, standaloneConfig::getPort);
    }

    /**
     * 设置此连接工厂使用的数据库的索引。默认值为 0
     *
     * @param index 数据库索引
     */
    public void setDatabase(int index) {
        Assert.isTrue(index >= 0, "invalid DB index (a positive index required)");
        if (RedisConfiguration.isDatabaseIndexAware(configuration)) {
            ((WithDatabaseIndex) configuration).setDatabase(index);
            return;
        }
        standaloneConfig.setDatabase(index);
    }

    /**
     * 返回数据库的索引
     *
     * @return 数据库索引
     */
    public int getDatabase() {
        return RedisConfiguration.getDatabaseOrElse(configuration, standaloneConfig::getDatabase);
    }

//    /**
//     * 设置连接超时（以毫秒为单位）
//     *
//     * @param timeout timeout
//     * @throws IllegalStateException 如果 {@link LettuceClientConfiguration} 是不可变的
//     * @deprecated since 2.0, configure the timeout using {@link LettuceClientConfiguration}.
//     */
//    @Deprecated
//    public void setTimeout(long timeout) {
//        getMutableConfiguration().setTimeout(Duration.ofMillis(timeout));
//    }

    /**
     * 返回连接超时（以毫秒为单位）
     */
    public long getTimeout() {
        return getClientTimeout();
    }

//    /**
//     * 设置为使用 SSL 连接
//     *
//     * @param useSsl {@literal true} 使用 SSL
//     * @throws IllegalStateException 如果 {@link LettuceClientConfiguration} 是不可变的
//     * @deprecated since 2.0, configure SSL usage using {@link LettuceClientConfiguration}.
//     */
//    @Deprecated
//    public void setUseSsl(boolean useSsl) {
//        getMutableConfiguration().setUseSsl(useSsl);
//    }

    /**
     * 返回是否使用 SSL
     */
    public boolean isUseSsl() {
        return clientConfiguration.isUseSsl();
    }

//    /**
//     * 设置为在使用 SSL 时使用验证证书有效性主机名检查
//     *
//     * @param verifyPeer {@literal false} 不验证主机名
//     * @throws IllegalStateException 如果 {@link LettuceClientConfiguration} 是不可变的
//     * @deprecated since 2.0, configure peer verification using {@link LettuceClientConfiguration}.
//     */
//    @Deprecated
//    public void setVerifyPeer(boolean verifyPeer) {
//        getMutableConfiguration().setVerifyPeer(verifyPeer);
//    }

    /**
     * 返回是否在使用 SSL 时验证证书有效性主机名检查
     *
     * @return 使用 SSL 时是否验证对等点
     */
    public boolean isVerifyPeer() {
        return clientConfiguration.isVerifyPeer();
    }

//    /**
//     * 设置为发出 StartTLS
//     *
//     * @param startTls {@literal true} 发出 StartTLS
//     * @throws IllegalStateException 如果 {@link LettuceClientConfiguration} 是不可变的
//     * @deprecated since 2.0, configure StartTLS using {@link LettuceClientConfiguration}.
//     */
//    @Deprecated
//    public void setStartTls(boolean startTls) {
//        getMutableConfiguration().setStartTls(startTls);
//    }

    /**
     * 返回是否发出 StartTLS
     */
    public boolean isStartTls() {
        return clientConfiguration.isStartTls();
    }

//    /**
//     * 设置此连接工厂使用的客户端名称
//     *
//     * @param clientName 客户端名称。可以是 {@literal null}
//     * @throws IllegalStateException 如果 {@link LettuceClientConfiguration} 是不可变的
//     * @deprecated configure the client name using {@link LettuceClientConfiguration}.
//     */
//    @Deprecated
//    public void setClientName(String clientName) {
//        this.getMutableConfiguration().setClientName(clientName);
//    }

    /**
     * 返回客户端名称
     *
     * @return 如果没有设置客户端名称或 {@literal null}
     */
    public String getClientName() {
        return clientConfiguration.getClientName().orElse(null);
    }

//    /**
//     * 设置与Redis服务器进行身份验证时使用的密码
//     *
//     * @param password 要设置的密码
//     * @deprecated since 2.0, configure the password using {@link RedisStandaloneConfiguration}, {@link RedisSentinelConfiguration} or {@link RedisClusterConfiguration}.
//     */
//    @Deprecated
//    public void setPassword(String password) {
//        if (RedisConfiguration.isAuthenticationAware(configuration)) {
//            ((WithPassword) configuration).setPassword(password);
//            return;
//        }
//        standaloneConfig.setPassword(RedisPassword.of(password));
//    }

    /**
     * 返回用于与Redis服务器进行身份验证的密码
     *
     * @return 身份验证密码，如果未设置，则为 {@literal null}
     */
    public String getPassword() {
        return getRedisPassword().map(String::new).orElse(null);
    }

    private RedisPassword getRedisPassword() {
        return RedisConfiguration.getPasswordOrElse(configuration, standaloneConfig::getPassword);
    }

    private String getRedisUsername() {
        return RedisConfiguration.getUsernameOrElse(configuration, standaloneConfig::getUsername);
    }

//    /**
//     * 设置RedisClient关闭超时时间(以毫秒为单位)
//     *
//     * @param shutdownTimeout 关机超时
//     * @throws IllegalStateException 如果 {@link LettuceClientConfiguration} 是不可变的
//     * @deprecated since 2.0, configure the shutdown timeout using {@link LettuceClientConfiguration}.
//     */
//    @Deprecated
//    public void setShutdownTimeout(long shutdownTimeout) {
//        getMutableConfiguration().setShutdownTimeout(Duration.ofMillis(shutdownTimeout));
//    }

    /**
     * 返回关闭RedisClient的关闭超时时间(以毫秒为单位)
     *
     * @return 关闭超时
     */
    public long getShutdownTimeout() {
        return clientConfiguration.getShutdownTimeout().toMillis();
    }

//    /**
//     * 设置 {@link ClientResources} 以重用客户端基础结构。 <br />
//     * 设置为 {@literal null} 表示不共享资源
//     *
//     * @param clientResources 可以是 {@literal null}
//     * @throws IllegalStateException 如果 {@link LettuceClientConfiguration} 是不可变的
//     * @deprecated since 2.0, configure {@link ClientResources} using {@link LettuceClientConfiguration}.
//     */
//    @Deprecated
//    public void setClientResources(ClientResources clientResources) {
//        getMutableConfiguration().setClientResources(clientResources);
//    }

    /**
     * 获取 {@link ClientResources} 来重用基础设施
     *
     * @return {@literal null} 如果没有设置
     */
    public ClientResources getClientResources() {
        return clientConfiguration.getClientResources().orElse(null);
    }

    // --------------------------------------------------------------------------------------------
    // 操作
    // --------------------------------------------------------------------------------------------

    /**
     * @return 当存在 {@link RedisStaticMasterReplicaConfiguration} 时为true。
     */
    private boolean isStaticMasterReplicaAware() {
        return RedisConfiguration.isStaticMasterReplicaConfiguration(configuration);
    }

    /**
     * @return 如果存在 {@link RedisSocketConfiguration}，则为true。
     */
    private boolean isDomainSocketAware() {
        return RedisConfiguration.isDomainSocketConfiguration(configuration);
    }

    /**
     * @return 当 {@link RedisSentinelConfiguration} 存在时为true。
     */
    public boolean isRedisSentinelAware() {
        return RedisConfiguration.isSentinelConfiguration(configuration);
    }

    /**
     * @return 当存在 {@link RedisClusterConfiguration} 时为true
     */
    public boolean isClusterAware() {
        return RedisConfiguration.isClusterConfiguration(configuration);
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        assertInitialized();
        return new LettuceSentinelConnection(connectionProvider);
    }

    public LettuceClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    public RedisStandaloneConfiguration getStandaloneConfiguration() {
        return standaloneConfig;
    }

    /**
     * @return {@link RedisSocketConfiguration}，如果没有设置可能是 {@literal null}。
     */
    public RedisSocketConfiguration getSocketConfiguration() {
        return isDomainSocketAware() ? (RedisSocketConfiguration) configuration : null;
    }

    /**
     * @return {@link RedisSentinelConfiguration}，可能是 {@literal null}。
     */
    public RedisSentinelConfiguration getSentinelConfiguration() {
        return isRedisSentinelAware() ? (RedisSentinelConfiguration) configuration : null;
    }

    /**
     * @return {@link RedisClusterConfiguration}，可以是 {@literal null}。
     */
    public RedisClusterConfiguration getClusterConfiguration() {
        return isClusterAware() ? (RedisClusterConfiguration) configuration : null;
    }

    /**
     * 返回此实例使用的本机 {@link AbstractRedisClient}。
     * 客户端被初始化为 {@link #afterPropertiesSet() bean初始化生命周期} 的一部分，并且仅在该连接工厂初始化时可用。
     * <p>
     * 根据配置，客户端可以是 {@link RedisClusterClient} 或 {@link RedisClusterClient}
     *
     * @return 本机 {@link AbstractRedisClient}。如果没有初始化，可以是 {@literal null}
     * @see #afterPropertiesSet()
     */
    public AbstractRedisClient getNativeClient() {
        assertInitialized();
        return this.client;
    }

    /**
     * 返回此实例使用的本机 {@link AbstractRedisClient}。
     * 客户端被初始化为 {@link #afterPropertiesSet() bean初始化生命周期} 的一部分，并且仅在该连接工厂初始化时可用。
     * 如果尚未初始化，则抛出 {@link IllegalStateException}。
     * <p>
     * 根据配置，客户端可以是 {@link RedisClusterClient} 或 {@link RedisClusterClient}。
     *
     * @return 本机 {@link AbstractRedisClient}
     * @throws IllegalStateException 如果尚未初始化
     * @see #getNativeClient()
     */
    public AbstractRedisClient getRequiredNativeClient() {
        AbstractRedisClient client = getNativeClient();
        Assert.state(client != null, "Client not yet initialized. Did you forget to call initialize the bean?");
        return client;
    }

    @Override
    public RedisConnection getConnection() {
        assertInitialized();
        if (isClusterAware()) {
            return getClusterConnection();
        }
        LettuceConnection connection;
        connection = doCreateLettuceConnection(getSharedConnection(), connectionProvider, getTimeout(), getDatabase());
        connection.setConvertPipelineAndTxResults(convertPipelineAndTxResults);
        return connection;
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        assertInitialized();
        if (!isClusterAware()) {
            throw new InvalidDataAccessApiUsageException("Cluster is not configured!");
        }
        RedisClusterClient clusterClient = (RedisClusterClient) client;
        StatefulRedisClusterConnection<byte[], byte[]> sharedConnection = getSharedClusterConnection();
        LettuceClusterTopologyProvider topologyProvider = new LettuceClusterTopologyProvider(clusterClient);
        return doCreateLettuceClusterConnection(
                sharedConnection,
                connectionProvider,
                topologyProvider,
                clusterCommandExecutor,
                clientConfiguration.getCommandTimeout()
        );
    }

    /**
     * 如果启用了 {@link #getShareNativeConnection() 本机连接共享}，则初始化共享连接并重置任何先前存在的连接
     */
    public void initConnection() {
        resetConnection();
        if (isClusterAware()) {
            getSharedClusterConnection();
        } else {
            getSharedConnection();
        }
    }

    /**
     * 重置底层共享连接，在下次访问时重新初始化
     */
    public void resetConnection() {
        assertInitialized();
        connection.resetConnection();
        synchronized (this.connectionMonitor) {
            this.connection = null;
        }
    }

    /**
     * 验证共享连接并在无效时重新初始化
     */
    public void validateConnection() {
        assertInitialized();
        getOrCreateSharedConnection().validateConnection();
    }

    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return EXCEPTION_TRANSLATION.translate(ex);
    }

    // --------------------------------------------------------------------------------------------
    // 私有方法
    // --------------------------------------------------------------------------------------------

    private void dispose(LettuceConnectionProvider connectionProvider) {
        if (connectionProvider instanceof DisposableBean) {
            try {
                ((DisposableBean) connectionProvider).destroy();
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn(connectionProvider + " did not shut down gracefully.", e);
                }
            }
        }
    }

    /**
     * {@link LettuceConnection} 创建的自定义挂钩
     *
     * @param sharedConnection   共享的 {@link StatefulRedisConnection} 如果 {@link #getShareNativeConnection()} 是 {@literal true}； {@literal null} 否则
     * @param connectionProvider {@link LettuceConnectionProvider} 释放连接
     * @param timeout            {@link TimeUnit#MILLISECONDS} 中的命令超时
     * @param database           要操作的数据库索引
     * @return {@link LettuceConnection}
     * @throws IllegalArgumentException 如果必需的参数是 {@literal null}
     */
    protected LettuceConnection doCreateLettuceConnection(StatefulRedisConnection<byte[], byte[]> sharedConnection,
                                                          LettuceConnectionProvider connectionProvider,
                                                          long timeout,
                                                          int database) {
        LettuceConnection connection = new LettuceConnection(sharedConnection, connectionProvider, timeout, database);
        connection.setPipeliningFlushPolicy(this.pipeliningFlushPolicy);
        return connection;
    }

    /**
     * {@link LettuceClusterConnection} 创建的自定义挂钩
     *
     * @param sharedConnection       共享的 {@link StatefulRedisConnection} 如果 {@link #getShareNativeConnection()} 是 {@literal true}； {@literal null} 否则
     * @param connectionProvider     {@link LettuceConnectionProvider} 释放连接
     * @param topologyProvider       {@link ClusterTopologyProvider}
     * @param clusterCommandExecutor {@link ClusterCommandExecutor} 释放连接
     * @param commandTimeout         命令超时 {@link Duration}
     * @return {@link LettuceConnection}
     * @throws IllegalArgumentException 如果必需的参数是 {@literal null}
     */
    protected LettuceClusterConnection doCreateLettuceClusterConnection(StatefulRedisClusterConnection<byte[], byte[]> sharedConnection,
                                                                        LettuceConnectionProvider connectionProvider,
                                                                        ClusterTopologyProvider topologyProvider,
                                                                        ClusterCommandExecutor clusterCommandExecutor,
                                                                        Duration commandTimeout) {
        LettuceClusterConnection connection = new LettuceClusterConnection(
                sharedConnection, connectionProvider, topologyProvider, clusterCommandExecutor, commandTimeout
        );
        connection.setPipeliningFlushPolicy(this.pipeliningFlushPolicy);
        return connection;
    }

    private SharedConnection<byte[]> getOrCreateSharedConnection() {
        synchronized (this.connectionMonitor) {
            if (this.connection == null) {
                this.connection = new SharedConnection<>(connectionProvider);
            }
            return this.connection;
        }
    }

    /**
     * @return 共享连接使用 {@literal byte[]} 编码以强制使用API。 {@literal null} 如果 {@link #getShareNativeConnection() 连接共享} 被禁用或连接到Redis群集。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected StatefulRedisConnection<byte[], byte[]> getSharedConnection() {
        return shareNativeConnection && !isClusterAware()
                ? (StatefulRedisConnection) getOrCreateSharedConnection().getConnection()
                : null;
    }

    /**
     * @return 共享集群连接使用 {@literal byte[]} 编码以强制使用API。 {@literal null} 如果 {@link #getShareNativeConnection() 连接共享} 被禁用或连接到Redis StandaloneSentinelMaster副本时。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected StatefulRedisClusterConnection<byte[], byte[]> getSharedClusterConnection() {
        return shareNativeConnection && isClusterAware() ?
                (StatefulRedisClusterConnection) getOrCreateSharedConnection().getConnection() :
                null;
    }

    @SuppressWarnings("SameParameterValue")
    private LettuceConnectionProvider createConnectionProvider(AbstractRedisClient client, RedisCodec<?, ?> codec) {
        LettuceConnectionProvider connectionProvider = doCreateConnectionProvider(client, codec);
        if (this.clientConfiguration instanceof LettucePoolingClientConfiguration) {
            return new LettucePoolingConnectionProvider(
                    connectionProvider,
                    (LettucePoolingClientConfiguration) this.clientConfiguration
            );
        }
        return connectionProvider;
    }

    /**
     * 在给定 {@link AbstractRedisClient} 和 {@link RedisCodec} 的情况下，创建一个 {@link LettuceConnectionProvider} 。
     * 此连接工厂的配置指定创建的连接提供程序的类型。
     * 此方法为 {@link RedisClient} 或 {@link RedisClusterClient} 创建 {@link LettuceConnectionProvider} 。
     * 子类可以重写此方法来修饰连接提供程序。
     *
     * @param client {@link RedisClient} 或 {@link RedisClusterClient} ，不得为 {@literal null}
     * @param codec  用于连接创建，不得为  {@literal null} 默认情况下，为 {@code byte[]} 编解码器。反应式连接需要{@link java.nio.ByteBuffer}编解码器。
     * @return 连接提供商
     */
    protected LettuceConnectionProvider doCreateConnectionProvider(AbstractRedisClient client, RedisCodec<?, ?> codec) {
        ReadFrom readFrom = getClientConfiguration().getReadFrom().orElse(null);
        if (isStaticMasterReplicaAware()) {
            List<RedisURI> nodes = ((RedisStaticMasterReplicaConfiguration) configuration).getNodes().stream()
                    .map(it -> createRedisURIAndApplySettings(it.getHostName(), it.getPort()))
                    .peek(it -> it.setDatabase(getDatabase()))
                    .collect(Collectors.toList());
            return new StaticMasterReplicaConnectionProvider((RedisClient) client, codec, nodes, readFrom);
        }
        if (isClusterAware()) {
            return new ClusterConnectionProvider((RedisClusterClient) client, codec, readFrom);
        }
        return new StandaloneConnectionProvider((RedisClient) client, codec, readFrom);
    }

    protected AbstractRedisClient createClient() {
        if (isStaticMasterReplicaAware()) {
            RedisClient redisClient = clientConfiguration.getClientResources()
                    .map(RedisClient::create)
                    .orElseGet(RedisClient::create);
            clientConfiguration.getClientOptions().ifPresent(redisClient::setOptions);
            return redisClient;
        }
        if (isRedisSentinelAware()) {
            RedisURI redisURI = getSentinelRedisURI();
            RedisClient redisClient = clientConfiguration.getClientResources()
                    .map(clientResources -> RedisClient.create(clientResources, redisURI))
                    .orElseGet(() -> RedisClient.create(redisURI));
            clientConfiguration.getClientOptions().ifPresent(redisClient::setOptions);
            return redisClient;
        }
        if (isClusterAware()) {
            List<RedisURI> initialUris = new ArrayList<>();
            ClusterConfiguration configuration = (ClusterConfiguration) this.configuration;
            for (RedisNode node : configuration.getClusterNodes()) {
                initialUris.add(createRedisURIAndApplySettings(node.getHost(), node.getPort()));
            }
            RedisClusterClient clusterClient = clientConfiguration.getClientResources()
                    .map(clientResources -> RedisClusterClient.create(clientResources, initialUris))
                    .orElseGet(() -> RedisClusterClient.create(initialUris));
            clusterClient.setOptions(getClusterClientOptions(configuration));
            return clusterClient;
        }
        RedisURI uri = isDomainSocketAware() ?
                createRedisSocketURIAndApplySettings(((DomainSocketConfiguration) configuration).getSocket()) :
                createRedisURIAndApplySettings(getHostName(), getPort());
        RedisClient redisClient = clientConfiguration.getClientResources()
                .map(clientResources -> RedisClient.create(clientResources, uri))
                .orElseGet(() -> RedisClient.create(uri));
        clientConfiguration.getClientOptions().ifPresent(redisClient::setOptions);
        return redisClient;
    }

    private ClusterClientOptions getClusterClientOptions(ClusterConfiguration configuration) {
        Optional<ClientOptions> clientOptions = clientConfiguration.getClientOptions();
        ClusterClientOptions clusterClientOptions = clientOptions
                .filter(ClusterClientOptions.class::isInstance)
                .map(ClusterClientOptions.class::cast)
                .orElseGet(() -> clientOptions.map(it -> ClusterClientOptions.builder(it).build()).orElseGet(ClusterClientOptions::create));
        if (configuration.getMaxRedirects() != null) {
            return clusterClientOptions.mutate().maxRedirects(configuration.getMaxRedirects()).build();
        }
        return clusterClientOptions;
    }

    private RedisURI getSentinelRedisURI() {
        RedisURI redisUri = LettuceConverters.sentinelConfigurationToRedisURI((RedisSentinelConfiguration) configuration);
        applyToAll(redisUri, it -> {
            clientConfiguration.getClientName().ifPresent(it::setClientName);
            it.setSsl(clientConfiguration.isUseSsl());
            it.setVerifyPeer(clientConfiguration.isVerifyPeer());
            it.setStartTls(clientConfiguration.isStartTls());
            it.setTimeout(clientConfiguration.getCommandTimeout());
        });
        redisUri.setDatabase(getDatabase());
        return redisUri;
    }

    private void assertInitialized() {
        Assert.state(this.initialized, "LettuceConnectionFactory was not initialized through afterPropertiesSet()");
        Assert.state(!this.destroyed, "LettuceConnectionFactory was destroyed and cannot be used anymore");
    }

    private static void applyToAll(RedisURI source, Consumer<RedisURI> action) {
        action.accept(source);
        source.getSentinels().forEach(action);
    }

    private RedisURI createRedisURIAndApplySettings(String host, int port) {
        RedisURI.Builder builder = RedisURI.Builder.redis(host, port);
        applyAuthentication(builder);
        clientConfiguration.getClientName().ifPresent(builder::withClientName);
        builder.withDatabase(getDatabase());
        builder.withSsl(clientConfiguration.isUseSsl());
        builder.withVerifyPeer(clientConfiguration.isVerifyPeer());
        builder.withStartTls(clientConfiguration.isStartTls());
        builder.withTimeout(clientConfiguration.getCommandTimeout());
        return builder.build();
    }

    private RedisURI createRedisSocketURIAndApplySettings(String socketPath) {
        RedisURI.Builder builder = RedisURI.Builder.socket(socketPath);
        applyAuthentication(builder);
        builder.withDatabase(getDatabase());
        builder.withTimeout(clientConfiguration.getCommandTimeout());
        return builder.build();
    }

    private void applyAuthentication(RedisURI.Builder builder) {
        String username = getRedisUsername();
        if (StringUtils.hasText(username)) {
            // See https://github.com/lettuce-io/lettuce-core/issues/1404
            builder.withAuthentication(username, new String(getRedisPassword().toOptional().orElse(new char[0])));
        } else {
            getRedisPassword().toOptional().ifPresent(builder::withPassword);
        }
    }

    private MutableLettuceClientConfiguration getMutableConfiguration() {
        Assert.state(
                clientConfiguration instanceof MutableLettuceClientConfiguration,
                () -> String.format(
                        "Client configuration must be instance of MutableLettuceClientConfiguration but is %s",
                        ClassUtils.getShortName(clientConfiguration.getClass())
                )
        );
        return (MutableLettuceClientConfiguration) clientConfiguration;
    }

    private long getClientTimeout() {
        return clientConfiguration.getCommandTimeout().toMillis();
    }

    /**
     * 共享连接的包装器。
     * 跟踪连接生命周期，包装器是线程安全的，因为它通过阻塞同步并发调用。
     *
     * @param <E> 连接编码
     */
    class SharedConnection<E> {
        private final LettuceConnectionProvider connectionProvider;
        /**
         * 共享连接的同步监视器
         */
        private final Object connectionMonitor = new Object();
        private StatefulConnection<E, E> connection;

        SharedConnection(LettuceConnectionProvider connectionProvider) {
            this.connectionProvider = connectionProvider;
        }

        /**
         * 返回一个有效的 Lettuce 连接。
         * 如果{@link #setValidateConnection(boolean) enabled} 初始化并验证连接。
         *
         * @return 连接
         */
        StatefulConnection<E, E> getConnection() {
            synchronized (this.connectionMonitor) {
                if (this.connection == null) {
                    this.connection = getNativeConnection();
                }
                if (getValidateConnection()) {
                    validateConnection();
                }
                return this.connection;
            }
        }

        /**
         * 从关联的 {@link LettuceConnectionProvider} 获取连接
         *
         * @return 连接
         */
        @SuppressWarnings("unchecked")
        private StatefulConnection<E, E> getNativeConnection() {
            return connectionProvider.getConnection(StatefulConnection.class);
        }

        /**
         * 验证连接。无效连接将被关闭，连接状态将被重置。
         */
        @SuppressWarnings("rawtypes")
        void validateConnection() {
            synchronized (this.connectionMonitor) {
                boolean valid = false;
                if (connection != null && connection.isOpen()) {
                    try {
                        if (connection instanceof StatefulRedisConnection) {
                            ((StatefulRedisConnection) connection).sync().ping();
                        }
                        if (connection instanceof StatefulRedisClusterConnection) {
                            ((StatefulRedisClusterConnection) connection).sync().ping();
                        }
                        valid = true;
                    } catch (Exception e) {
                        log.debug("Validation failed", e);
                    }
                }
                if (!valid) {
                    log.info("Validation of shared connection failed. Creating a new connection.");
                    resetConnection();
                    this.connection = getNativeConnection();
                }
            }
        }

        /**
         * 重置底层共享连接，在下次访问时重新初始化
         */
        void resetConnection() {
            synchronized (this.connectionMonitor) {
                if (this.connection != null) {
                    this.connectionProvider.release(this.connection);
                }
                this.connection = null;
            }
        }
    }

    /**
     * {@link LettuceClientConfiguration} 的可变实现
     */
    static class MutableLettuceClientConfiguration implements LettuceClientConfiguration {
        private boolean useSsl;
        private boolean verifyPeer = true;
        private boolean startTls;
        private ClientResources clientResources;
        private String clientName;
        private Duration timeout = Duration.ofSeconds(RedisURI.DEFAULT_TIMEOUT);
        private Duration shutdownTimeout = Duration.ofMillis(100);

        @Override
        public boolean isUseSsl() {
            return useSsl;
        }

        void setUseSsl(boolean useSsl) {
            this.useSsl = useSsl;
        }

        @Override
        public boolean isVerifyPeer() {
            return verifyPeer;
        }

        void setVerifyPeer(boolean verifyPeer) {
            this.verifyPeer = verifyPeer;
        }

        @Override
        public boolean isStartTls() {
            return startTls;
        }

        void setStartTls(boolean startTls) {
            this.startTls = startTls;
        }

        @Override
        public Optional<ClientResources> getClientResources() {
            return Optional.ofNullable(clientResources);
        }

        void setClientResources(ClientResources clientResources) {
            this.clientResources = clientResources;
        }

        @Override
        public Optional<ClientOptions> getClientOptions() {
            return Optional.empty();
        }

        @Override
        public Optional<ReadFrom> getReadFrom() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getClientName() {
            return Optional.ofNullable(clientName);
        }

        /**
         * @param clientName 可以是 {@literal null}
         */
        void setClientName(String clientName) {
            this.clientName = clientName;
        }

        @Override
        public Duration getCommandTimeout() {
            return timeout;
        }

        void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        @Override
        public Duration getShutdownTimeout() {
            return shutdownTimeout;
        }

        void setShutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
        }

        @Override
        public Duration getShutdownQuietPeriod() {
            return shutdownTimeout;
        }
    }

    /**
     * {@link LettuceConnectionProvider} 将连接异常转换为 {@link RedisConnectionException}
     */
    private static class ExceptionTranslatingConnectionProvider implements LettuceConnectionProvider, LettuceConnectionProvider.TargetAware {
        private final LettuceConnectionProvider delegate;

        public ExceptionTranslatingConnectionProvider(LettuceConnectionProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType) {
            try {
                return delegate.getConnection(connectionType);
            } catch (RuntimeException e) {
                throw translateException(e);
            }
        }

        @Override
        public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType, RedisURI redisURI) {
            try {
                return ((TargetAware) delegate).getConnection(connectionType, redisURI);
            } catch (RuntimeException e) {
                throw translateException(e);
            }
        }

        @Override
        public <T extends StatefulConnection<?, ?>> CompletionStage<T> getConnectionAsync(Class<T> connectionType) {
            CompletableFuture<T> future = new CompletableFuture<>();
            delegate.getConnectionAsync(connectionType).whenComplete((t, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(translateException(throwable));
                } else {
                    future.complete(t);
                }
            });
            return future;
        }

        @Override
        public <T extends StatefulConnection<?, ?>> CompletionStage<T> getConnectionAsync(Class<T> connectionType, RedisURI redisURI) {
            CompletableFuture<T> future = new CompletableFuture<>();
            ((TargetAware) delegate).getConnectionAsync(connectionType, redisURI).whenComplete((t, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(translateException(throwable));
                } else {
                    future.complete(t);
                }
            });
            return future;
        }

        @Override
        public void release(StatefulConnection<?, ?> connection) {
            delegate.release(connection);
        }

        @Override
        public CompletableFuture<Void> releaseAsync(StatefulConnection<?, ?> connection) {
            return delegate.releaseAsync(connection);
        }

        public void destroy() throws Exception {
            if (delegate instanceof DisposableBean) {
                ((DisposableBean) delegate).destroy();
            }
        }

        private RuntimeException translateException(Throwable e) {
            return e instanceof RedisConnectionFailureException ?
                    (RedisConnectionFailureException) e :
                    new RedisConnectionFailureException("Unable to connect to Redis", e);
        }
    }
}
