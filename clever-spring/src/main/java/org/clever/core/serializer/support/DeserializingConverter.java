package org.clever.core.serializer.support;

import org.clever.core.convert.converter.Converter;
import org.clever.core.serializer.DefaultDeserializer;
import org.clever.core.serializer.Deserializer;
import org.clever.util.Assert;

import java.io.ByteArrayInputStream;

/**
 * 委托给 {@link Deserializer} 将字节数组中的数据转换为对象的 {@link Converter}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:49 <br/>
 */
public class DeserializingConverter implements Converter<byte[], Object> {
    private final Deserializer<Object> deserializer;

    /**
     * 使用“latest user-defined ClassLoader”，使用默认的 {@link java.io.ObjectInputStream} 配置创建 {@code DeserializingConverter}
     *
     * @see DefaultDeserializer#DefaultDeserializer()
     */
    public DeserializingConverter() {
        this.deserializer = new DefaultDeserializer();
    }

    /**
     * 创建一个 {@code DeserializingConverter}，用于使用带有给定 {@code ClassLoader} 的 {@link java.io.ObjectInputStream}
     *
     * @see DefaultDeserializer#DefaultDeserializer(ClassLoader)
     */
    public DeserializingConverter(ClassLoader classLoader) {
        this.deserializer = new DefaultDeserializer(classLoader);
    }

    /**
     * 创建一个委托给所提供的 {@link Deserializer} 的 {@code DeserializingConverter}
     */
    public DeserializingConverter(Deserializer<Object> deserializer) {
        Assert.notNull(deserializer, "Deserializer must not be null");
        this.deserializer = deserializer;
    }

    @Override
    public Object convert(byte[] source) {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
        try {
            return this.deserializer.deserialize(byteStream);
        } catch (Throwable ex) {
            throw new SerializationFailedException("Failed to deserialize payload. " + "Is the byte array a result of corresponding serialization for " + this.deserializer.getClass().getSimpleName() + "?", ex);
        }
    }
}
