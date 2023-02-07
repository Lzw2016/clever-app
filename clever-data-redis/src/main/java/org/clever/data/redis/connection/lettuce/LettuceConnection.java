package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import org.clever.beans.BeanUtils;
import org.clever.core.convert.converter.Converter;
import org.clever.dao.DataAccessException;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.dao.QueryTimeoutException;
import org.clever.data.redis.ExceptionTranslationStrategy;
import org.clever.data.redis.FallbackExceptionTranslationStrategy;
import org.clever.data.redis.connection.*;
import org.clever.data.redis.connection.convert.TransactionResultConverter;
import org.clever.data.redis.connection.lettuce.LettuceConnectionProvider.TargetAware;
import org.clever.data.redis.connection.lettuce.LettuceResult.LettuceResultBuilder;
import org.clever.data.redis.connection.lettuce.LettuceResult.LettuceStatusResult;
import org.clever.data.redis.core.RedisCommand;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * {@code RedisConnection} 在 <a href="https://github.com/mp911de/lettuce">Lettuce</a> Redis 客户端之上实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:15 <br/>
 */
public class LettuceConnection extends AbstractRedisConnection {
    static final RedisCodec<byte[], byte[]> CODEC = ByteArrayCodec.INSTANCE;
    private static final ExceptionTranslationStrategy EXCEPTION_TRANSLATION = new FallbackExceptionTranslationStrategy(LettuceConverters.exceptionConverter());
    private static final TypeHints typeHints = new TypeHints();

    private final int defaultDbIndex;
    private final long timeout;
    private final LettuceConnectionProvider connectionProvider;
    private final StatefulConnection<byte[], byte[]> asyncSharedConn;

    private int dbIndex;
    private boolean isMulti = false;
    private boolean isPipelined = false;
    // 仅指主连接，因为 pub/sub 发生在不同的连接上
    private boolean isClosed = false;
    private StatefulConnection<byte[], byte[]> asyncDedicatedConn;
    private List<LettuceResult<?, ?>> ppline;
    private PipeliningFlushState flushState;
    private final Queue<FutureResult<?>> txResults = new LinkedList<>();
    private volatile LettuceSubscription subscription;
    // 指示是否需要断开连接的标志
    private boolean convertPipelineAndTxResults = true;
    private PipeliningFlushPolicy pipeliningFlushPolicy = PipeliningFlushPolicy.flushEachCommand();

    LettuceResult<?, ?> newLettuceResult(Future<?> resultHolder) {
        return newLettuceResult(resultHolder, (val) -> val);
    }

    <T, R> LettuceResult<T, R> newLettuceResult(Future<T> resultHolder, Converter<T, R> converter) {
        return LettuceResultBuilder.<T, R>forResponse(resultHolder)
                .mappedWith(converter)
                .convertPipelineAndTxResults(convertPipelineAndTxResults)
                .build();
    }

    <T, R> LettuceResult<T, R> newLettuceResult(Future<T> resultHolder, Converter<T, R> converter, Supplier<R> defaultValue) {
        return LettuceResultBuilder.<T, R>forResponse(resultHolder)
                .mappedWith(converter)
                .convertPipelineAndTxResults(convertPipelineAndTxResults)
                .defaultNullTo(defaultValue)
                .build();
    }

    <T, R> LettuceResult<T, R> newLettuceStatusResult(Future<T> resultHolder) {
        return LettuceResultBuilder.<T, R>forResponse(resultHolder).buildStatusResult();
    }

    private static class LettuceTransactionResultConverter<T> extends TransactionResultConverter<T> {
        public LettuceTransactionResultConverter(Queue<FutureResult<T>> txResults, Converter<Exception, DataAccessException> exceptionConverter) {
            super(txResults, exceptionConverter);
        }

        @Override
        public List<Object> convert(List<Object> execResults) {
            // Lettuce Empty list 表示 null（观察变量被修改）
            if (execResults.isEmpty()) {
                return null;
            }
            return super.convert(execResults);
        }
    }

//    /**
//     * 实例化一个新的 Lettuce 连接
//     *
//     * @param sharedConnection 与其他 {@link LettuceConnection} 共享的本机连接。不应用于事务或阻塞操作。
//     * @param timeout          连接超时（以毫秒为单位）
//     * @param client           建立 pubsub 连接时使用的 {@link RedisClient}。
//     * @param pool             用于阻塞和 tx 操作的连接池。
//     * @param defaultDbIndex   建立专用连接时与 {@link RedisClient} 一起使用的数据库索引。
//     * @deprecated since 2.0, use {@link #LettuceConnection(StatefulRedisConnection, LettuceConnectionProvider, long, int)}
//     */
//    @Deprecated
//    public LettuceConnection(StatefulRedisConnection<byte[], byte[]> sharedConnection,
//                             long timeout,
//                             AbstractRedisClient client,
//                             LettucePool pool,
//                             int defaultDbIndex) {
//        if (pool != null) {
//            this.connectionProvider = new LettucePoolConnectionProvider(pool);
//        } else {
//            this.connectionProvider = new StandaloneConnectionProvider((RedisClient) client, CODEC);
//        }
//        this.asyncSharedConn = sharedConnection;
//        this.timeout = timeout;
//        this.defaultDbIndex = defaultDbIndex;
//        this.dbIndex = this.defaultDbIndex;
//    }
//
//    /**
//     * 实例化一个新的 Lettuce 连接
//     *
//     * @param sharedConnection 与其他 {@link LettuceConnection} 共享的本机连接。不应用于事务或阻塞操作
//     * @param timeout          连接超时（以毫秒为单位）
//     * @param client           建立 pubsub 连接时使用的 {@link RedisClient}
//     * @param pool             用于阻塞和 tx 操作的连接池
//     * @deprecated since 2.0, use {@link #LettuceConnection(StatefulRedisConnection, LettuceConnectionProvider, long, int)}
//     */
//    @Deprecated
//    public LettuceConnection(StatefulRedisConnection<byte[], byte[]> sharedConnection, long timeout, RedisClient client, LettucePool pool) {
//        this(sharedConnection, timeout, client, pool, 0);
//    }
//
//    /**
//     * 实例化一个新的 Lettuce 连接
//     *
//     * @param sharedConnection 与其他 {@link LettuceConnection} 共享的本机连接。不会用于事务或阻塞操作
//     * @param timeout          连接超时（以毫秒为单位）
//     * @param client           在建立 pubsub、阻塞和 tx 连接时使用的 {@link RedisClient}
//     */
//    public LettuceConnection(StatefulRedisConnection<byte[], byte[]> sharedConnection, long timeout, RedisClient client) {
//        this(sharedConnection, timeout, client, null);
//    }
//
//    /**
//     * 实例化一个新的 Lettuce 连接
//     *
//     * @param timeout 连接超时（以毫秒为单位） @param client 实例化 pubsub 连接时使用的 {@link RedisClient}
//     * @param pool    用于所有其他本机连接的连接池
//     * @deprecated since 2.0, use pooling via {@link LettucePoolingClientConfiguration}.
//     */
//    @Deprecated
//    public LettuceConnection(long timeout, RedisClient client, LettucePool pool) {
//        this(null, timeout, client, pool);
//    }
//
//    /**
//     * 实例化一个新的 Lettuce 连接
//     *
//     * @param timeout 连接超时（以毫秒为单位）
//     * @param client  实例化本机连接时使用的 {@link RedisClient}
//     */
//    public LettuceConnection(long timeout, RedisClient client) {
//        this(null, timeout, client, null);
//    }

    /**
     * @param sharedConnection   与其他 {@link LettuceConnection} 共享的本机连接。不应用于事务或阻塞操作
     * @param connectionProvider 连接提供者获取和释放本机连接
     * @param timeout            连接超时（毫秒）
     * @param defaultDbIndex     建立专用连接时与 {@link RedisClient} 一起使用的数据库索引。
     */
    LettuceConnection(StatefulConnection<byte[], byte[]> sharedConnection,
                      LettuceConnectionProvider connectionProvider,
                      long timeout,
                      int defaultDbIndex) {
        Assert.notNull(connectionProvider, "LettuceConnectionProvider must not be null.");
        this.asyncSharedConn = sharedConnection;
        this.connectionProvider = connectionProvider;
        this.timeout = timeout;
        this.defaultDbIndex = defaultDbIndex;
        this.dbIndex = this.defaultDbIndex;
    }

    /**
     * @param sharedConnection   与其他 {@link LettuceConnection} 共享的本机连接。不应用于事务或阻塞操作。
     * @param connectionProvider 连接提供者获取和释放本机连接。
     * @param timeout            连接超时（以毫秒为单位）
     * @param defaultDbIndex     建立专用连接时与 {@link RedisClient} 一起使用的数据库索引。
     */
    public LettuceConnection(StatefulRedisConnection<byte[], byte[]> sharedConnection,
                             LettuceConnectionProvider connectionProvider,
                             long timeout,
                             int defaultDbIndex) {
        this((StatefulConnection<byte[], byte[]>) sharedConnection, connectionProvider, timeout, defaultDbIndex);
    }

    // --------------------------------------------------------------------------------------------
    // 配置
    // --------------------------------------------------------------------------------------------

    @Override
    public boolean isQueueing() {
        return isMulti;
    }

    @Override
    public boolean isPipelined() {
        return isPipelined;
    }

    @Override
    public Subscription getSubscription() {
        return subscription;
    }

    /**
     * 指定是否应将流水线和事务结果转换为预期的数据类型。
     * 如果为 false，{@link #closePipeline()} 和 {@link #exec()} 的结果将是 Lettuce 驱动程序返回的类型
     *
     * @param convertPipelineAndTxResults 是否转换 pipeline 和 tx 结果
     */
    public void setConvertPipelineAndTxResults(boolean convertPipelineAndTxResults) {
        this.convertPipelineAndTxResults = convertPipelineAndTxResults;
    }

    /**
     * 使用流水线时配置刷新策略
     *
     * @param pipeliningFlushPolicy 控制命令何时写入 Redis 连接的刷新策略
     * @see PipeliningFlushPolicy#flushEachCommand()
     * @see #openPipeline()
     * @see StatefulRedisConnection#flushCommands()
     */
    public void setPipeliningFlushPolicy(PipeliningFlushPolicy pipeliningFlushPolicy) {
        Assert.notNull(pipeliningFlushPolicy, "PipeliningFlushingPolicy must not be null!");
        this.pipeliningFlushPolicy = pipeliningFlushPolicy;
    }

    LettuceConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    // --------------------------------------------------------------------------------------------
    // 获取操作对象
    // --------------------------------------------------------------------------------------------

    @Override
    public RedisGeoCommands geoCommands() {
        return new LettuceGeoCommands(this);
    }

    @Override
    public RedisHashCommands hashCommands() {
        return new LettuceHashCommands(this);
    }

    @Override
    public RedisHyperLogLogCommands hyperLogLogCommands() {
        return new LettuceHyperLogLogCommands(this);
    }

    @Override
    public RedisKeyCommands keyCommands() {
        return new LettuceKeyCommands(this);
    }

    @Override
    public RedisListCommands listCommands() {
        return new LettuceListCommands(this);
    }

    @Override
    public RedisSetCommands setCommands() {
        return new LettuceSetCommands(this);
    }

    @Override
    public RedisScriptingCommands scriptingCommands() {
        return new LettuceScriptingCommands(this);
    }

    @Override
    public RedisStreamCommands streamCommands() {
        return new LettuceStreamCommands(this);
    }

    @Override
    public RedisStringCommands stringCommands() {
        return new LettuceStringCommands(this);
    }

    @Override
    public RedisServerCommands serverCommands() {
        return new LettuceServerCommands(this);
    }

    @Override
    public RedisZSetCommands zSetCommands() {
        return new LettuceZSetCommands(this);
    }

    // --------------------------------------------------------------------------------------------
    // 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 给定命令的“本机”或“原始”执行以及给定参数。
     *
     * @param command               要执行的命令
     * @param commandOutputTypeHint 要使用的输出类型，可能是（可能是 {@literal null} ）
     * @param args                  可能的命令参数（可能是 {@literal null}）
     * @return execution result.
     * @see RedisConnection#execute(String, byte[]...)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object execute(String command, CommandOutput commandOutputTypeHint, byte[]... args) {
        Assert.hasText(command, "a valid command needs to be specified");
        String name = command.trim().toUpperCase();
        ProtocolKeyword commandType = getCommandType(name);
        validateCommandIfRunningInTransactionMode(commandType, args);
        CommandArgs<byte[], byte[]> cmdArg = new CommandArgs<>(CODEC);
        if (!ObjectUtils.isEmpty(args)) {
            cmdArg.addKeys(args);
        }
        CommandOutput expectedOutput = commandOutputTypeHint != null ? commandOutputTypeHint : typeHints.getTypeHint(commandType);
        Command cmd = new Command(commandType, expectedOutput, cmdArg);
        return invoke().just(RedisClusterAsyncCommands::dispatch, cmd.getType(), cmd.getOutput(), cmd.getArgs());
    }

    @Override
    public Object execute(String command, byte[]... args) {
        return execute(command, null, args);
    }

    @Override
    public void close() throws DataAccessException {
        super.close();
        if (isClosed) {
            return;
        }
        isClosed = true;
        if (asyncDedicatedConn != null) {
            try {
                if (customizedDatabaseIndex()) {
                    potentiallySelectDatabase(defaultDbIndex);
                }
                connectionProvider.release(asyncDedicatedConn);
            } catch (RuntimeException ex) {
                throw convertLettuceAccessException(ex);
            }
        }
        if (subscription != null) {
            if (subscription.isAlive()) {
                subscription.doClose();
            }
            subscription = null;
        }
        this.dbIndex = defaultDbIndex;
    }

    @Override
    public boolean isClosed() {
        return isClosed && !isSubscribed();
    }

    @Override
    public RedisClusterAsyncCommands<byte[], byte[]> getNativeConnection() {
        LettuceSubscription subscription = this.subscription;
        return (subscription != null ? subscription.getNativeConnection().async() : getAsyncConnection());
    }

    @Override
    public void openPipeline() {
        if (!isPipelined) {
            isPipelined = true;
            ppline = new ArrayList<>();
            flushState = this.pipeliningFlushPolicy.newPipeline();
            flushState.onOpen(this.getOrCreateDedicatedConnection());
        }
    }

    @Override
    public List<Object> closePipeline() {
        if (!isPipelined) {
            return Collections.emptyList();
        }
        flushState.onClose(this.getOrCreateDedicatedConnection());
        flushState = null;
        isPipelined = false;
        List<io.lettuce.core.protocol.RedisCommand<?, ?, ?>> futures = new ArrayList<>(ppline.size());
        for (LettuceResult<?, ?> result : ppline) {
            futures.add(result.getResultHolder());
        }
        try {
            // noinspection SuspiciousToArrayCall
            boolean done = LettuceFutures.awaitAll(timeout, TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[0]));
            List<Object> results = new ArrayList<>(futures.size());
            Exception problem = null;
            if (done) {
                for (LettuceResult<?, ?> result : ppline) {
                    if (result.getResultHolder().getOutput().hasError()) {
                        Exception err = new InvalidDataAccessApiUsageException(result.getResultHolder().getOutput().getError());
                        // remember only the first error
                        if (problem == null) {
                            problem = err;
                        }
                        results.add(err);
                    } else if (!result.isStatus()) {
                        try {
                            results.add(result.conversionRequired() ? result.convert(result.get()) : result.get());
                        } catch (DataAccessException e) {
                            if (problem == null) {
                                problem = e;
                            }
                            results.add(e);
                        }
                    }
                }
            }
            ppline.clear();
            if (problem != null) {
                throw new RedisPipelineException(problem, results);
            }
            if (done) {
                return results;
            }
            throw new RedisPipelineException(new QueryTimeoutException("Redis command timed out"));
        } catch (Exception e) {
            throw new RedisPipelineException(e);
        }
    }

    @Override
    public byte[] echo(byte[] message) {
        return invoke().just(RedisClusterAsyncCommands::echo, message);
    }

    @Override
    public String ping() {
        return invoke().just(RedisClusterAsyncCommands::ping);
    }

    @Override
    public void select(int dbIndex) {
        if (asyncSharedConn != null) {
            throw new UnsupportedOperationException("Selecting a new database not supported due to shared connection. " + "Use separate ConnectionFactorys to work with multiple databases");
        }
        this.dbIndex = dbIndex;
        invokeStatus().just(
                RedisClusterAsyncCommands::dispatch,
                CommandType.SELECT,
                new StatusOutput<>(ByteArrayCodec.INSTANCE),
                new CommandArgs<>(ByteArrayCodec.INSTANCE).add(dbIndex)
        );
    }

    @Override
    public void multi() {
        if (isQueueing()) {
            return;
        }
        isMulti = true;
        try {
            if (isPipelined()) {
                getAsyncDedicatedRedisCommands().multi();
                return;
            }
            getDedicatedRedisCommands().multi();
        } catch (Exception ex) {
            throw convertLettuceAccessException(ex);
        }
    }

    @Override
    public void discard() {
        isMulti = false;
        try {
            if (isPipelined()) {
                pipeline(newLettuceStatusResult(getAsyncDedicatedRedisCommands().discard()));
                return;
            }
            getDedicatedRedisCommands().discard();
        } catch (Exception ex) {
            throw convertLettuceAccessException(ex);
        } finally {
            txResults.clear();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Object> exec() {
        isMulti = false;
        try {
            Converter<Exception, DataAccessException> exceptionConverter = this::convertLettuceAccessException;
            if (isPipelined()) {
                RedisFuture<TransactionResult> exec = getAsyncDedicatedRedisCommands().exec();
                LettuceTransactionResultConverter resultConverter = new LettuceTransactionResultConverter(
                        new LinkedList<>(txResults), exceptionConverter
                );
                pipeline(newLettuceResult(exec, source -> resultConverter.convert(
                        LettuceConverters.transactionResultUnwrapper().convert(source)
                )));
                return null;
            }
            TransactionResult transactionResult = getDedicatedRedisCommands().exec();
            List<Object> results = LettuceConverters.transactionResultUnwrapper().convert(transactionResult);
            return convertPipelineAndTxResults ?
                    new LettuceTransactionResultConverter(txResults, exceptionConverter).convert(results) :
                    results;
        } catch (Exception ex) {
            throw convertLettuceAccessException(ex);
        } finally {
            txResults.clear();
        }
    }

    @Override
    public void watch(byte[]... keys) {
        if (isQueueing()) {
            throw new UnsupportedOperationException();
        }
        try {
            if (isPipelined()) {
                pipeline(newLettuceStatusResult(getAsyncDedicatedRedisCommands().watch(keys)));
                return;
            }
            if (isQueueing()) {
                transaction(new LettuceStatusResult<>(getAsyncDedicatedRedisCommands().watch(keys)));
                return;
            }
            getDedicatedRedisCommands().watch(keys);
        } catch (Exception ex) {
            throw convertLettuceAccessException(ex);
        }
    }

    @Override
    public void unwatch() {
        try {
            if (isPipelined()) {
                pipeline(newLettuceStatusResult(getAsyncDedicatedRedisCommands().unwatch()));
                return;
            }
            if (isQueueing()) {
                transaction(newLettuceStatusResult(getAsyncDedicatedRedisCommands().unwatch()));
                return;
            }
            getDedicatedRedisCommands().unwatch();
        } catch (Exception ex) {
            throw convertLettuceAccessException(ex);
        }
    }

    // --------------------------------------------------------------------------------------------
    // Pub/Sub(发布/订阅) 功能
    // --------------------------------------------------------------------------------------------

    @Override
    public boolean isSubscribed() {
        return (subscription != null && subscription.isAlive());
    }

    @Override
    public Long publish(byte[] channel, byte[] message) {
        return invoke().just(RedisClusterAsyncCommands::publish, channel, message);
    }

    @Override
    public void pSubscribe(MessageListener listener, byte[]... patterns) {
        checkSubscription();
        if (isQueueing() || isPipelined()) {
            throw new UnsupportedOperationException("Transaction/Pipelining is not supported for Pub/Sub subscriptions!");
        }
        try {
            subscription = initSubscription(listener);
            subscription.pSubscribe(patterns);
        } catch (Exception ex) {
            throw convertLettuceAccessException(ex);
        }
    }

    @Override
    public void subscribe(MessageListener listener, byte[]... channels) {
        checkSubscription();
        if (isQueueing() || isPipelined()) {
            throw new UnsupportedOperationException("Transaction/Pipelining is not supported for Pub/Sub subscriptions!");
        }
        try {
            subscription = initSubscription(listener);
            subscription.subscribe(channels);
        } catch (Exception ex) {
            throw convertLettuceAccessException(ex);
        }
    }

    // --------------------------------------------------------------------------------------------
    // 私有方法
    // --------------------------------------------------------------------------------------------

    protected DataAccessException convertLettuceAccessException(Exception ex) {
        return EXCEPTION_TRANSLATION.translate(ex);
    }

    @SuppressWarnings("unchecked")
    <T> T failsafeReadScanValues(List<?> source, @SuppressWarnings("rawtypes") Converter converter) {
        try {
            return (T) (converter != null ? converter.convert(source) : source);
        } catch (IndexOutOfBoundsException e) {
            // ignore this one
        }
        return null;
    }

    /**
     * {@link #close()} 当前连接并打开到 Redis 服务器的新 pubsub 连接
     *
     * @return 从不 {@literal null}
     */
    @SuppressWarnings("unchecked")
    protected StatefulRedisPubSubConnection<byte[], byte[]> switchToPubSub() {
        close();
        return connectionProvider.getConnection(StatefulRedisPubSubConnection.class);
    }

    /**
     * 用于创建 {@link LettuceSubscription} 的自定义挂钩
     *
     * @param listener           {@link MessageListener} 通知
     * @param connection         pubsub 连接
     * @param connectionProvider the {@link LettuceConnectionProvider} for connection release.
     * @return a {@link LettuceSubscription}.
     */
    protected LettuceSubscription doCreateSubscription(MessageListener listener,
                                                       StatefulRedisPubSubConnection<byte[], byte[]> connection,
                                                       LettuceConnectionProvider connectionProvider) {
        return new LettuceSubscription(listener, connection, connectionProvider);
    }

    void pipeline(LettuceResult<?, ?> result) {
        if (flushState != null) {
            flushState.onCommand(getOrCreateDedicatedConnection());
        }
        if (isQueueing()) {
            transaction(result);
        } else {
            ppline.add(result);
        }
    }

    /**
     * 获取 {@link LettuceInvoker} 以使用默认的 {@link #getAsyncConnection() getAsyncConnection} 调用 Lettuce 方法。
     *
     * @return the {@link LettuceInvoker}.
     */
    LettuceInvoker invoke() {
        return invoke(getAsyncConnection());
    }

    /**
     * 获取 {@link LettuceInvoker} 以使用给定的 {@link RedisClusterAsyncCommands RedisClusterAsyncCommands} 调用 Lettuce 方法。
     *
     * @param connection the connection to use.
     * @return the {@link LettuceInvoker}.
     */
    LettuceInvoker invoke(RedisClusterAsyncCommands<byte[], byte[]> connection) {
        return doInvoke(connection, false);
    }

    /**
     * 获取 {@link LettuceInvoker} 来调用 Lettuce 方法，使用默认的 {@link #getAsyncConnection() getAsyncConnection} 返回状态响应。
     * 状态响应不包含在事务和管道结果中。
     *
     * @return the {@link LettuceInvoker}.
     */
    LettuceInvoker invokeStatus() {
        return doInvoke(getAsyncConnection(), true);
    }

    private LettuceInvoker doInvoke(RedisClusterAsyncCommands<byte[], byte[]> connection, boolean statusCommand) {
        if (isPipelined()) {
            return new LettuceInvoker(connection, (future, converter, nullDefault) -> {
                try {
                    if (statusCommand) {
                        pipeline(newLettuceStatusResult(future.get()));
                    } else {
                        pipeline(newLettuceResult(future.get(), converter, nullDefault));
                    }
                } catch (Exception ex) {
                    throw convertLettuceAccessException(ex);
                }
                return null;
            });
        }
        if (isQueueing()) {
            return new LettuceInvoker(connection, (future, converter, nullDefault) -> {
                try {
                    if (statusCommand) {
                        transaction(newLettuceStatusResult(future.get()));
                    } else {
                        transaction(newLettuceResult(future.get(), converter, nullDefault));
                    }
                } catch (Exception ex) {
                    throw convertLettuceAccessException(ex);
                }
                return null;
            });
        }
        return new LettuceInvoker(connection, (future, converter, nullDefault) -> {
            try {
                Object result = await(future.get());
                if (result == null) {
                    return nullDefault.get();
                }
                return converter.convert(result);
            } catch (Exception ex) {
                throw convertLettuceAccessException(ex);
            }
        });
    }

    void transaction(FutureResult<?> result) {
        txResults.add(result);
    }

    RedisClusterAsyncCommands<byte[], byte[]> getAsyncConnection() {
        if (isQueueing() || isPipelined()) {
            return getAsyncDedicatedConnection();
        }
        if (asyncSharedConn != null) {
            if (asyncSharedConn instanceof StatefulRedisConnection) {
                return ((StatefulRedisConnection<byte[], byte[]>) asyncSharedConn).async();
            }
            if (asyncSharedConn instanceof StatefulRedisClusterConnection) {
                return ((StatefulRedisClusterConnection<byte[], byte[]>) asyncSharedConn).async();
            }
        }
        return getAsyncDedicatedConnection();
    }

    protected RedisClusterCommands<byte[], byte[]> getConnection() {
        if (isQueueing()) {
            return getDedicatedConnection();
        }
        if (asyncSharedConn != null) {
            if (asyncSharedConn instanceof StatefulRedisConnection) {
                return ((StatefulRedisConnection<byte[], byte[]>) asyncSharedConn).sync();
            }
            if (asyncSharedConn instanceof StatefulRedisClusterConnection) {
                return ((StatefulRedisClusterConnection<byte[], byte[]>) asyncSharedConn).sync();
            }
        }
        return getDedicatedConnection();
    }

    RedisClusterCommands<byte[], byte[]> getDedicatedConnection() {
        StatefulConnection<byte[], byte[]> connection = getOrCreateDedicatedConnection();
        if (connection instanceof StatefulRedisConnection) {
            return ((StatefulRedisConnection<byte[], byte[]>) connection).sync();
        }
        if (connection instanceof StatefulRedisClusterConnection) {
            return ((StatefulRedisClusterConnection<byte[], byte[]>) connection).sync();
        }
        throw new IllegalStateException(String.format("%s is not a supported connection type.", connection.getClass().getName()));
    }

    protected RedisClusterAsyncCommands<byte[], byte[]> getAsyncDedicatedConnection() {
        StatefulConnection<byte[], byte[]> connection = getOrCreateDedicatedConnection();
        if (connection instanceof StatefulRedisConnection) {
            return ((StatefulRedisConnection<byte[], byte[]>) connection).async();
        }
        if (asyncDedicatedConn instanceof StatefulRedisClusterConnection) {
            return ((StatefulRedisClusterConnection<byte[], byte[]>) connection).async();
        }
        throw new IllegalStateException(String.format("%s is not a supported connection type.", connection.getClass().getName()));
    }

    @SuppressWarnings("unchecked")
    protected StatefulConnection<byte[], byte[]> doGetAsyncDedicatedConnection() {
        StatefulConnection<byte[], byte[]> connection = connectionProvider.getConnection(StatefulConnection.class);
        if (customizedDatabaseIndex()) {
            potentiallySelectDatabase(dbIndex);
        }
        return connection;
    }

    @Override
    protected boolean isActive(RedisNode node) {
        StatefulRedisSentinelConnection<String, String> connection = null;
        try {
            connection = getConnection(node);
            return connection.sync().ping().equalsIgnoreCase("pong");
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connectionProvider.release(connection);
            }
        }
    }

    @Override
    protected RedisSentinelConnection getSentinelConnection(RedisNode sentinel) {
        StatefulRedisSentinelConnection<String, String> connection = getConnection(sentinel);
        return new LettuceSentinelConnection(connection);
    }

    @SuppressWarnings("unchecked")
    private StatefulRedisSentinelConnection<String, String> getConnection(RedisNode sentinel) {
        return ((TargetAware) connectionProvider).getConnection(StatefulRedisSentinelConnection.class, getRedisURI(sentinel));
    }

    private <T> T await(RedisFuture<T> cmd) {
        if (isMulti) {
            return null;
        }
        try {
            return LettuceFutures.awaitOrCancel(cmd, timeout, TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            throw convertLettuceAccessException(e);
        }
    }

    private StatefulConnection<byte[], byte[]> getOrCreateDedicatedConnection() {
        if (asyncDedicatedConn == null) {
            asyncDedicatedConn = doGetAsyncDedicatedConnection();
        }
        return asyncDedicatedConn;
    }

    private RedisCommands<byte[], byte[]> getDedicatedRedisCommands() {
        return (RedisCommands<byte[], byte[]>) getDedicatedConnection();
    }

    private RedisAsyncCommands<byte[], byte[]> getAsyncDedicatedRedisCommands() {
        return (RedisAsyncCommands<byte[], byte[]>) getAsyncDedicatedConnection();
    }

    private void checkSubscription() {
        if (isSubscribed()) {
            throw new RedisSubscribedConnectionException("Connection already subscribed; use the connection Subscription to cancel or add new channels");
        }
    }

    private LettuceSubscription initSubscription(MessageListener listener) {
        return doCreateSubscription(listener, switchToPubSub(), connectionProvider);
    }

    private RedisURI getRedisURI(RedisNode node) {
        return RedisURI.Builder.redis(node.getHost(), node.getPort()).build();
    }

    private boolean customizedDatabaseIndex() {
        return defaultDbIndex != dbIndex;
    }

    private void potentiallySelectDatabase(int dbIndex) {
        if (asyncDedicatedConn instanceof StatefulRedisConnection) {
            ((StatefulRedisConnection<byte[], byte[]>) asyncDedicatedConn).sync().select(dbIndex);
        }
    }

    io.lettuce.core.ScanCursor getScanCursor(long cursorId) {
        return io.lettuce.core.ScanCursor.of(Long.toString(cursorId));
    }

    private void validateCommandIfRunningInTransactionMode(ProtocolKeyword cmd, byte[]... args) {
        if (this.isQueueing()) {
            validateCommand(cmd, args);
        }
    }

    private void validateCommand(ProtocolKeyword cmd, byte[]... args) {
        RedisCommand redisCommand = RedisCommand.failsafeCommandLookup(cmd.name());
        if (!RedisCommand.UNKNOWN.equals(redisCommand) && redisCommand.requiresArguments()) {
            try {
                redisCommand.validateArgumentCount(args != null ? args.length : 0);
            } catch (IllegalArgumentException e) {
                throw new InvalidDataAccessApiUsageException(String.format("Validation failed for %s command.", cmd), e);
            }
        }
    }

    private static ProtocolKeyword getCommandType(String name) {
        try {
            return CommandType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return new CustomCommandType(name);
        }
    }

    // --------------------------------------------------------------------------------------------
    // 内部类
    // --------------------------------------------------------------------------------------------

    /**
     * {@link TypeHints} 为给定的 {@link CommandType} 提供 {@link CommandOutput} 信息
     */
    @SuppressWarnings("OverwrittenKey")
    static class TypeHints {
        @SuppressWarnings("rawtypes")
        private static final Map<ProtocolKeyword, Class<? extends CommandOutput>> COMMAND_OUTPUT_TYPE_MAPPING = new HashMap<>();
        @SuppressWarnings("rawtypes")
        private static final Map<Class<?>, Constructor<CommandOutput>> CONSTRUCTORS = new ConcurrentHashMap<>();

        static {
            // INTEGER
            COMMAND_OUTPUT_TYPE_MAPPING.put(BITCOUNT, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(BITOP, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(BITPOS, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(DBSIZE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(DECR, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(DECRBY, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(DEL, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(COPY, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(GETBIT, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HDEL, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HINCRBY, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HLEN, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(INCR, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(INCRBY, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LINSERT, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LLEN, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LPUSH, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LPOS, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LPUSHX, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LREM, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PTTL, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PUBLISH, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RPUSH, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RPUSHX, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SADD, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SCARD, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SDIFFSTORE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SETBIT, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SETRANGE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SINTERSTORE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SREM, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SUNIONSTORE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(STRLEN, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(TTL, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZADD, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZCOUNT, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZINTERSTORE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZRANK, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZREM, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZREMRANGEBYRANK, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZREMRANGEBYSCORE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZREVRANK, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZUNIONSTORE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PFCOUNT, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PFMERGE, IntegerOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PFADD, IntegerOutput.class);
            // DOUBLE
            COMMAND_OUTPUT_TYPE_MAPPING.put(HINCRBYFLOAT, DoubleOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(INCRBYFLOAT, DoubleOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(MGET, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZINCRBY, DoubleOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZSCORE, DoubleOutput.class);
            // DOUBLE LIST
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZMSCORE, DoubleListOutput.class);
            // MAP
            COMMAND_OUTPUT_TYPE_MAPPING.put(HGETALL, MapOutput.class);
            // KEY LIST
            COMMAND_OUTPUT_TYPE_MAPPING.put(HKEYS, KeyListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(KEYS, KeyListOutput.class);
            // KEY VALUE
            COMMAND_OUTPUT_TYPE_MAPPING.put(BRPOP, KeyValueOutput.class);
            // SINGLE VALUE
            COMMAND_OUTPUT_TYPE_MAPPING.put(BRPOPLPUSH, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ECHO, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(GET, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(GETRANGE, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(GETSET, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HGET, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LINDEX, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LPOP, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RANDOMKEY, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RENAME, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RPOP, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RPOPLPUSH, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SPOP, ValueOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SRANDMEMBER, ValueOutput.class);
            // STATUS VALUE
            COMMAND_OUTPUT_TYPE_MAPPING.put(BGREWRITEAOF, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(BGSAVE, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(CLIENT, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(DEBUG, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(DISCARD, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(FLUSHALL, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(FLUSHDB, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HMSET, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(INFO, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LSET, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LTRIM, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(MIGRATE, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(MSET, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(QUIT, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RESTORE, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SAVE, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SELECT, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SET, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SETEX, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SHUTDOWN, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SLAVEOF, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SYNC, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(TYPE, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(WATCH, StatusOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(UNWATCH, StatusOutput.class);
            // VALUE LIST
            COMMAND_OUTPUT_TYPE_MAPPING.put(HMGET, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(MGET, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HVALS, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(LRANGE, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SORT, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZRANGE, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZRANGEBYSCORE, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZREVRANGE, ValueListOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(ZREVRANGEBYSCORE, ValueListOutput.class);
            // BOOLEAN
            COMMAND_OUTPUT_TYPE_MAPPING.put(EXISTS, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(EXPIRE, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(EXPIREAT, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HEXISTS, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HSET, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(HSETNX, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(MOVE, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(COPY, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(MSETNX, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PERSIST, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PEXPIRE, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(PEXPIREAT, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(RENAMENX, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SETNX, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SISMEMBER, BooleanOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SMOVE, BooleanOutput.class);
            // MULTI
            COMMAND_OUTPUT_TYPE_MAPPING.put(EXEC, MultiOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(MULTI, MultiOutput.class);
            // DATE
            COMMAND_OUTPUT_TYPE_MAPPING.put(LASTSAVE, DateOutput.class);
            // VALUE SET
            COMMAND_OUTPUT_TYPE_MAPPING.put(SDIFF, ValueSetOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SINTER, ValueSetOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SMEMBERS, ValueSetOutput.class);
            COMMAND_OUTPUT_TYPE_MAPPING.put(SUNION, ValueSetOutput.class);
        }

        /**
         * 返回默认为给定 {@link CommandType} 或 {@link ByteArrayOutput} 映射的 {@link CommandOutput}
         *
         * @return {@link ByteArrayOutput} 在没有匹配的 {@link CommandOutput} 可用时作为默认值
         */
        @SuppressWarnings("rawtypes")
        public CommandOutput getTypeHint(ProtocolKeyword type) {
            return getTypeHint(type, new ByteArrayOutput<>(CODEC));
        }

        /**
         * 返回给定 {@link CommandOutput} 的给定 {@link CommandType} 映射的 {@link CommandOutput} 作为默认值
         */
        @SuppressWarnings("rawtypes")
        public CommandOutput getTypeHint(ProtocolKeyword type, CommandOutput defaultType) {
            if (type == null || !COMMAND_OUTPUT_TYPE_MAPPING.containsKey(type)) {
                return defaultType;
            }
            CommandOutput<?, ?, ?> outputType = instanciateCommandOutput(COMMAND_OUTPUT_TYPE_MAPPING.get(type));
            return outputType != null ? outputType : defaultType;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private CommandOutput<?, ?, ?> instanciateCommandOutput(Class<? extends CommandOutput> type) {
            Assert.notNull(type, "Cannot create instance for 'null' type.");
            Constructor<CommandOutput> constructor = CONSTRUCTORS.get(type);
            if (constructor == null) {
                constructor = (Constructor<CommandOutput>) ClassUtils.getConstructorIfAvailable(type, RedisCodec.class);
                CONSTRUCTORS.put(type, constructor);
            }
            return BeanUtils.instantiateClass(constructor, CODEC);
        }
    }

//    static class LettucePoolConnectionProvider implements LettuceConnectionProvider {
//        private final LettucePool pool;
//
//        LettucePoolConnectionProvider(LettucePool pool) {
//            this.pool = pool;
//        }
//
//        @Override
//        public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType) {
//            return connectionType.cast(pool.getResource());
//        }
//
//        @Override
//        public <T extends StatefulConnection<?, ?>> CompletionStage<T> getConnectionAsync(Class<T> connectionType) {
//            throw new UnsupportedOperationException("Async operations not supported!");
//        }
//
//        @Override
//        @SuppressWarnings("unchecked")
//        public void release(StatefulConnection<?, ?> connection) {
//            if (connection.isOpen()) {
//                if (connection instanceof StatefulRedisConnection) {
//                    StatefulRedisConnection<?, ?> redisConnection = (StatefulRedisConnection<?, ?>) connection;
//                    if (redisConnection.isMulti()) {
//                        redisConnection.async().discard();
//                    }
//                }
//                pool.returnResource((StatefulConnection<byte[], byte[]>) connection);
//            } else {
//                pool.returnBrokenResource((StatefulConnection<byte[], byte[]>) connection);
//            }
//        }
//    }

    /**
     * 控制流水线刷新行为的策略接口。
     * Lettuce 将每个命令单独写入（刷新）到 Redis 连接。
     * 可以自定义刷新行为以优化性能。
     * 刷新可以是无状态的或有状态的。
     * 有状态刷新的一个示例是基于大小（缓冲区）的刷新，以在配置的命令数量后刷新。
     *
     * @see StatefulRedisConnection#setAutoFlushCommands(boolean)
     * @see StatefulRedisConnection#flushCommands()
     */
    public interface PipeliningFlushPolicy {
        /**
         * 在每个命令后返回一个刷新策略（默认行为）
         *
         * @return 在每个命令后刷新的策略
         */
        static PipeliningFlushPolicy flushEachCommand() {
            return FlushEachCommand.INSTANCE;
        }

        /**
         * 只有在调用 {@link #closePipeline()} 时才返回刷新策略
         *
         * @return 在每个命令后刷新的策略
         */
        static PipeliningFlushPolicy flushOnClose() {
            return FlushOnClose.INSTANCE;
        }

        /**
         * 返回缓冲命令的策略，并在达到配置的 {@code bufferSize} 后刷新。
         * 缓冲区是重复出现的，因此缓冲区大小为例如 {@code 2} 将在 2、4、6、... 命令后刷新。
         *
         * @param bufferSize 刷新前要缓冲的命令数。必须大于零。
         * @return 发出配置数量的命令后，将缓冲命令刷新到 Redis 连接的策略。
         */
        static PipeliningFlushPolicy buffered(int bufferSize) {
            Assert.isTrue(bufferSize > 0, "Buffer size must be greater than 0");
            return () -> new BufferedFlushing(bufferSize);
        }

        PipeliningFlushState newPipeline();
    }

    /**
     * 与当前正在进行的管道的刷新关联的状态对象
     */
    public interface PipeliningFlushState {
        /**
         * 如果管道被打开回调
         *
         * @see #openPipeline()
         */
        void onOpen(StatefulConnection<?, ?> connection);

        /**
         * 回调每个发出的 Redis 命令
         *
         * @see #pipeline(LettuceResult)
         */
        void onCommand(StatefulConnection<?, ?> connection);

        /**
         * 管道关闭时回调
         *
         * @see #closePipeline()
         */
        void onClose(StatefulConnection<?, ?> connection);
    }

    /**
     * 在每个命令上刷新的实现
     */
    private enum FlushEachCommand implements PipeliningFlushPolicy, PipeliningFlushState {
        INSTANCE;

        @Override
        public PipeliningFlushState newPipeline() {
            return INSTANCE;
        }

        @Override
        public void onOpen(StatefulConnection<?, ?> connection) {
        }

        @Override
        public void onCommand(StatefulConnection<?, ?> connection) {
        }

        @Override
        public void onClose(StatefulConnection<?, ?> connection) {
        }
    }

    /**
     * 关闭管道时刷新的实现
     */
    private enum FlushOnClose implements PipeliningFlushPolicy, PipeliningFlushState {
        INSTANCE;

        @Override
        public PipeliningFlushState newPipeline() {
            return INSTANCE;
        }

        @Override
        public void onOpen(StatefulConnection<?, ?> connection) {
            connection.setAutoFlushCommands(false);
        }

        @Override
        public void onCommand(StatefulConnection<?, ?> connection) {
        }

        @Override
        public void onClose(StatefulConnection<?, ?> connection) {
            connection.flushCommands();
            connection.setAutoFlushCommands(true);
        }
    }

    /**
     * 缓冲刷新的管道状态
     */
    private static class BufferedFlushing implements PipeliningFlushState {
        private final AtomicLong commands = new AtomicLong();
        private final int flushAfter;

        public BufferedFlushing(int flushAfter) {
            this.flushAfter = flushAfter;
        }

        @Override
        public void onOpen(StatefulConnection<?, ?> connection) {
            connection.setAutoFlushCommands(false);
        }

        @Override
        public void onCommand(StatefulConnection<?, ?> connection) {
            if (commands.incrementAndGet() % flushAfter == 0) {
                connection.flushCommands();
            }
        }

        @Override
        public void onClose(StatefulConnection<?, ?> connection) {
            connection.flushCommands();
            connection.setAutoFlushCommands(true);
        }
    }

    static class CustomCommandType implements ProtocolKeyword {
        private final String name;

        CustomCommandType(String name) {
            this.name = name;
        }

        @Override
        public byte[] getBytes() {
            return name.getBytes(StandardCharsets.US_ASCII);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CustomCommandType)) {
                return false;
            }
            CustomCommandType that = (CustomCommandType) o;
            return ObjectUtils.nullSafeEquals(name, that.name);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
