package org.clever.core.serializer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * 使用Java序列化将对象写入输出流的 {@link Serializer} 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 16:34 <br/>
 */
public class DefaultSerializer implements Serializer<Object> {
    /**
     * 使用Java序列化将源对象写入输出流。
     * 源对象必须实现 {@link Serializable}
     *
     * @see ObjectOutputStream#writeObject(Object)
     */
    @Override
    public void serialize(Object object, OutputStream outputStream) throws IOException {
        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " requires a Serializable payload " + "but received an object of type [" + object.getClass().getName() + "]");
        }
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
    }
}
