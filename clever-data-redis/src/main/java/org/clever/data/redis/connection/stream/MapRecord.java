package org.clever.data.redis.connection.stream;

import org.clever.data.redis.connection.stream.StreamRecords.MapBackedRecord;
import org.clever.data.redis.hash.HashMapper;
import org.clever.data.redis.serializer.RedisSerializer;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * 由一组 {@literal field/value} 对支持的流中的 {@link Record}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:56 <br/>
 */
public interface MapRecord<S, K, V> extends Record<S, Map<K, V>>, Iterable<Map.Entry<K, V>> {
    /**
     * 创建与 {@code stream} 键和 {@link Map value} 关联的新 {@link MapRecord}
     *
     * @param stream stream key
     * @param map    值
     * @return {@link ObjectRecord} 持有 {@code stream} 键和 {@code value}
     */
    static <S, K, V> MapRecord<S, K, V> create(S stream, Map<K, V> map) {
        Assert.notNull(stream, "Stream must not be null");
        Assert.notNull(map, "Map must not be null");
        return new MapBackedRecord<>(stream, RecordId.autoGenerate(), map);
    }

    @Override
    MapRecord<S, K, V> withId(RecordId id);

    @Override
    <SK> MapRecord<SK, K, V> withStreamKey(SK key);

    /**
     * 将给定的 {@link Function mapFunction} 应用于支持集合中的每个条目以创建新的 {@link MapRecord}
     *
     * @param mapFunction 不能是 {@literal null}
     * @param <HK>        新后备集合的字段类型
     * @param <HV>        新后备集合的值类型
     * @return {@link MapRecord} 的新实例
     */
    default <HK, HV> MapRecord<S, HK, HV> mapEntries(Function<Entry<K, V>, Entry<HK, HV>> mapFunction) {
        Map<HK, HV> mapped = new LinkedHashMap<>();
        iterator().forEachRemaining(it -> {
            Entry<HK, HV> mappedPair = mapFunction.apply(it);
            mapped.put(mappedPair.getKey(), mappedPair.getValue());
        });
        return StreamRecords.newRecord().in(getStream()).withId(getId()).ofMap(mapped);
    }

    /**
     * 通过应用映射 {@link Function} 映射此 {@link MapRecord}
     *
     * @param mapFunction 应用于此 {@link MapRecord} 元素的函数
     * @return 映射的 {@link MapRecord}
     */
    default <SK, HK, HV> MapRecord<SK, HK, HV> map(Function<MapRecord<S, K, V>, MapRecord<SK, HK, HV>> mapFunction) {
        return mapFunction.apply(this);
    }

    /**
     * 使用给定的 {@link RedisSerializer} 序列化 {@link #getStream() key} 和 {@link #getValue() fieldvalue pairs}。已分配的 {@link RecordId id} 被转移到新实例
     *
     * @param serializer 如果 {@link Record} 只保存二进制数据，则可以是 {@literal null}
     * @return 保存序列化值的新 {@link ByteRecord}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    default ByteRecord serialize(RedisSerializer<?> serializer) {
        return serialize((RedisSerializer) serializer, (RedisSerializer) serializer, (RedisSerializer) serializer);
    }

    /**
     * 使用 {@literal streamSerializer} 序列化 {@link #getStream() key}，使用 {@literal fieldSerializer} 序列化字段名称，使用 {@literal valueSerializer} 序列化值
     * 已分配的 {@link RecordId id} 被转移到新实例。
     *
     * @param streamSerializer 如果key是二进制的，则可以是 {@literal null}
     * @param fieldSerializer  如果字段是二进制的，则可以是 {@literal null}
     * @param valueSerializer  如果值是二进制的，则可以是 {@literal null}。
     * @return new {@link ByteRecord} holding the serialized values.
     */
    default ByteRecord serialize(RedisSerializer<? super S> streamSerializer,
                                 RedisSerializer<? super K> fieldSerializer,
                                 RedisSerializer<? super V> valueSerializer) {
        MapRecord<S, byte[], byte[]> binaryMap = mapEntries(
                it -> Collections.singletonMap(
                        StreamSerialization.serialize(fieldSerializer, it.getKey()),
                        StreamSerialization.serialize(valueSerializer, it.getValue())
                ).entrySet().iterator().next()
        );
        return StreamRecords.newRecord()
                .in(streamSerializer != null ? streamSerializer.serialize(getStream()) : (byte[]) getStream())
                .withId(getId()).ofBytes(binaryMap.getValue());
    }

    /**
     * 将给定的 {@link HashMapper} 应用于支持值以创建新的 {@link MapRecord}。已分配的 {@link RecordId id} 被转移到新实例。
     *
     * @param mapper 不能是 {@literal null}
     * @param <OV>   支持 {@link ObjectRecord} 的值的类型
     * @return {@link ObjectRecord} 的新实例
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    default <OV> ObjectRecord<S, OV> toObjectRecord(HashMapper<? super OV, ? super K, ? super V> mapper) {
        return Record.<S, OV>of((OV) mapper.fromHash((Map) getValue())).withId(getId()).withStreamKey(getStream());
    }
}
