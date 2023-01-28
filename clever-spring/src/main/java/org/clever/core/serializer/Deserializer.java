package org.clever.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 用于将对象流式传输到OutputStream的策略接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:36 <br/>
 *
 * @param <T> the object type
 * @see Serializer
 */
@FunctionalInterface
public interface Deserializer<T> {
    /**
     * 从给定的InputStream中读取（组装）T类型的对象。
     * <p>注意：实现不应该关闭给定的InputStream（或该InputStream的任何修饰器），而是将其留给调用者
     *
     * @param inputStream 输入流
     * @return 反序列化对象
     * @throws IOException 如果从流中读取错误
     */
    T deserialize(InputStream inputStream) throws IOException;

    /**
     * 从给定的字节数组中读取（组装）T类型的对象
     *
     * @param serialized 字节数组
     * @return 反序列化对象
     * @throws IOException 如果反序列化失败
     */
    default T deserializeFromByteArray(byte[] serialized) throws IOException {
        return deserialize(new ByteArrayInputStream(serialized));
    }
}
