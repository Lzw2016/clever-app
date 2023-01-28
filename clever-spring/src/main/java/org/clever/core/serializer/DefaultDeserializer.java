package org.clever.core.serializer;

import org.clever.core.ConfigurableObjectInputStream;
import org.clever.core.NestedIOException;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * 使用Java序列化读取输入流的默认 {@link Deserializer} 实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:39 <br/>
 *
 * @see ObjectInputStream
 */
public class DefaultDeserializer implements Deserializer<Object> {
    private final ClassLoader classLoader;

    /**
     * 使用“latest user-defined ClassLoader”，使用默认的 {@link ObjectInputStream} 配置创建 {@code DefaultDeserializer}
     */
    public DefaultDeserializer() {
        this.classLoader = null;
    }

    /**
     * 创建一个 {@code DefaultDeserializer}，用于使用带有给定 {@code ClassLoader} 的 {@link ObjectInputStream}
     *
     * @see ConfigurableObjectInputStream#ConfigurableObjectInputStream(InputStream, ClassLoader)
     */
    public DefaultDeserializer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 读取提供的 {@code InputStream} 并将内容反序列化为对象。
     *
     * @see ObjectInputStream#readObject()
     */
    @Override
    public Object deserialize(InputStream inputStream) throws IOException {
        ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream, this.classLoader);
        try {
            return objectInputStream.readObject();
        } catch (ClassNotFoundException ex) {
            throw new NestedIOException("Failed to deserialize object type", ex);
        }
    }
}
