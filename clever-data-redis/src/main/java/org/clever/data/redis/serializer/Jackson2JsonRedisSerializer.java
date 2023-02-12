package org.clever.data.redis.serializer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.clever.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * {@link RedisSerializer}，可以使用 {@link ObjectMapper} 读写 JSON。
 * <a href="https://github.com/FasterXML/jackson-core">Jackson's</a> 和 <a href="https://github.com/FasterXML/jackson-databind">Jackson Databind</a>
 * <p>
 * 此转换器可用于绑定到类型化 bean 或非类型化 {@link java.util.HashMap HashMap} 实例。
 * <p>
 * <b>注意:</b>空对象被序列化为空数组，反之亦然。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/12 22:57 <br/>
 */
public class Jackson2JsonRedisSerializer<T> implements RedisSerializer<T> {
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final JavaType javaType;
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 为给定目标 {@link Class} 创建一个新的 {@link Jackson2JsonRedisSerializer}
     */
    public Jackson2JsonRedisSerializer(Class<T> type) {
        this.javaType = getJavaType(type);
    }

    /**
     * 为给定目标 {@link JavaType} 创建一个新的 {@link Jackson2JsonRedisSerializer}
     */
    public Jackson2JsonRedisSerializer(JavaType javaType) {
        this.javaType = javaType;
    }

    public T deserialize(byte[] bytes) throws SerializationException {
        if (SerializationUtils.isEmpty(bytes)) {
            return null;
        }
        try {
            return this.objectMapper.readValue(bytes, 0, bytes.length, javaType);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] serialize(Object t) throws SerializationException {
        if (t == null) {
            return SerializationUtils.EMPTY_ARRAY;
        }
        try {
            return this.objectMapper.writeValueAsBytes(t);
        } catch (Exception ex) {
            throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * 设置此视图的 {@code ObjectMapper}。如果未设置，则使用默认的 {@link ObjectMapper#ObjectMapper() ObjectMapper}。
     * <p>
     * 设置自定义配置的 {@code ObjectMapper} 是进一步控制 JSON 序列化过程的一种方法。
     * 例如，可以配置扩展的 {@link SerializerFactory}，为特定类型提供自定义序列化程序。
     * 优化序列化过程的另一个选项是对要序列化的类型使用 Jackson 提供的注释，在这种情况下，不需要自定义配置的 ObjectMapper。
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "'objectMapper' must not be null");
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the Jackson {@link JavaType} for the specific class.
     * <p>
     * 默认实现返回 {@link TypeFactory#constructType(java.lang.reflect.Type)}，但这可以在子类中重写，以允许自定义泛型集合处理。例如：
     *
     * <pre>{@code
     * protected JavaType getJavaType(Class<?> clazz) {
     * 	if (List.class.isAssignableFrom(clazz)) {
     * 		return TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, MyBean.class);
     *    } else {
     * 		return super.getJavaType(clazz);
     *    }
     * }
     * }</pre>
     *
     * @param clazz 要为其返回 Java 类型的类
     * @return java type
     */
    protected JavaType getJavaType(Class<?> clazz) {
        return TypeFactory.defaultInstance().constructType(clazz);
    }
}
