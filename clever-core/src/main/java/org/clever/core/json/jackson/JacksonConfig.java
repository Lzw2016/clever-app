package org.clever.core.json.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.clever.beans.BeanUtils;
import org.clever.beans.FatalBeanException;
import org.clever.core.tuples.TupleThree;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Jackson 序列化反序列化配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 16:25 <br/>
 */
@Data
public class JacksonConfig {
    public static final String PREFIX = "jackson";

    /**
     * 日期格式字符串或完全限定的日期格式类名称。默认: 'yyyy-MM-dd HH:mm:ss'。
     */
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    /**
     * 用于格式化的语言环境
     */
    private Locale locale = Locale.SIMPLIFIED_CHINESE;
    /**
     * 格式化日期时使用的时区。例如，“America/Los_Angeles”或“GMT+10”。
     */
    private TimeZone timeZone = TimeZone.getTimeZone("GMT+8");
    /**
     * Jackson 的 {@link com.fasterxml.jackson.databind.PropertyNamingStrategy} 中的静态成员之一之一。
     * 也可以是 PropertyNamingStrategy 子类的完全限定类名。
     */
    private String propertyNamingStrategy;
    /**
     * 控制序列化期间包含的属性。
     * 使用 Jackson 的 JsonInclude.Include 枚举中的值之一进行配置。
     */
    private JsonInclude.Include defaultPropertyInclusion;
    /**
     * Jackson 可见性阈值，可用于限制自动检测哪些方法（和字段）。
     */
    private Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility = new EnumMap<>(PropertyAccessor.class);
    /**
     * 开启/关闭 Jackson 特性会影响 Java 对象的序列化方式。
     */
    private Map<SerializationFeature, Boolean> serialization = new EnumMap<>(SerializationFeature.class);
    /**
     * 开启/关闭 Jackson 特性会影响 Java 对象的反序列化方式。
     */
    private Map<DeserializationFeature, Boolean> deserialization = new EnumMap<>(DeserializationFeature.class);
    /**
     * 开启/关闭 Jackson 的通用特性
     */
    private Map<MapperFeature, Boolean> mapper = new EnumMap<>(MapperFeature.class);
    /**
     * 开启/关闭 Jackson 的解析器特性
     */
    private Map<JsonParser.Feature, Boolean> parser = new EnumMap<>(JsonParser.Feature.class);
    /**
     * 开启/关闭 Jackson 的生成器特性
     */
    private Map<JsonGenerator.Feature, Boolean> generator = new EnumMap<>(JsonGenerator.Feature.class);

    /**
     * 应用当前配置
     */
    public void apply(ObjectMapper mapper) {
        Assert.notNull(mapper, "参数 mapper 不能为空");
        JacksonConfig jackson = this;
        // dateFormat
        if (StringUtils.isNotBlank(jackson.getDateFormat())) {
            mapper.setDateFormat(new SimpleDateFormat(jackson.getDateFormat()));
        }
        // locale
        if (jackson.getLocale() != null) {
            mapper.setLocale(jackson.getLocale());
        }
        // timeZone
        if (jackson.getTimeZone() != null) {
            mapper.setTimeZone(jackson.getTimeZone());
        }
        // propertyNamingStrategy
        String strategy = jackson.getPropertyNamingStrategy();
        if (strategy != null) {
            PropertyNamingStrategy propertyNamingStrategy;
            try {
                propertyNamingStrategy = (PropertyNamingStrategy) BeanUtils.instantiateClass(ClassUtils.forName(strategy, null));
            } catch (ClassNotFoundException ex) {
                Field field = ReflectionUtils.findField(PropertyNamingStrategy.class, strategy, PropertyNamingStrategy.class);
                Assert.notNull(field, () -> "Constant named '" + strategy + "' not found on " + PropertyNamingStrategy.class.getName());
                try {
                    propertyNamingStrategy = (PropertyNamingStrategy) field.get(null);
                } catch (Exception ex2) {
                    throw new IllegalStateException(ex2);
                }
            }
            mapper.setPropertyNamingStrategy(propertyNamingStrategy);
        }
        // defaultPropertyInclusion
        if (jackson.getDefaultPropertyInclusion() != null) {
            mapper.setDefaultPropertyInclusion(jackson.getDefaultPropertyInclusion());
        }
        // visibility
        if (jackson.getVisibility() != null) {
            jackson.getVisibility().forEach(mapper::setVisibility);
        }
        Consumer<TupleThree<ObjectMapper, Object, Boolean>> configureFeature = tuple -> {
            ObjectMapper objectMapper = tuple.getValue1();
            Object feature = tuple.getValue2();
            Boolean enabled = tuple.getValue3();
            if (feature instanceof JsonParser.Feature) {
                objectMapper.configure((JsonParser.Feature) feature, enabled);
            } else if (feature instanceof JsonGenerator.Feature) {
                objectMapper.configure((JsonGenerator.Feature) feature, enabled);
            } else if (feature instanceof SerializationFeature) {
                objectMapper.configure((SerializationFeature) feature, enabled);
            } else if (feature instanceof DeserializationFeature) {
                objectMapper.configure((DeserializationFeature) feature, enabled);
            } else if (feature instanceof MapperFeature) {
                // noinspection deprecation
                objectMapper.configure((MapperFeature) feature, enabled);
            } else {
                throw new FatalBeanException("Unknown feature class: " + feature.getClass().getName());
            }
        };
        // serialization
        if (jackson.getSerialization() != null) {
            jackson.getSerialization().forEach((feature, enabled) -> configureFeature.accept(TupleThree.creat(mapper, feature, enabled)));
        }
        // deserialization
        if (jackson.getDeserialization() != null) {
            jackson.getDeserialization().forEach((feature, enabled) -> configureFeature.accept(TupleThree.creat(mapper, feature, enabled)));
        }
        // mapper
        if (jackson.getMapper() != null) {
            jackson.getMapper().forEach((feature, enabled) -> configureFeature.accept(TupleThree.creat(mapper, feature, enabled)));
        }
        // parser
        if (jackson.getParser() != null) {
            jackson.getParser().forEach((feature, enabled) -> configureFeature.accept(TupleThree.creat(mapper, feature, enabled)));
        }
        // generator
        if (jackson.getGenerator() != null) {
            jackson.getGenerator().forEach((feature, enabled) -> configureFeature.accept(TupleThree.creat(mapper, feature, enabled)));
        }
    }
}
