package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.XClaimArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.models.stream.PendingMessage;
import io.lettuce.core.models.stream.PendingMessages;
import org.clever.core.convert.converter.Converter;
import org.clever.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.clever.data.redis.connection.convert.ListConverter;
import org.clever.data.redis.connection.stream.*;
import org.clever.data.redis.util.ByteUtils;
import org.clever.util.NumberUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 适用于 Redis 流特定类型的转换器。
 * <p>
 * 转换器通常在值对象参数对象之间进行转换，保留值的实际类型（即此处不发生序列化反序列化）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:36 <br/>
 */
class StreamConverters {
    private static final Converter<List<StreamMessage<byte[], byte[]>>, List<RecordId>> MESSAGEs_TO_IDs = new ListConverter<>(messageToIdConverter());

    private static final BiFunction<List<PendingMessage>, String, org.clever.data.redis.connection.stream.PendingMessages> PENDING_MESSAGES_CONVERTER = (source, groupName) -> {
        List<org.clever.data.redis.connection.stream.PendingMessage> messages = source.stream().map(it -> {
            RecordId id = RecordId.of(it.getId());
            Consumer consumer = Consumer.from(groupName, it.getConsumer());
            return new org.clever.data.redis.connection.stream.PendingMessage(
                    id,
                    consumer,
                    Duration.ofMillis(it.getMsSinceLastDelivery()),
                    it.getRedeliveryCount()
            );
        }).collect(Collectors.toList());
        return new org.clever.data.redis.connection.stream.PendingMessages(groupName, messages);
    };

    private static final BiFunction<PendingMessages, String, PendingMessagesSummary> PENDING_MESSAGES_SUMMARY_CONVERTER = (source, groupName) -> {
        org.clever.data.domain.Range<String> range = source.getMessageIds().isUnbounded() ?
                org.clever.data.domain.Range.unbounded() :
                org.clever.data.domain.Range.open(source.getMessageIds().getLower().getValue(), source.getMessageIds().getUpper().getValue());
        return new PendingMessagesSummary(groupName, source.getCount(), range, source.getConsumerMessageCount());
    };

    /**
     * 转换 {@link StreamReadOptions} 到 Lettuce {@link XReadArgs}
     *
     * @param readOptions 不得为 {@literal null}
     * @return 转换后的 {@link XReadArgs}
     */
    static XReadArgs toReadArgs(StreamReadOptions readOptions) {
        return StreamReadOptionsToXReadArgsConverter.INSTANCE.convert(readOptions);
    }

    /**
     * Convert {@link XClaimOptions} to Lettuce's {@link XClaimArgs}.
     *
     * @param options 不得为 {@literal null}
     * @return 转换后的 {@link XClaimArgs}
     */
    static XClaimArgs toXClaimArgs(XClaimOptions options) {
        return XClaimOptionsToXClaimArgsConverter.INSTANCE.convert(options);
    }

    static Converter<StreamMessage<byte[], byte[]>, ByteRecord> byteRecordConverter() {
        return (it) -> StreamRecords.newRecord().in(it.getStream()).withId(it.getId()).ofBytes(it.getBody());
    }

    static Converter<List<StreamMessage<byte[], byte[]>>, List<ByteRecord>> byteRecordListConverter() {
        return new ListConverter<>(byteRecordConverter());
    }

    static Converter<StreamMessage<byte[], byte[]>, RecordId> messageToIdConverter() {
        return (it) -> RecordId.of(it.getId());
    }

    static Converter<List<StreamMessage<byte[], byte[]>>, List<RecordId>> messagesToIds() {
        return MESSAGEs_TO_IDs;
    }

    /**
     * 将原始Lettuce xpending 结果转换为 {@link PendingMessages}
     *
     * @param groupName 组名称
     * @param range     请求的消息范围
     * @param source    lettuce原始的response
     */
    static org.clever.data.redis.connection.stream.PendingMessages toPendingMessages(String groupName,
                                                                                     org.clever.data.domain.Range<?> range,
                                                                                     List<PendingMessage> source) {
        return PENDING_MESSAGES_CONVERTER.apply(source, groupName).withinRange(range);
    }

    /**
     * 将原始Lettuce xpending 结果转换为 {@link PendingMessagesSummary}
     *
     * @param source lettuce原始的response
     */
    static PendingMessagesSummary toPendingMessagesInfo(String groupName, PendingMessages source) {
        return PENDING_MESSAGES_SUMMARY_CONVERTER.apply(source, groupName);
    }

    /**
     * 我们需要将值转换为正确的目标类型，因为Lettuce会给我们 {@link ByteBuffer} 或数组，但解析器要求我们将它们作为 {@link String} 或数值。
     * 哦，{@literal null} 值也不是真正的好公民，所以我们将它们设为空字符串 - 看到它有效 - 不知何故
     *
     * @param value 不要让我开始这个
     * @return Lettuce解析器能够理解的预转换值
     */
    @SuppressWarnings("rawtypes")
    private static Object preConvertNativeValues(Object value) {
        if (value instanceof ByteBuffer || value instanceof byte[]) {
            byte[] targetArray = value instanceof ByteBuffer ? ByteUtils.getBytes((ByteBuffer) value) : (byte[]) value;
            String tmp = LettuceConverters.toString(targetArray);
            try {
                return NumberUtils.parseNumber(tmp, Long.class);
            } catch (NumberFormatException e) {
                return tmp;
            }
        }
        if (value instanceof List) {
            List<Object> targetList = new ArrayList<>();
            for (Object it : (List) value) {
                targetList.add(preConvertNativeValues(it));
            }
            return targetList;
        }
        return value != null ? value : "";
    }

    /**
     * {@link Converter} 要转换 {@link StreamReadOptions} 到 Lettuce {@link XReadArgs}.
     */
    enum StreamReadOptionsToXReadArgsConverter implements Converter<StreamReadOptions, XReadArgs> {
        INSTANCE;

        @Override
        public XReadArgs convert(StreamReadOptions source) {
            XReadArgs args = new XReadArgs();
            if (source.isNoack()) {
                args.noack(true);
            }
            if (source.getBlock() != null) {
                args.block(source.getBlock());
            }
            if (source.getCount() != null) {
                args.count(source.getCount());
            }
            return args;
        }
    }

    /**
     * {@link Converter} 要转换 {@link XClaimOptions} 到 Lettuce {@link XClaimArgs}
     */
    enum XClaimOptionsToXClaimArgsConverter implements Converter<XClaimOptions, XClaimArgs> {
        INSTANCE;

        @Override
        public XClaimArgs convert(XClaimOptions source) {
            XClaimArgs args = XClaimArgs.Builder.minIdleTime(source.getMinIdleTime());
            args.minIdleTime(source.getMinIdleTime());
            args.force(source.isForce());
            if (source.getIdleTime() != null) {
                args.idle(source.getIdleTime());
            }
            if (source.getRetryCount() != null) {
                args.retryCount(source.getRetryCount());
            }
            if (source.getUnixTime() != null) {
                args.time(source.getUnixTime());
            }
            return args;
        }
    }
}
