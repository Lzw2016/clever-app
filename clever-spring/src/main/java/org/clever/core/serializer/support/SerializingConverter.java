package org.clever.core.serializer.support;

import org.clever.core.convert.converter.Converter;
import org.clever.core.serializer.DefaultSerializer;
import org.clever.core.serializer.Serializer;
import org.clever.util.Assert;

/**
 * 委托给 {@link Serializer} 将对象转换为字节数组的 {@link Converter}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:48 <br/>
 */
public class SerializingConverter implements Converter<Object, byte[]> {
    private final Serializer<Object> serializer;

    /**
     * 创建使用标准Java序列化的默认 {@code SerializationConverter}
     */
    public SerializingConverter() {
        this.serializer = new DefaultSerializer();
    }

    /**
     * 创建委托给所提供的 {@link Serializer} 的 {@code SerializeConverter}
     */
    public SerializingConverter(Serializer<Object> serializer) {
        Assert.notNull(serializer, "Serializer must not be null");
        this.serializer = serializer;
    }

    /**
     * 序列化源对象并返回字节数组结果
     */
    @Override
    public byte[] convert(Object source) {
        try {
            return this.serializer.serializeToByteArray(source);
        } catch (Throwable ex) {
            throw new SerializationFailedException("Failed to serialize object using " + this.serializer.getClass().getSimpleName(), ex);
        }
    }
}
