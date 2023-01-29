package org.clever.data.redis.connection.stream;

import org.clever.data.redis.connection.stream.StreamRecords.ObjectBackedRecord;
import org.clever.data.redis.hash.HashMapper;
import org.clever.util.Assert;

/**
 * 映射到单个对象的流中的 {@link Record}。这可能是一个简单的类型，例如 {@link String} 或一个复杂的类型
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:30 <br/>
 *
 * @param <V> the type of the backing Object.
 */
public interface ObjectRecord<S, V> extends Record<S, V> {
    /**
     * 创建与 {@code stream} 键和 {@code value} 关联的新 {@link ObjectRecord}
     *
     * @param stream stream key
     * @param value  value
     * @return {@link ObjectRecord} 持有 {@code stream} 键和 {@code value}
     */
    static <S, V> ObjectRecord<S, V> create(S stream, V value) {
        Assert.notNull(stream, "Stream must not be null");
        Assert.notNull(value, "Value must not be null");
        return new ObjectBackedRecord<>(stream, RecordId.autoGenerate(), value);
    }

    @Override
    ObjectRecord<S, V> withId(RecordId id);

    <SK> ObjectRecord<SK, V> withStreamKey(SK key);

    /**
     * 将给定的 {@link HashMapper} 应用于支持值以创建新的 {@link MapRecord}。已分配的 {@link RecordId id} 被转移到新实例
     *
     * @param mapper 不得为 {@literal null}
     * @param <HK>   结果 {@link MapRecord} 的键类型
     * @param <HV>   结果 {@link MapRecord} 的值类型
     * @return {@link MapRecord} 的新实例
     */
    default <HK, HV> MapRecord<S, HK, HV> toMapRecord(HashMapper<? super V, HK, HV> mapper) {
        return Record.<S, HK, HV>of(mapper.toHash(getValue())).withId(getId()).withStreamKey(getStream());
    }
}
