package org.clever.data.redis.connection.stream;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 表示流Id及其偏移量的值对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:29 <br/>
 */
public final class StreamOffset<K> {
    private final K key;
    private final ReadOffset offset;

    private StreamOffset(K key, ReadOffset offset) {
        Assert.notNull(key, "Key must not be null");
        Assert.notNull(offset, "ReadOffset must not be null");
        this.key = key;
        this.offset = offset;
    }

    /**
     * 给定 {@code key} 和 {@link ReadOffset} 创建一个 {@link StreamOffset}
     *
     * @param stream     stream key
     * @param readOffset 要使用的 {@link ReadOffset}
     * @return {@link StreamOffset} 的新实例
     */
    public static <K> StreamOffset<K> create(K stream, ReadOffset readOffset) {
        return new StreamOffset<>(stream, readOffset);
    }

    /**
     * 从 {@link ReadOffset#latest()} 开始给定 {@code key} 创建一个 {@link StreamOffset}
     *
     * @param stream stream key
     * @return {@link StreamOffset} 的新实例
     */
    public static <K> StreamOffset<K> latest(K stream) {
        return new StreamOffset<>(stream, ReadOffset.latest());
    }

    /**
     * 从 {@link ReadOffset#from(String) ReadOffsetfrom("0-0")} 开始给定 {@code stream} 创建一个 {@link StreamOffset}。
     *
     * @param stream stream key
     * @return {@link StreamOffset} 的新实例
     */
    public static <K> StreamOffset<K> fromStart(K stream) {
        return new StreamOffset<>(stream, ReadOffset.from("0-0"));
    }

    /**
     * 从给定的 {@link Record#getId() 记录 ID} 创建一个 {@link StreamOffset} 作为创建 {@link ReadOffset#from(String)} 的参考
     *
     * @param reference 用作参考点的记录
     * @return {@link StreamOffset} 的新实例
     */
    public static <K> StreamOffset<K> from(Record<K, ?> reference) {
        Assert.notNull(reference, "Reference record must not be null");
        return create(reference.getStream(), ReadOffset.from(reference.getId()));
    }

    public K getKey() {
        return this.key;
    }

    public ReadOffset getOffset() {
        return this.offset;
    }

    @Override
    public String toString() {
        return "StreamOffset{" + "key=" + key + ", offset=" + offset + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StreamOffset<?> that = (StreamOffset<?>) o;
        if (!ObjectUtils.nullSafeEquals(key, that.key)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(offset, that.offset);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(key);
        result = 31 * result + ObjectUtils.nullSafeHashCode(offset);
        return result;
    }
}
