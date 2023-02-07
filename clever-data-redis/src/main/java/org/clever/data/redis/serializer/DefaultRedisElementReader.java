package org.clever.data.redis.serializer;

import org.clever.data.redis.util.ByteUtils;

import java.nio.ByteBuffer;

/**
 * {@link RedisElementReader} 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:00 <br/>
 */
class DefaultRedisElementReader<T> implements RedisElementReader<T> {
    private final RedisSerializer<T> serializer;

    DefaultRedisElementReader(RedisSerializer<T> serializer) {
        this.serializer = serializer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(ByteBuffer buffer) {
        if (serializer == null) {
            return (T) buffer;
        }
        return serializer.deserialize(ByteUtils.extractBytes(buffer));
    }
}
