package org.clever.data.redis.connection.stream;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 表示流的读取偏移量的值对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:10 <br/>
 */
public final class ReadOffset {
    private final String offset;

    private ReadOffset(String offset) {
        this.offset = offset;
    }

    /**
     * 从最新的偏移量读取
     */
    public static ReadOffset latest() {
        return new ReadOffset("$");
    }

    /**
     * 读取所有新到达的元素，其 id 大于消费者组消费的最后一个元素
     *
     * @return 没有特定偏移量的 {@link ReadOffset} 对象
     */
    public static ReadOffset lastConsumed() {
        return new ReadOffset(">");
    }

    /**
     * 从 {@code offset} 开始从流中读取所有到达的元素
     *
     * @param offset 流偏移量
     * @return {@link ReadOffset} 从 {@code offset} 开始
     */
    public static ReadOffset from(String offset) {
        Assert.hasText(offset, "Offset must not be empty");
        return new ReadOffset(offset);
    }

    /**
     * 从 {@link Record Id} 开始从流中读取所有到达的元素。使用 {@link RecordId#shouldBeAutoGenerated() 自动生成} {@link RecordId} 返回 {@link #latest()} 读取偏移量
     *
     * @param offset 流偏移量
     * @return {@link ReadOffset} 从 {@link RecordId} 开始
     */
    public static ReadOffset from(RecordId offset) {
        if (offset.shouldBeAutoGenerated()) {
            return latest();
        }
        return from(offset.getValue());
    }

    public String getOffset() {
        return this.offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReadOffset that = (ReadOffset) o;
        return ObjectUtils.nullSafeEquals(offset, that.offset);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(offset);
    }

    public String toString() {
        return "ReadOffset(offset=" + this.getOffset() + ")";
    }
}