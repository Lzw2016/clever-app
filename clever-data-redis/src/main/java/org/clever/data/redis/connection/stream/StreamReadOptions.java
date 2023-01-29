package org.clever.data.redis.connection.stream;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.time.Duration;


/**
 * 从 Redis 流中读取消息的选项
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:30 <br/>
 */
public class StreamReadOptions {
    private static final StreamReadOptions EMPTY = new StreamReadOptions(null, null, false);

    private final Long block;
    private final Long count;
    private final boolean noack;

    private StreamReadOptions(Long block, Long count, boolean noack) {
        this.block = block;
        this.count = count;
        this.noack = noack;
    }

    /**
     * 创建一个空的 {@link StreamReadOptions} 实例
     *
     * @return 一个空的 {@link StreamReadOptions} 实例
     */
    public static StreamReadOptions empty() {
        return EMPTY;
    }

    /**
     * 在消费者组的上下文中读取时，通过设置 {@code NOACK} 标志启用自动确认。
     * 出于可读性原因，此方法是 {@link #autoAcknowledge()} 的别名。
     *
     * @return {@link StreamReadOptions} 应用了 {@code noack}
     */
    public StreamReadOptions noack() {
        return autoAcknowledge();
    }

    /**
     * 在消费者组的上下文中读取时，通过设置 {@code NOACK} 标志启用自动确认。
     * 出于可读性原因，此方法是 {@link #noack()} 的别名。
     *
     * @return 应用了 {@code noack} 的 {@link StreamReadOptions} 的新实例
     */
    public StreamReadOptions autoAcknowledge() {
        return new StreamReadOptions(block, count, true);
    }

    /**
     * 使用阻塞读取并提供 {@link Duration 超时}，如果没有消息被读取，调用将在超时后终止
     *
     * @param timeout 阻塞读取的超时不能为 {@literal null} 或负数
     * @return 应用了 {@code block} 的 {@link StreamReadOptions} 的新实例
     */
    public StreamReadOptions block(Duration timeout) {
        Assert.notNull(timeout, "Block timeout must not be null!");
        Assert.isTrue(!timeout.isNegative(), "Block timeout must not be negative!");
        return new StreamReadOptions(timeout.toMillis(), count, noack);
    }

    /**
     * 限制每个流返回的消息数
     *
     * @param count 要读取的最大消息数
     * @return {@link StreamReadOptions} 应用了 {@code count}
     */
    public StreamReadOptions count(long count) {
        Assert.isTrue(count > 0, "Count must be greater or equal to zero!");
        return new StreamReadOptions(block, count, noack);
    }

    /**
     * @return {@literal true} 如果参数表示阻塞读取
     */
    public boolean isBlocking() {
        return getBlock() != null && getBlock() >= 0;
    }

    public Long getBlock() {
        return block;
    }

    public Long getCount() {
        return count;
    }

    public boolean isNoack() {
        return noack;
    }

    @Override
    public String toString() {
        return "StreamReadOptions{" + "block=" + block + ", count=" + count + ", noack=" + noack + ", blocking=" + isBlocking() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StreamReadOptions that = (StreamReadOptions) o;
        if (noack != that.noack)
            return false;
        if (!ObjectUtils.nullSafeEquals(block, that.block)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(count, that.count);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(block);
        result = 31 * result + ObjectUtils.nullSafeHashCode(count);
        result = 31 * result + (noack ? 1 : 0);
        return result;
    }
}
