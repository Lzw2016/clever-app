package org.clever.data.redis.connection.stream;

import org.clever.data.redis.serializer.RedisSerializer;

/**
 * 流序列化的实用程序方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:29 <br/>
 */
class StreamSerialization {
    /**
     * 使用可选的 {@link RedisSerializer} 序列化 {@code value}。如果无法进行转换，则 {@code value} 被假定为字节数组
     *
     * @param serializer 序列化器。可以是 {@literal null}
     * @param value      要序列化的值
     * @return {@code value} 的序列化（二进制）表示
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static byte[] serialize(RedisSerializer<?> serializer, Object value) {
        return canSerialize(serializer, value) ? ((RedisSerializer) serializer).serialize(value) : (byte[]) value;
    }

    /**
     * 返回给定的 {@link RedisSerializer} 是否能够将 {@code value} 序列化为 {@literal byte[]}
     *
     * @param serializer 序列化器。可以是 {@literal null}
     * @param value      要序列化的值
     * @return {@literal true} 如果给定的 {@link RedisSerializer} 能够将 {@code value} 序列化为 {@literal byte[]}
     */
    private static boolean canSerialize(RedisSerializer<?> serializer, Object value) {
        return serializer != null && (value == null || serializer.canSerialize(value.getClass()));
    }
}
