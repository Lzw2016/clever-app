package org.clever.data.redis.stream;

import org.clever.core.convert.ConversionFailedException;
import org.clever.core.convert.TypeDescriptor;
import org.clever.dao.DataAccessResourceFailureException;
import org.clever.data.redis.connection.stream.*;
import org.clever.data.redis.stream.StreamMessageListenerContainer.ConsumerStreamReadRequest;
import org.clever.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.clever.util.ErrorHandler;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link Task} that invokes a {@link BiFunction read function} to poll on a Redis Stream.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 11:09 <br/>
 */
class StreamPollTask<K, V extends Record<K, ?>> implements Task {
    private final StreamListener<K, V> listener;
    private final ErrorHandler errorHandler;
    private final Predicate<Throwable> cancelSubscriptionOnError;
    private final Function<ReadOffset, List<ByteRecord>> readFunction;
    private final Function<ByteRecord, V> deserializer;
    private final PollState pollState;
    private final TypeDescriptor targetType;
    private volatile boolean isInEventLoop = false;

    StreamPollTask(StreamReadRequest<K> streamRequest,
                   StreamListener<K, V> listener,
                   ErrorHandler errorHandler,
                   TypeDescriptor targetType,
                   Function<ReadOffset, List<ByteRecord>> readFunction,
                   Function<ByteRecord, V> deserializer) {
        this.listener = listener;
        this.errorHandler = Optional.ofNullable(streamRequest.getErrorHandler()).orElse(errorHandler);
        this.cancelSubscriptionOnError = streamRequest.getCancelSubscriptionOnError();
        this.readFunction = readFunction;
        this.deserializer = deserializer;
        this.pollState = createPollState(streamRequest);
        this.targetType = targetType;
    }

    private static PollState createPollState(StreamReadRequest<?> streamRequest) {
        StreamOffset<?> streamOffset = streamRequest.getStreamOffset();
        if (streamRequest instanceof ConsumerStreamReadRequest) {
            return PollState.consumer(((ConsumerStreamReadRequest<?>) streamRequest).getConsumer(), streamOffset.getOffset());
        }
        return PollState.standalone(streamOffset.getOffset());
    }

    @Override
    public void cancel() throws DataAccessResourceFailureException {
        this.pollState.cancel();
    }

    @Override
    public State getState() {
        return pollState.getState();
    }

    @Override
    public boolean awaitStart(Duration timeout) throws InterruptedException {
        return pollState.awaitStart(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void run() {
        pollState.starting();
        try {
            isInEventLoop = true;
            pollState.running();
            doLoop();
        } finally {
            isInEventLoop = false;
        }
    }

    private void doLoop() {
        do {
            try {
                // noinspection BusyWait | 允许中断
                Thread.sleep(0);
                List<ByteRecord> raw = readRecords();
                deserializeAndEmitRecords(raw);
            } catch (InterruptedException e) {
                cancel();
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                if (cancelSubscriptionOnError.test(e)) {
                    cancel();
                }
                errorHandler.handleError(e);
            }
        } while (pollState.isSubscriptionActive());
    }

    private List<ByteRecord> readRecords() {
        return readFunction.apply(pollState.getCurrentReadOffset());
    }

    private void deserializeAndEmitRecords(List<ByteRecord> records) {
        for (ByteRecord raw : records) {
            try {
                pollState.updateReadOffset(raw.getId().getValue());
                V record = convertRecord(raw);
                listener.onMessage(record);
            } catch (RuntimeException e) {
                if (cancelSubscriptionOnError.test(e)) {
                    cancel();
                    errorHandler.handleError(e);
                    return;
                }
                errorHandler.handleError(e);
            }
        }
    }

    private V convertRecord(ByteRecord record) {
        try {
            return deserializer.apply(record);
        } catch (RuntimeException e) {
            throw new ConversionFailedException(TypeDescriptor.forObject(record), targetType, record, e);
        }
    }

    @Override
    public boolean isActive() {
        return State.RUNNING.equals(getState()) || isInEventLoop;
    }

    /**
     * 表示特定流订阅的当前轮询状态的对象
     */
    static class PollState {
        private final ReadOffsetStrategy readOffsetStrategy;
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private final Optional<Consumer> consumer;
        private volatile ReadOffset currentOffset;
        private volatile State state = State.CREATED;
        private volatile CountDownLatch awaitStart = new CountDownLatch(1);

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private PollState(Optional<Consumer> consumer, ReadOffsetStrategy readOffsetStrategy, ReadOffset currentOffset) {
            this.readOffsetStrategy = readOffsetStrategy;
            this.currentOffset = currentOffset;
            this.consumer = consumer;
        }

        /**
         * 为独立读取创建一个新的状态对象
         *
         * @param offset 要使用的 {@link ReadOffset}
         * @return {@link PollState} 的新实例
         */
        static PollState standalone(ReadOffset offset) {
            ReadOffsetStrategy strategy = ReadOffsetStrategy.getStrategy(offset);
            return new PollState(Optional.empty(), strategy, strategy.getFirst(offset, Optional.empty()));
        }

        /**
         * 为 consumer/group-read 创建一个新的状态对象
         *
         * @param consumer 要使用的 {@link Consumer}
         * @param offset   应用的 {@link ReadOffset}
         * @return {@link PollState} 的新实例
         */
        static PollState consumer(Consumer consumer, ReadOffset offset) {
            ReadOffsetStrategy strategy = ReadOffsetStrategy.getStrategy(offset);
            Optional<Consumer> optionalConsumer = Optional.of(consumer);
            return new PollState(optionalConsumer, strategy, strategy.getFirst(offset, optionalConsumer));
        }

        boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
            return awaitStart.await(timeout, unit);
        }

        public State getState() {
            return state;
        }

        /**
         * @return {@literal true} 如果订阅有效
         */
        boolean isSubscriptionActive() {
            return state == State.STARTING || state == State.RUNNING;
        }

        /**
         * 将状态设置为 {@link Task.State#STARTING}
         */
        void starting() {
            state = State.STARTING;
        }

        /**
         * 将状态切换为 {@link Task.State#RUNNING}
         */
        void running() {
            state = State.RUNNING;
            CountDownLatch awaitStart = this.awaitStart;
            if (awaitStart.getCount() == 1) {
                awaitStart.countDown();
            }
        }

        /**
         * 将状态设置为 {@link Task.State#CANCELLED} 并重新武装 {@link #awaitStart(long, TimeUnit) await synchronizer}。
         */
        void cancel() {
            awaitStart = new CountDownLatch(1);
            state = State.CANCELLED;
        }

        /**
         * 推进 {@link ReadOffset}
         */
        void updateReadOffset(String messageId) {
            currentOffset = readOffsetStrategy.getNext(getCurrentReadOffset(), consumer, messageId);
        }

        ReadOffset getCurrentReadOffset() {
            return currentOffset;
        }
    }
}
