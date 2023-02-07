package org.clever.data.redis.serializer;

import org.clever.util.Assert;

import java.nio.ByteBuffer;

/**
 * 指定反序列化器的策略接口，该反序列化器可以将存储在 Redis 中的二进制元素表示反序列化为对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:59 <br/>
 */
@FunctionalInterface
public interface RedisElementReader<T> {
    /**
     * 将 {@link ByteBuffer} 反序列化为相应的类型
     *
     * @param buffer 不得为 {@literal null}
     * @return 反序列化的值。 可以是 {@literal null}。
     */
    T read(ByteBuffer buffer);

    /**
     * 使用给定的 {@link RedisSerializer} 创建新的 {@link RedisElementReader}
     *
     * @param serializer 不得为 {@literal null}
     * @return {@link RedisElementReader} 的新实例
     */
    static <T> RedisElementReader<T> from(RedisSerializer<T> serializer) {
        Assert.notNull(serializer, "Serializer must not be null!");
        return new DefaultRedisElementReader<>(serializer);
    }
}
