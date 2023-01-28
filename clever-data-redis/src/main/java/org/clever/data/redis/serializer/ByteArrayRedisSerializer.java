package org.clever.data.redis.serializer;

/**
 * 使用 {@code byte[]} 的原始 {@link RedisSerializer}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 17:05 <br/>
 */
enum ByteArrayRedisSerializer implements RedisSerializer<byte[]> {
    INSTANCE;

    @Override
    public byte[] serialize(byte[] bytes) throws SerializationException {
        return bytes;
    }

    @Override
    public byte[] deserialize(byte[] bytes) throws SerializationException {
        return bytes;
    }
}
