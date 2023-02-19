package org.clever.data.redis.stream;

import org.clever.core.convert.TypeDescriptor;
import org.clever.dao.DataAccessResourceFailureException;
import org.clever.data.redis.connection.RedisConnectionFactory;
import org.clever.data.redis.connection.stream.*;
import org.clever.data.redis.core.RedisCallback;
import org.clever.data.redis.core.RedisTemplate;
import org.clever.data.redis.core.StreamOperations;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.util.Assert;
import org.clever.util.ErrorHandler;
import org.clever.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * 基于{@link StreamMessageListenerContainer} 的简单{@link Executor} 实现，用于运行{@link Task tasks} 以轮询Redis Streams。
 * <p>
 * 此消息容器创建在 {@link Executor} 上执行的长时间运行的任务
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 10:41 <br/>
 */
class DefaultStreamMessageListenerContainer<K, V extends Record<K, ?>> implements StreamMessageListenerContainer<K, V> {
    private final Object lifecycleMonitor = new Object();
    private final Executor taskExecutor;
    private final ErrorHandler errorHandler;
    private final StreamReadOptions readOptions;
    private final RedisTemplate<K, ?> template;
    private final StreamOperations<K, Object, Object> streamOperations;
    private final StreamMessageListenerContainerOptions<K, V> containerOptions;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private boolean running = false;

    /**
     * 创建一个新的 {@link DefaultStreamMessageListenerContainer}
     *
     * @param connectionFactory 不得为 {@literal null}
     * @param containerOptions  不得为 {@literal null}
     */
    DefaultStreamMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            StreamMessageListenerContainerOptions<K, V> containerOptions) {
        Assert.notNull(connectionFactory, "RedisConnectionFactory must not be null!");
        Assert.notNull(containerOptions, "StreamMessageListenerContainerOptions must not be null!");
        this.taskExecutor = containerOptions.getExecutor();
        this.errorHandler = containerOptions.getErrorHandler();
        this.readOptions = getStreamReadOptions(containerOptions);
        this.template = createRedisTemplate(connectionFactory, containerOptions);
        this.containerOptions = containerOptions;
        if (containerOptions.hasHashMapper()) {
            this.streamOperations = this.template.opsForStream(containerOptions.getRequiredHashMapper());
        } else {
            this.streamOperations = this.template.opsForStream();
        }
    }

    private static StreamReadOptions getStreamReadOptions(StreamMessageListenerContainerOptions<?, ?> options) {
        StreamReadOptions readOptions = StreamReadOptions.empty();
        if (options.getBatchSize().isPresent()) {
            readOptions = readOptions.count(options.getBatchSize().getAsInt());
        }
        if (!options.getPollTimeout().isZero()) {
            readOptions = readOptions.block(options.getPollTimeout());
        }
        return readOptions;
    }

    private RedisTemplate<K, V> createRedisTemplate(
            RedisConnectionFactory connectionFactory,
            StreamMessageListenerContainerOptions<K, V> containerOptions) {
        RedisTemplate<K, V> template = new RedisTemplate<>();
        template.setKeySerializer(containerOptions.getKeySerializer());
        template.setValueSerializer(containerOptions.getKeySerializer());
        template.setHashKeySerializer(containerOptions.getHashKeySerializer());
        template.setHashValueSerializer(containerOptions.getHashValueSerializer());
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        synchronized (lifecycleMonitor) {
            if (this.running) {
                return;
            }
            subscriptions.stream()
                    .filter(it -> !it.isActive())
                    .filter(it -> it instanceof TaskSubscription)
                    .map(TaskSubscription.class::cast)
                    .map(TaskSubscription::getTask)
                    .forEach(taskExecutor::execute);
            running = true;
        }
    }

    @Override
    public void stop() {
        synchronized (lifecycleMonitor) {
            if (this.running) {
                subscriptions.forEach(Cancelable::cancel);
                running = false;
            }
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (this.lifecycleMonitor) {
            return running;
        }
    }

    @Override
    public Subscription register(StreamReadRequest<K> streamRequest, StreamListener<K, V> listener) {
        return doRegister(getReadTask(streamRequest, listener));
    }

    private StreamPollTask<K, V> getReadTask(StreamReadRequest<K> streamRequest, StreamListener<K, V> listener) {
        Function<ReadOffset, List<ByteRecord>> readFunction = getReadFunction(streamRequest);
        Function<ByteRecord, V> deserializerToUse = getDeserializer();
        TypeDescriptor targetType = TypeDescriptor.valueOf(
                containerOptions.hasHashMapper() ? containerOptions.getTargetType() : MapRecord.class
        );
        return new StreamPollTask<>(streamRequest, listener, errorHandler, targetType, readFunction, deserializerToUse);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Function<ByteRecord, V> getDeserializer() {
        Function<ByteRecord, MapRecord<K, Object, Object>> deserializer = streamOperations::deserializeRecord;
        if (containerOptions.getHashMapper() == null) {
            return (Function) deserializer;
        }
        return source -> {
            MapRecord<K, Object, Object> intermediate = deserializer.apply(source);
            return (V) streamOperations.map(intermediate, this.containerOptions.getTargetType());
        };
    }

    @SuppressWarnings("unchecked")
    private Function<ReadOffset, List<ByteRecord>> getReadFunction(StreamReadRequest<K> streamRequest) {
        byte[] rawKey = ((RedisSerializer<K>) template.getKeySerializer()).serialize(streamRequest.getStreamOffset().getKey());
        if (streamRequest instanceof StreamMessageListenerContainer.ConsumerStreamReadRequest) {
            ConsumerStreamReadRequest<K> consumerStreamRequest = (ConsumerStreamReadRequest<K>) streamRequest;
            StreamReadOptions readOptions = consumerStreamRequest.isAutoAcknowledge() ? this.readOptions.autoAcknowledge() : this.readOptions;
            Consumer consumer = consumerStreamRequest.getConsumer();
            return (offset) -> template.execute((RedisCallback<List<ByteRecord>>) connection -> connection.streamCommands().xReadGroup(consumer, readOptions, StreamOffset.create(rawKey, offset)));
        }
        return (offset) -> template.execute((RedisCallback<List<ByteRecord>>) connection -> connection.streamCommands().xRead(readOptions, StreamOffset.create(rawKey, offset)));
    }

    private Subscription doRegister(Task task) {
        Subscription subscription = new TaskSubscription(task);
        synchronized (lifecycleMonitor) {
            this.subscriptions.add(subscription);
            if (this.running) {
                taskExecutor.execute(task);
            }
        }
        return subscription;
    }

    @Override
    public void remove(Subscription subscription) {
        synchronized (lifecycleMonitor) {
            if (subscriptions.contains(subscription)) {
                if (subscription.isActive()) {
                    subscription.cancel();
                }
                subscriptions.remove(subscription);
            }
        }
    }

    /**
     * {@link Subscription} 包装一个 {@link Task}
     */
    static class TaskSubscription implements Subscription {
        private final Task task;

        protected TaskSubscription(Task task) {
            this.task = task;
        }

        Task getTask() {
            return task;
        }

        @Override
        public boolean isActive() {
            return task.isActive();
        }

        @Override
        public boolean await(Duration timeout) throws InterruptedException {
            return task.awaitStart(timeout);
        }

        @Override
        public void cancel() throws DataAccessResourceFailureException {
            task.cancel();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TaskSubscription that = (TaskSubscription) o;
            return ObjectUtils.nullSafeEquals(task, that.task);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(task);
        }
    }

    /**
     * 记录 {@link ErrorHandler}
     */
    enum LoggingErrorHandler implements ErrorHandler {
        INSTANCE;
        private final Logger logger;

        LoggingErrorHandler() {
            this.logger = LoggerFactory.getLogger(LoggingErrorHandler.class);
        }

        public void handleError(Throwable t) {
            if (this.logger.isErrorEnabled()) {
                this.logger.error("Unexpected error occurred in scheduled task.", t);
            }
        }
    }
}
