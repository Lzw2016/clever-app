package org.clever.data.redis.serializer;

import org.clever.util.ClassUtils;

/**
 * 对象到字节数组（二进制数据）的基本接口序列化和反序列化。
 * 建议将实现设计为在序列化和反序列化端处理空对象语义数组。
 * 注意，Redis不接受空键或值，但可以返回空回复（对于不存在的键）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:59 <br/>
 */
public interface RedisSerializer<T> {
    /**
     * 将给定对象序列化为二进制数据
     *
     * @param t 要序列化的对象。可以是 {@literal null}
     * @return 等效二进制数据。可以是 {@literal null}
     */
    byte[] serialize(T t) throws SerializationException;

    /**
     * 从给定的二进制数据反序列化对象
     *
     * @param bytes 对象二进制表示。可以是 {@literal null}
     * @return 等效对象实例。可以是 {@literal null}
     */
    T deserialize(byte[] bytes) throws SerializationException;

    /**
     * 使用java序列化获取 {@link RedisSerializer} <br />
     * <strong>注意:</strong> 确保域对象实际上是 {@link java.io.Serializable Serializable}
     *
     * @return 从不 {@literal null}
     */
    static RedisSerializer<Object> java() {
        return java(null);
    }

    /**
     * 使用给定的 {@link ClassLoader} 使用java序列化获取 {@link RedisSerializer} <br />
     * <strong>注意:</strong> 确保域对象实际上是 {@link java.io.Serializable Serializable}
     *
     * @param classLoader 用于反序列化的 {@link ClassLoader} 。可以是 {@literal null}
     * @return {@link RedisSerializer} 的新实例。 从不 {@literal null}
     */
    static RedisSerializer<Object> java(ClassLoader classLoader) {
        return new JdkSerializationRedisSerializer(classLoader);
    }

    /**
     * 获取一个 {@link RedisSerializer} ，它可以使用 <a href="https://github.com/FasterXML/jackson-core">Jackson</a>.
     *
     * @return 从不 {@literal null}
     */
    static RedisSerializer<Object> json() {
        return new GenericJackson2JsonRedisSerializer();
    }

    /**
     * 获得一个简单的 {@link java.lang.String} 到 {@literal byte[]}
     * 序列化程序使用 {@link java.nio.charset.StandardCharsets#UTF_8 UTF-8} 作为默认值 {@link java.nio.charset.Charset}.
     *
     * @return 从不 {@literal null}
     */
    static RedisSerializer<String> string() {
        return StringRedisSerializer.UTF_8;
    }

    /**
     * 获取通过 {@code byte[]} 的 {@link RedisSerializer}
     *
     * @return 从不 {@literal null}
     */
    static RedisSerializer<byte[]> byteArray() {
        return ByteArrayRedisSerializer.INSTANCE;
    }

    default boolean canSerialize(Class<?> type) {
        return ClassUtils.isAssignable(getTargetType(), type);
    }

    default Class<?> getTargetType() {
        return Object.class;
    }
}
