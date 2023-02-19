package org.clever.data.redis.stream;

import org.clever.data.redis.connection.stream.Consumer;
import org.clever.data.redis.connection.stream.ReadOffset;

import java.util.Optional;

/**
 * 确定第一个和后续 {@link ReadOffset} 的策略
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 11:11 <br/>
 */
enum ReadOffsetStrategy {
    /**
     * 使用最后看到的消息 ID
     */
    NextMessage {
        @Override
        public ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer) {
            return readOffset;
        }

        @Override
        public ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId) {
            return ReadOffset.from(lastConsumedMessageId);
        }
    },
    /**
     * 最后消耗的策略
     */
    LastConsumed {
        @Override
        public ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer) {
            return consumer.map(it -> ReadOffset.lastConsumed()).orElseGet(ReadOffset::latest);
        }

        @Override
        public ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId) {
            return consumer.map(it -> ReadOffset.lastConsumed()).orElseGet(() -> ReadOffset.from(lastConsumedMessageId));
        }
    },
    /**
     * 始终使用最新的流消息
     */
    Latest {
        @Override
        public ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer) {
            return ReadOffset.latest();
        }

        @Override
        public ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId) {
            return ReadOffset.latest();
        }
    };

    /**
     * 返回给定初始 {@link ReadOffset} 的 {@link ReadOffsetStrategy}
     *
     * @param offset 不得为 {@literal null}
     * @return {@link ReadOffsetStrategy}
     */
    static ReadOffsetStrategy getStrategy(ReadOffset offset) {
        if (ReadOffset.latest().equals(offset)) {
            return Latest;
        }
        if (ReadOffset.lastConsumed().equals(offset)) {
            return LastConsumed;
        }
        return NextMessage;
    }

    /**
     * 确定第一个 {@link ReadOffset}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public abstract ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer);

    /**
     * 确定给定 {@code lastConsumedMessageId} 的下一个 {@link ReadOffset}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public abstract ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId);
}
