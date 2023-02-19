package org.clever.data.redis.stream;

import org.clever.context.SmartLifecycle;
import org.clever.core.SharedThreadPoolExecutor;
import org.clever.data.redis.connection.RedisConnectionFactory;
import org.clever.data.redis.connection.stream.*;
import org.clever.data.redis.hash.HashMapper;
import org.clever.data.redis.hash.Jackson2HashMapper;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.data.redis.serializer.StringRedisSerializer;
import org.clever.data.redis.stream.DefaultStreamMessageListenerContainer.LoggingErrorHandler;
import org.clever.util.Assert;
import org.clever.util.ErrorHandler;

import java.time.Duration;
import java.util.OptionalInt;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * 表示消息侦听器容器的框架使用的抽象。<br />
 * 不打算在外部实施。
 * <p>
 * 创建后，{@link StreamMessageListenerContainer} 可以订阅 Redis Stream 并使用传入的 {@link Record messages}。
 * {@link StreamMessageListenerContainer} 允许多个流读取请求并为每个读取请求返回一个 {@link Subscription} 句柄。
 * 取消 {@link Subscription} 最终终止后台轮询。
 * 使用 {@link RedisSerializer key and value serializers} 转换消息以支持各种序列化策略。 <br/>
 * {@link StreamMessageListenerContainer} 支持多种流消费模式：
 * <ul>
 * <li>Standalone</li>
 * <li>使用带有外部 {@link org.clever.data.redis.core.StreamOperations#acknowledge(Object, String, String...)} acknowledge 的 {@link Consumer}</li>
 * <li>使用具有自动确认功能的 {@link Consumer}</li>
 * </ul>
 * 从流中读取需要轮询和推进流偏移量的策略。根据初始的 {@link ReadOffset}，{@link StreamMessageListenerContainer} 应用单独的策略来获取下一个 {@link ReadOffset}：<br/>
 * <strong>Standalone</strong>
 * <ul>
 * <li>{@link ReadOffset#from(String)} 使用特定消息 ID 的偏移量：从给定的偏移量开始，并使用最后一次看到的 {@link Record#getId() message Id}。</li>
 * <li>{@link ReadOffset#lastConsumed()} 最后消费：从最新的偏移量 ({@code $}) 开始并使用最后一次看到的 {@link Record#getId() message Id}。</li>
 * <li>{@link ReadOffset#latest()} 最后消费：从最新的偏移量 ({@code $}) 开始，并使用最新的偏移量 ({@code $}) 进行后续读取。</li>
 * </ul>
 * <br/>
 * <strong>使用 {@link Consumer}</strong>
 * <ul>
 * <li>{@link ReadOffset#from(String)} 使用特定消息 ID 的偏移量：从给定的偏移量开始，并使用最后一次看到的 {@link Record#getId() 消息 Id}。</li>
 * <li>{@link ReadOffset#lastConsumed()} Last consumed：从消费者最后消费的消息（{@code >}）开始，使用消费者最后消费的消息（{@code >}）进行后续读取。</li>
 * <li>{@link ReadOffset#latest()} Last consumed：从最新的偏移量 ({@code $}) 开始，并使用最新的偏移量 ({@code $}) 进行后续读取。</li>
 * </ul>
 * <strong>注意：使用 {@link ReadOffset#latest()} 有可能丢失消息，因为消息可以在轮询暂停期间到达。使用 messagedId 作为偏移量或 {@link ReadOffset#lastConsumed()} 以尽量减少消息丢失的可能性。</strong>
 * <p>
 * {@link StreamMessageListenerContainer} 需要一个 {@link Executor} 在不同的 {@link Thread} 上分叉长时间运行的轮询任务。
 * 此线程用作事件循环以轮询流消息并调用 {@link StreamListener#onMessage(Record) listener callback}。
 * <p>
 * {@link StreamMessageListenerContainer} 任务在流读取和{@link StreamListener#onMessage(Record) listener notification}期间将错误传播到可配置的{@link ErrorHandler}。
 * 默认情况下，错误会停止 {@link Subscription}。
 * 为 {@link StreamReadRequest} 配置 {@link Predicate} 允许有条件的订阅取消或继续所有错误。
 * <p>
 * 请参阅以下示例代码如何使用 {@link StreamMessageListenerContainer}：
 *
 * <pre>{@code
 * RedisConnectionFactory factory = …;
 * StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer.create(factory);
 * Subscription subscription = container.receive(StreamOffset.fromStart("my-stream"), message -> …);
 * container.start();
 * // later
 * container.stop();
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/17 23:30 <br/>
 *
 * @param <K> 流键和流字段类型
 * @param <V> 流值类型
 * @see StreamMessageListenerContainerOptions#builder()
 * @see StreamListener
 * @see StreamReadRequest
 * @see ConsumerStreamReadRequest
 * @see StreamMessageListenerContainerOptionsBuilder#executor(Executor)
 * @see ErrorHandler
 * @see org.clever.data.redis.core.StreamOperations
 * @see RedisConnectionFactory
 */
public interface StreamMessageListenerContainer<K, V extends Record<K, ?>> extends SmartLifecycle {
    /**
     * 使用 {@link StringRedisSerializer string serializers} 给定 {@link RedisConnectionFactory} 创建一个新的 {@link StreamMessageListenerContainer}
     *
     * @param connectionFactory 不得为 {@literal null}
     * @return 新的 {@link StreamMessageListenerContainer}
     */
    static StreamMessageListenerContainer<String, MapRecord<String, String, String>> create(RedisConnectionFactory connectionFactory) {
        Assert.notNull(connectionFactory, "RedisConnectionFactory must not be null!");
        return create(connectionFactory, StreamMessageListenerContainerOptions.builder().serializer(StringRedisSerializer.UTF_8).build());
    }

    /**
     * 给定 {@link RedisConnectionFactory} 和 {@link StreamMessageListenerContainerOptions} 创建一个新的 {@link StreamMessageListenerContainer}
     *
     * @param connectionFactory 不得为 {@literal null}
     * @param options           不得为 {@literal null}
     * @return 新的 {@link StreamMessageListenerContainer}
     */
    static <K, V extends Record<K, ?>> StreamMessageListenerContainer<K, V> create(RedisConnectionFactory connectionFactory, StreamMessageListenerContainerOptions<K, V> options) {
        Assert.notNull(connectionFactory, "RedisConnectionFactory must not be null!");
        Assert.notNull(options, "StreamMessageListenerContainerOptions must not be null!");
        return new DefaultStreamMessageListenerContainer<>(connectionFactory, options);
    }

    /**
     * 为 Redis Stream 注册一个新的订阅。
     * 如果 {@link StreamMessageListenerContainer#isRunning() 已经在运行}，{@link Subscription} 将被添加并立即运行，
     * 否则它将在容器实际 {@link StreamMessageListenerContainer#start() 启动} 时被安排和启动。
     * <p>
     * {@link Record} 检索期间的错误导致基础任务的 {@link Subscription#cancel() 取消}。
     * <p>
     * 在 {@link StreamMessageListenerContainer#stop()} 上，所有 {@link Subscription 订阅} 在关闭容器本身之前被取消。
     *
     * @param streamOffset 流偏移量
     * @param listener     不得为 {@literal null}
     * @return 订阅句柄
     * @see StreamOffset#create(Object, ReadOffset)
     */
    default Subscription receive(StreamOffset<K> streamOffset, StreamListener<K, V> listener) {
        return register(StreamReadRequest.builder(streamOffset).build(), listener);
    }

    /**
     * 为 Redis Stream 注册一个新的订阅。
     * 如果 {@link StreamMessageListenerContainer#isRunning() 已经在运行}，{@link Subscription} 将被添加并立即运行，
     * 否则它将在容器实际 {@link StreamMessageListenerContainer#start() 启动} 时被安排和启动。
     * <p>
     * 处理后必须使用 {@link org.clever.data.redis.core.StreamOperations#acknowledge(Object, String, String...)} 确认每条消息。
     * <p>
     * {@link Record} 检索期间的错误导致基础任务的 {@link Subscription#cancel() 取消}。
     * <p>
     * 在 {@link StreamMessageListenerContainer#stop()} 上，所有 {@link Subscription 订阅} 在关闭容器本身之前被取消。
     *
     * @param consumer     消费群体， 不得为 {@literal null}
     * @param streamOffset 流偏移量
     * @param listener     不得为 {@literal null}
     * @return 订阅句柄
     * @see StreamOffset#create(Object, ReadOffset)
     * @see ReadOffset#lastConsumed()
     */
    default Subscription receive(Consumer consumer, StreamOffset<K> streamOffset, StreamListener<K, V> listener) {
        return register(StreamReadRequest.builder(streamOffset).consumer(consumer).autoAcknowledge(false).build(), listener);
    }

    /**
     * 为 Redis Stream 注册一个新的订阅。
     * 如果 {@link StreamMessageListenerContainer#isRunning() 已经在运行}，{@link Subscription} 将被添加并立即运行，
     * 否则它将在容器实际 {@link StreamMessageListenerContainer#start() 启动} 时被安排和启动。
     * <p>
     * 每条消息在收到时都会被确认。
     * <p>
     * {@link Record} 检索期间的错误导致基础任务的 {@link Subscription#cancel() 取消}。
     * <p>
     * 在 {@link StreamMessageListenerContainer#stop()} 上，所有 {@link Subscription 订阅} 在关闭容器本身之前被取消。
     *
     * @param consumer     消费群体， 不得为 {@literal null}
     * @param streamOffset 流偏移量
     * @param listener     不得为 {@literal null}
     * @return 订阅句柄
     * @see StreamOffset#create(Object, ReadOffset)
     * @see ReadOffset#lastConsumed()
     */
    default Subscription receiveAutoAck(Consumer consumer, StreamOffset<K> streamOffset, StreamListener<K, V> listener) {
        return register(StreamReadRequest.builder(streamOffset).consumer(consumer).autoAcknowledge(true).build(), listener);
    }

    /**
     * 为 Redis Stream 注册一个新的订阅。
     * 如果 {@link StreamMessageListenerContainer#isRunning() 已经在运行}，{@link Subscription} 将被添加并立即运行，
     * 否则它将在容器实际 {@link StreamMessageListenerContainer#start() 启动} 时被安排和启动。
     * <p>
     * {@link Record} 期间的错误根据测试 {@link StreamReadRequest#getCancelSubscriptionOnError() cancellation predicate} 是否取消基础任务进行测试。
     * <p>
     * 在 {@link StreamMessageListenerContainer#stop()} 上，所有 {@link Subscription subscriptions} 在关闭容器本身之前被取消。
     * <p>
     * {@link Record} 检索期间的错误委托给给定的 {@link StreamReadRequest#getErrorHandler()}。
     *
     * @param streamRequest 不得为 {@literal null}
     * @param listener      不得为 {@literal null}
     * @return 订阅句柄
     * @see StreamReadRequest
     * @see ConsumerStreamReadRequest
     */
    Subscription register(StreamReadRequest<K> streamRequest, StreamListener<K, V> listener);

    /**
     * 从容器中取消注册给定的 {@link Subscription}。
     * 这可以防止 {@link Subscription} 在潜在的 {@link SmartLifecycle#stop() stop}/{@link SmartLifecycle#start() start} 场景中重新启动。<br />
     * {@link Subscription#isActive() active} {@link Subscription subscription} 在删除之前被 {@link Subscription#cancel() cancelled}。
     *
     * @param subscription 不得为 {@literal null}
     */
    void remove(Subscription subscription);

    /**
     * 请求读取 Redis 流
     *
     * @param <K> 流键和流字段类型
     * @see StreamReadRequestBuilder
     */
    class StreamReadRequest<K> {
        private final StreamOffset<K> streamOffset;
        private final ErrorHandler errorHandler;
        private final Predicate<Throwable> cancelSubscriptionOnError;

        private StreamReadRequest(StreamOffset<K> streamOffset, ErrorHandler errorHandler, Predicate<Throwable> cancelSubscriptionOnError) {
            this.streamOffset = streamOffset;
            this.errorHandler = errorHandler;
            this.cancelSubscriptionOnError = cancelSubscriptionOnError;
        }

        /**
         * @return {@link StreamReadRequest} 的新构建器
         */
        public static <K> StreamReadRequestBuilder<K> builder(StreamOffset<K> offset) {
            return new StreamReadRequestBuilder<>(offset);
        }

        public StreamOffset<K> getStreamOffset() {
            return streamOffset;
        }

        public ErrorHandler getErrorHandler() {
            return errorHandler;
        }

        public Predicate<Throwable> getCancelSubscriptionOnError() {
            return cancelSubscriptionOnError;
        }
    }

    /**
     * 请求使用 {@link Consumer} 读取 Redis Stream
     *
     * @param <K> 流键和流字段类型
     * @see StreamReadRequestBuilder
     */
    class ConsumerStreamReadRequest<K> extends StreamReadRequest<K> {
        private final Consumer consumer;
        private final boolean autoAck;

        private ConsumerStreamReadRequest(StreamOffset<K> streamOffset,
                                          ErrorHandler errorHandler,
                                          Predicate<Throwable> cancelSubscriptionOnError,
                                          Consumer consumer,
                                          boolean autoAck) {
            super(streamOffset, errorHandler, cancelSubscriptionOnError);
            this.consumer = consumer;
            this.autoAck = autoAck;
        }

        public Consumer getConsumer() {
            return consumer;
        }

        public boolean isAutoAcknowledge() {
            return autoAck;
        }
    }

    /**
     * 构建 {@link StreamReadRequest} 的构建器
     *
     * @param <K> 流键和流字段类型
     */
    class StreamReadRequestBuilder<K> {
        final StreamOffset<K> streamOffset;
        ErrorHandler errorHandler;
        Predicate<Throwable> cancelSubscriptionOnError = t -> true;

        StreamReadRequestBuilder(StreamOffset<K> streamOffset) {
            this.streamOffset = streamOffset;
        }

        StreamReadRequestBuilder(StreamReadRequestBuilder<K> other) {
            this.streamOffset = other.streamOffset;
            this.errorHandler = other.errorHandler;
            this.cancelSubscriptionOnError = other.cancelSubscriptionOnError;
        }

        /**
         * 配置 {@link ErrorHandler} 以在 {@link Throwable errors} 上收到通知
         *
         * @param errorHandler 不得为空
         * @return {@code this} {@link StreamReadRequestBuilder}.
         */
        public StreamReadRequestBuilder<K> errorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * 配置取消 {@link Predicate} 以在 {@link Throwable errors} 上收到通知。
         * {@link Predicate} 的结果决定是否通过返回 {@literal true} 来取消订阅。
         *
         * @param cancelSubscriptionOnError 不得为空
         * @return {@code this} {@link StreamReadRequestBuilder}.
         */
        public StreamReadRequestBuilder<K> cancelOnError(Predicate<Throwable> cancelSubscriptionOnError) {
            this.cancelSubscriptionOnError = cancelSubscriptionOnError;
            return this;
        }

        /**
         * 配置 {@link Consumer} 以在消费者组中消费流消息
         *
         * @param consumer 不得为空
         * @return 一个新的 {@link ConsumerStreamReadRequestBuilder}
         */
        public ConsumerStreamReadRequestBuilder<K> consumer(Consumer consumer) {
            return new ConsumerStreamReadRequestBuilder<>(this).consumer(consumer);
        }

        /**
         * 构建 {@link StreamReadRequest} 的新实例
         *
         * @return {@link StreamReadRequest} 的新实例
         */
        public StreamReadRequest<K> build() {
            return new StreamReadRequest<>(streamOffset, errorHandler, cancelSubscriptionOnError);
        }
    }

    /**
     * 构建 {@link ConsumerStreamReadRequest} 的构建器
     *
     * @param <K> 流键和流字段类型
     */
    class ConsumerStreamReadRequestBuilder<K> extends StreamReadRequestBuilder<K> {
        private Consumer consumer;
        private boolean autoAck = true;

        ConsumerStreamReadRequestBuilder(StreamReadRequestBuilder<K> other) {
            super(other);
        }

        /**
         * 配置 {@link ErrorHandler} 以在 {@link Throwable 读取、反序列化和侦听器错误}时收到通知
         *
         * @param errorHandler 不得为空
         * @return {@code this} {@link ConsumerStreamReadRequestBuilder}.
         */
        public ConsumerStreamReadRequestBuilder<K> errorHandler(ErrorHandler errorHandler) {
            super.errorHandler(errorHandler);
            return this;
        }

        /**
         * 配置取消 {@link Predicate} 以在 {@link Throwable 读取、反序列化和侦听器错误}时收到通知。
         * {@link Predicate} 的结果决定是否通过返回 {@literal true} 来取消订阅。
         *
         * @param cancelSubscriptionOnError 不得为空
         * @return {@code this} {@link ConsumerStreamReadRequestBuilder}.
         */
        public ConsumerStreamReadRequestBuilder<K> cancelOnError(Predicate<Throwable> cancelSubscriptionOnError) {
            super.cancelOnError(cancelSubscriptionOnError);
            return this;
        }

        /**
         * 配置 {@link Consumer} 以在消费者组中消费流消息
         *
         * @param consumer 不得为空
         * @return {@code this} {@link ConsumerStreamReadRequestBuilder}.
         */
        public ConsumerStreamReadRequestBuilder<K> consumer(Consumer consumer) {
            this.consumer = consumer;
            return this;
        }

        /**
         * 配置流消息消费的自动确认
         *
         * @param autoAck {@literal true}（默认）自动确认收到的消息或 {@literal false} 用于外部确认
         * @return {@code this} {@link ConsumerStreamReadRequestBuilder}.
         */
        public ConsumerStreamReadRequestBuilder<K> autoAcknowledge(boolean autoAck) {
            this.autoAck = autoAck;
            return this;
        }

        /**
         * 构建 {@link ConsumerStreamReadRequest} 的新实例
         *
         * @return {@link ConsumerStreamReadRequest} 的新实例
         */
        public ConsumerStreamReadRequest<K> build() {
            return new ConsumerStreamReadRequest<>(streamOffset, errorHandler, cancelSubscriptionOnError, consumer, autoAck);
        }
    }

    /**
     * {@link StreamMessageListenerContainer} 的选项
     *
     * @param <K> 流键和流字段类型
     * @param <V> 流值类型
     * @see StreamMessageListenerContainerOptionsBuilder
     */
    class StreamMessageListenerContainerOptions<K, V extends Record<K, ?>> {
        private final Duration pollTimeout;
        private final Integer batchSize;
        private final RedisSerializer<K> keySerializer;
        private final RedisSerializer<Object> hashKeySerializer;
        private final RedisSerializer<Object> hashValueSerializer;
        private final Class<Object> targetType;
        private final HashMapper<Object, Object, Object> hashMapper;
        private final ErrorHandler errorHandler;
        private final Executor executor;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private StreamMessageListenerContainerOptions(Duration pollTimeout,
                                                      Integer batchSize,
                                                      RedisSerializer<K> keySerializer,
                                                      RedisSerializer<Object> hashKeySerializer,
                                                      RedisSerializer<Object> hashValueSerializer,
                                                      Class<?> targetType,
                                                      HashMapper<V, ?, ?> hashMapper,
                                                      ErrorHandler errorHandler,
                                                      Executor executor) {
            this.pollTimeout = pollTimeout;
            this.batchSize = batchSize;
            this.keySerializer = keySerializer;
            this.hashKeySerializer = hashKeySerializer;
            this.hashValueSerializer = hashValueSerializer;
            this.targetType = (Class) targetType;
            this.hashMapper = (HashMapper) hashMapper;
            this.errorHandler = errorHandler;
            this.executor = executor;
        }

        /**
         * @return {@link StreamMessageListenerContainerOptions} 的新构建器
         */
        public static StreamMessageListenerContainerOptionsBuilder<String, MapRecord<String, String, String>> builder() {
            return new StreamMessageListenerContainerOptionsBuilder<>().serializer(StringRedisSerializer.UTF_8);
        }

        /**
         * 在读取期间使用 {@code BLOCK} 选项阻止轮询的超时
         *
         * @return 超时
         */
        public Duration getPollTimeout() {
            return pollTimeout;
        }

        /**
         * 在读取期间使用 {@code COUNT} 选项进行批量大小轮询
         *
         * @return 批量大小
         */
        public OptionalInt getBatchSize() {
            return batchSize != null ? OptionalInt.of(batchSize) : OptionalInt.empty();
        }

        public RedisSerializer<K> getKeySerializer() {
            return keySerializer;
        }

        public RedisSerializer<Object> getHashKeySerializer() {
            return hashKeySerializer;
        }

        public RedisSerializer<Object> getHashValueSerializer() {
            return hashValueSerializer;
        }

        public HashMapper<Object, Object, Object> getHashMapper() {
            return hashMapper;
        }

        public HashMapper<Object, Object, Object> getRequiredHashMapper() {
            if (!hasHashMapper()) {
                throw new IllegalStateException("No HashMapper configured");
            }
            return hashMapper;
        }

        public boolean hasHashMapper() {
            return hashMapper != null;
        }

        public Class<Object> getTargetType() {
            if (this.targetType != null) {
                return targetType;
            }
            return Object.class;
        }

        /**
         * @return 默认的 {@link ErrorHandler}
         */
        public ErrorHandler getErrorHandler() {
            return errorHandler;
        }

        /**
         * @return {@link Executor} 运行流轮询 {@link Task}。默认为 {@code SimpleAsyncTaskExecutor}
         */
        public Executor getExecutor() {
            return executor;
        }
    }

    /**
     * {@link StreamMessageListenerContainerOptions} 的构建器
     *
     * @param <K> 流键和流字段类型
     * @param <V> 流值类型
     */
    @SuppressWarnings("unchecked")
    class StreamMessageListenerContainerOptionsBuilder<K, V extends Record<K, ?>> {
        private Duration pollTimeout = Duration.ofSeconds(2);
        private Integer batchSize;
        private RedisSerializer<K> keySerializer;
        private RedisSerializer<Object> hashKeySerializer;
        private RedisSerializer<Object> hashValueSerializer;
        private HashMapper<V, ?, ?> hashMapper;
        private Class<?> targetType;
        private ErrorHandler errorHandler = LoggingErrorHandler.INSTANCE;
        private Executor executor = SharedThreadPoolExecutor.getNormal();

        private StreamMessageListenerContainerOptionsBuilder() {
        }

        /**
         * 在读取期间为 {@code BLOCK} 选项配置轮询超时
         *
         * @param pollTimeout 不得为 {@literal null} or negative.
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}.
         */
        public StreamMessageListenerContainerOptionsBuilder<K, V> pollTimeout(Duration pollTimeout) {
            Assert.notNull(pollTimeout, "Poll timeout must not be null!");
            Assert.isTrue(!pollTimeout.isNegative(), "Poll timeout must not be negative!");
            this.pollTimeout = pollTimeout;
            return this;
        }

        /**
         * 在读取期间为 {@code COUNT} 选项配置批量大小
         *
         * @param messagesPerPoll 必须大于零
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        public StreamMessageListenerContainerOptionsBuilder<K, V> batchSize(int messagesPerPoll) {
            Assert.isTrue(messagesPerPoll > 0, "Batch size must be greater zero!");
            this.batchSize = messagesPerPoll;
            return this;
        }

        /**
         * 配置 {@link Executor} 以运行流轮询 {@link Task}
         *
         * @param executor 不得为空
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        public StreamMessageListenerContainerOptionsBuilder<K, V> executor(Executor executor) {
            Assert.notNull(executor, "Executor must not be null!");
            this.executor = executor;
            return this;
        }

        /**
         * 配置 {@link ErrorHandler} 以在 {@link Throwable errors} 上收到通知
         *
         * @param errorHandler 不得为空
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        public StreamMessageListenerContainerOptionsBuilder<K, V> errorHandler(ErrorHandler errorHandler) {
            Assert.notNull(errorHandler, "ErrorHandler must not be null!");
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * 配置键、散列键和散列值序列化器
         *
         * @param serializer 不得为 {@literal null}
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        @SuppressWarnings("rawtypes")
        public <T> StreamMessageListenerContainerOptionsBuilder<T, MapRecord<T, T, T>> serializer(RedisSerializer<T> serializer) {
            Assert.notNull(serializer, "RedisSerializer must not be null");
            this.keySerializer = (RedisSerializer) serializer;
            this.hashKeySerializer = (RedisSerializer) serializer;
            this.hashValueSerializer = (RedisSerializer) serializer;
            return (StreamMessageListenerContainerOptionsBuilder) this;
        }

        /**
         * 配置密钥序列化程序
         *
         * @param serializer 不得为 {@literal null}
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        @SuppressWarnings("rawtypes")
        public <NK, NV extends Record<NK, ?>> StreamMessageListenerContainerOptionsBuilder<NK, NV> keySerializer(RedisSerializer<NK> serializer) {
            Assert.notNull(serializer, "RedisSerializer must not be null");
            this.keySerializer = (RedisSerializer) serializer;
            return (StreamMessageListenerContainerOptionsBuilder) this;
        }

        /**
         * 配置哈希键序列化程序
         *
         * @param serializer 不得为 {@literal null}
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        @SuppressWarnings("rawtypes")
        public <HK, HV> StreamMessageListenerContainerOptionsBuilder<K, MapRecord<K, HK, HV>> hashKeySerializer(RedisSerializer<HK> serializer) {
            Assert.notNull(serializer, "RedisSerializer must not be null");
            this.hashKeySerializer = (RedisSerializer) serializer;
            return (StreamMessageListenerContainerOptionsBuilder) this;
        }

        /**
         * 配置哈希值序列化程序
         *
         * @param serializer 不得为 {@literal null}
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        @SuppressWarnings("rawtypes")
        public <HK, HV> StreamMessageListenerContainerOptionsBuilder<K, MapRecord<K, HK, HV>> hashValueSerializer(RedisSerializer<HV> serializer) {
            Assert.notNull(serializer, "RedisSerializer must not be null");
            this.hashValueSerializer = (RedisSerializer) serializer;
            return (StreamMessageListenerContainerOptionsBuilder) this;
        }

        /**
         * 配置哈希目标类型。将发出的 {@link Record} 类型更改为 {@link ObjectRecord}
         *
         * @param targetType 不得为 {@literal null}
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <NV> StreamMessageListenerContainerOptionsBuilder<K, ObjectRecord<K, NV>> targetType(Class<NV> targetType) {
            Assert.notNull(targetType, "Target type must not be null");
            this.targetType = targetType;
            if (this.hashMapper == null) {
                hashKeySerializer(RedisSerializer.byteArray());
                hashValueSerializer(RedisSerializer.byteArray());
                return (StreamMessageListenerContainerOptionsBuilder) objectMapper(Jackson2HashMapper.getSharedInstance());
            }
            return (StreamMessageListenerContainerOptionsBuilder) this;
        }

        /**
         * 配置哈希映射器。将发出的 {@link Record} 类型更改为 {@link ObjectRecord}
         *
         * @param hashMapper 不得为 {@literal null}
         * @return {@code this} {@link StreamMessageListenerContainerOptionsBuilder}
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <NV> StreamMessageListenerContainerOptionsBuilder<K, ObjectRecord<K, NV>> objectMapper(HashMapper<NV, ?, ?> hashMapper) {
            Assert.notNull(hashMapper, "HashMapper must not be null");
            this.hashMapper = (HashMapper) hashMapper;
            return (StreamMessageListenerContainerOptionsBuilder) this;
        }

        /**
         * 构建新的 {@link StreamMessageListenerContainerOptions}
         *
         * @return 新的 {@link StreamMessageListenerContainerOptions}
         */
        public StreamMessageListenerContainerOptions<K, V> build() {
            return new StreamMessageListenerContainerOptions<>(
                    pollTimeout,
                    batchSize,
                    keySerializer,
                    hashKeySerializer,
                    hashValueSerializer,
                    targetType,
                    hashMapper,
                    errorHandler,
                    executor
            );
        }
    }
}

