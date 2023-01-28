package org.clever.data.redis.serializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.clever.cache.support.NullValue;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.io.IOException;

/**
 * 基于通用 Jackson 2 的 {@link RedisSerializer}，使用动态类型将 {@link Object objects} 映射到 JSON
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 17:00 <br/>
 */
public class GenericJackson2JsonRedisSerializer implements RedisSerializer<Object> {
    private final ObjectMapper mapper;

    /**
     * 创建 {@link GenericJackson2JsonRedisSerializer} 并为默认类型配置 {@link ObjectMapper}
     */
    public GenericJackson2JsonRedisSerializer() {
        this((String) null);
    }

    /**
     * 创建 {@link GenericJackson2JsonRedisSerializer} 并使用给定的 {@literal name} 配置 {@link ObjectMapper} 以进行默认输入。<br/>
     * 在 {@literal empty} 或 {@literal null} 字符串的情况下，将使用默认的 {@link JsonTypeInfo.Id#CLASS}。
     *
     * @param classPropertyTypeName 持有类型信息的 JSON 属性的名称。可以是 {@literal null}
     * @see ObjectMapper#activateDefaultTypingAsProperty(PolymorphicTypeValidator, DefaultTyping, String)
     * @see ObjectMapper#activateDefaultTyping(PolymorphicTypeValidator, DefaultTyping, As)
     */
    public GenericJackson2JsonRedisSerializer(String classPropertyTypeName) {
        this(new ObjectMapper());
        // 简单地设置 {@code mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)} 在这里没有帮助，
        // 因为我们需要嵌入类型提示以使用默认类型功能进行反序列化
        registerNullValueSerializer(mapper, classPropertyTypeName);
        if (StringUtils.hasText(classPropertyTypeName)) {
            mapper.activateDefaultTypingAsProperty(
                    mapper.getPolymorphicTypeValidator(),
                    DefaultTyping.NON_FINAL,
                    classPropertyTypeName
            );
        } else {
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.PROPERTY);
        }
    }

    /**
     * 设置自定义配置的 {@link ObjectMapper} 是进一步控制 JSON 序列化过程的一种方法。<br/>
     * 例如，可以配置扩展的 {@link SerializerFactory} 为特定类型提供自定义序列化程序。
     *
     * @param mapper 不得为 {@literal null}
     */
    public GenericJackson2JsonRedisSerializer(ObjectMapper mapper) {

        Assert.notNull(mapper, "ObjectMapper must not be null!");
        this.mapper = mapper;
    }

    /**
     * 使用可选的 {@code classPropertyTypeName} 在给定的 {@link ObjectMapper} 中注册 {@link NullValueSerializer}。<br/>
     * 此方法应由通过提供外部 {@link ObjectMapper} 自定义 {@link GenericJackson2JsonRedisSerializer} 的代码调用。
     *
     * @param objectMapper          要自定义的对象映射器
     * @param classPropertyTypeName 类型属性的名称。如果 {@literal null} 为空，则默认为 {@code @class}。
     */
    public static void registerNullValueSerializer(ObjectMapper objectMapper, String classPropertyTypeName) {
        // 简单地设置 {@code mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)} 在这里没有帮助，
        // 因为我们需要嵌入类型提示以使用默认类型功能进行反序列化
        objectMapper.registerModule(new SimpleModule().addSerializer(new NullValueSerializer(classPropertyTypeName)));
    }

    @Override
    public byte[] serialize(Object source) throws SerializationException {
        if (source == null) {
            return SerializationUtils.EMPTY_ARRAY;
        }
        try {
            return mapper.writeValueAsBytes(source);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Could not write JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public Object deserialize(byte[] source) throws SerializationException {
        return deserialize(source, Object.class);
    }

    /**
     * @param source 可以是 {@literal null}
     * @param type   不得为 {@literal null}
     * @return {@literal null} 表示空源
     */
    public <T> T deserialize(byte[] source, Class<T> type) throws SerializationException {
        Assert.notNull(type, "Deserialization type must not be null! Please provide Object.class to make use of Jackson2 default typing.");
        if (SerializationUtils.isEmpty(source)) {
            return null;
        }
        try {
            return mapper.readValue(source, type);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@link StdSerializer} 添加默认输入所需的类信息。这允许反序列化 {@link NullValue}
     */
    private static class NullValueSerializer extends StdSerializer<NullValue> {
        private static final long serialVersionUID = 1999052150548658808L;
        private final String classIdentifier;

        /**
         * @param classIdentifier 可以是 {@literal null} 并且默认为 {@code @class}
         */
        NullValueSerializer(String classIdentifier) {
            super(NullValue.class);
            this.classIdentifier = StringUtils.hasText(classIdentifier) ? classIdentifier : "@class";
        }

        @Override
        public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField(classIdentifier, NullValue.class.getName());
            jgen.writeEndObject();
        }
    }
}
