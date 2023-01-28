package org.clever.core.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 用于将对象流式传输到OutputStream的策略接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:35 <br/>
 *
 * @param <T> the object type
 * @see Deserializer
 */
@FunctionalInterface
public interface Serializer<T> {
    /**
     * 将T类型的对象写入给定的OutputStream
     * <p>注意：实现不应该关闭给定的OutputStream（或该OutputStream的任何修饰器），而是将其留给调用者。
     *
     * @param object       要序列化的对象
     * @param outputStream 输出流
     * @throws IOException 写入流时出错
     */
    void serialize(T object, OutputStream outputStream) throws IOException;

    /**
     * 将T类型的对象转换为序列化字节数组。
     *
     * @param object 要序列化的对象
     * @return 生成的字节数组
     * @throws IOException 如果串行化失败
     */
    default byte[] serializeToByteArray(T object) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        serialize(object, out);
        return out.toByteArray();
    }
}
