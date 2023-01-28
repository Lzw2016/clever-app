package org.clever.data.redis.serializer;

import org.clever.core.convert.converter.Converter;
import org.clever.core.serializer.DefaultDeserializer;
import org.clever.core.serializer.DefaultSerializer;
import org.clever.core.serializer.support.DeserializingConverter;
import org.clever.core.serializer.support.SerializingConverter;
import org.clever.util.Assert;

/**
 * Java 序列化 Redis 序列化程序。委托给默认的（基于 Java 的）{@link DefaultSerializer serializer} 和 {@link DefaultDeserializer}。
 * 此 {@link RedisSerializer serializer} 可以使用自定义 {@link ClassLoader} 或自己的 {@link Converter converters} 构建
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:32 <br/>
 */
public class JdkSerializationRedisSerializer implements RedisSerializer<Object> {
    private final Converter<Object, byte[]> serializer;
    private final Converter<byte[], Object> deserializer;

    /**
     * 使用默认类加载器创建一个新的 {@link JdkSerializationRedisSerializer}
     */
    public JdkSerializationRedisSerializer() {
        this(new SerializingConverter(), new DeserializingConverter());
    }

    /**
     * 使用 {@link ClassLoader} 创建一个新的 {@link JdkSerializationRedisSerializer}
     *
     * @param classLoader {@link ClassLoader} 用于反序列化。可以是 {@literal null}
     */
    public JdkSerializationRedisSerializer(ClassLoader classLoader) {
        this(new SerializingConverter(), new DeserializingConverter(classLoader));
    }

    /**
     * 使用 {@link Converter 转换器} 创建一个新的 {@link JdkSerializationRedisSerializer} 来序列化和反序列化对象
     *
     * @param serializer   不能为 {@literal null}
     * @param deserializer 不能为 {@literal null}
     */
    public JdkSerializationRedisSerializer(Converter<Object, byte[]> serializer, Converter<byte[], Object> deserializer) {
        Assert.notNull(serializer, "Serializer must not be null!");
        Assert.notNull(deserializer, "Deserializer must not be null!");
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public Object deserialize(byte[] bytes) {
        if (SerializationUtils.isEmpty(bytes)) {
            return null;
        }
        try {
            return deserializer.convert(bytes);
        } catch (Exception ex) {
            throw new SerializationException("Cannot deserialize", ex);
        }
    }

    @Override
    public byte[] serialize(Object object) {
        if (object == null) {
            return SerializationUtils.EMPTY_ARRAY;
        }
        try {
            return serializer.convert(object);
        } catch (Exception ex) {
            throw new SerializationException("Cannot serialize", ex);
        }
    }
}
