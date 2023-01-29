package org.clever.data.redis.connection.stream;

import org.clever.data.redis.hash.HashMapper;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.data.redis.util.ByteUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 由一组二进制 {@literal field/value} 对支持的流中的 {@link Record}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:31 <br/>
 */
public interface ByteBufferRecord extends MapRecord<ByteBuffer, ByteBuffer, ByteBuffer> {
    @Override
    ByteBufferRecord withId(RecordId id);

    /**
     * 使用关联的流 {@literal key} 创建一个新的 {@link ByteBufferRecord}
     *
     * @param key 二进制流key
     * @return 一个新的 {@link ByteBufferRecord}
     */
    ByteBufferRecord withStreamKey(ByteBuffer key);

    /**
     * 使用给定的 {@link RedisSerializer} 反序列化 {@link #getStream() key} 和 {@link #getValue() fieldvalue pairs}。
     * 已分配的 {@link RecordId id} 被转移到新实例
     *
     * @param serializer 如果 {@link Record} 只保存二进制数据，则可以是 {@literal null}
     * @return 新的 {@link MapRecord} 持有反序列化的值
     */
    default <T> MapRecord<T, T, T> deserialize(RedisSerializer<T> serializer) {
        return deserialize(serializer, serializer, serializer);
    }

    /**
     * 使用 {@literal streamSerializer} 反序列化 {@link #getStream() key}，使用 {@literal fieldSerializer} 反序列化字段名称，使用 {@literal valueSerializer} 反序列化值。已分配的 {@link RecordId id} 被转移到新实例
     *
     * @param streamSerializer 如果密钥套件已经是目标格式，则可以是 {@literal null}
     * @param fieldSerializer  如果字段套件已经是目标格式，则可以是 {@literal null}
     * @param valueSerializer  如果值套件已经是目标格式，则可以是 {@literal null}
     * @return 新的 {@link MapRecord} 持有反序列化的值
     */
    @SuppressWarnings({"unchecked"})
    default <K, HK, HV> MapRecord<K, HK, HV> deserialize(RedisSerializer<? extends K> streamSerializer,
                                                         RedisSerializer<? extends HK> fieldSerializer,
                                                         RedisSerializer<? extends HV> valueSerializer) {
        return mapEntries(it -> Collections.singletonMap(
                fieldSerializer != null ? fieldSerializer.deserialize(ByteUtils.getBytes(it.getKey())) : (HK) it.getKey(),
                valueSerializer != null ? valueSerializer.deserialize(ByteUtils.getBytes(it.getValue())) : (HV) it.getValue()
        ).entrySet().iterator().next()).withStreamKey(
                streamSerializer != null ? streamSerializer.deserialize(ByteUtils.getBytes(getStream())) : (K) getStream()
        );
    }

    /**
     * 将二进制 {@link MapRecord} 转换为 {@link ByteRecord}
     *
     * @param source 不得为 {@literal null}
     * @return {@link ByteRecord} 的新实例
     */
    static ByteBufferRecord of(MapRecord<ByteBuffer, ByteBuffer, ByteBuffer> source) {
        return StreamRecords.newRecord().in(source.getStream()).withId(source.getId()).ofBuffer(source.getValue());
    }

    /**
     * 将二进制 {@link MapRecord} 转换为 {@link ObjectRecord}
     *
     * @param mapper 不得为 {@literal null}
     * @return {@link ByteRecord} 的新实例
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    default <OV> ObjectRecord<ByteBuffer, OV> toObjectRecord(HashMapper<? super OV, ? super ByteBuffer, ? super ByteBuffer> mapper) {
        Map<byte[], byte[]> targetMap = getValue().entrySet().stream().collect(
                Collectors.toMap(entry -> ByteUtils.getBytes(entry.getKey()), entry -> ByteUtils.getBytes(entry.getValue()))
        );
        return Record.<ByteBuffer, OV>of((OV) (mapper).fromHash((Map) targetMap)).withId(getId()).withStreamKey(getStream());
    }
}
