package org.clever.web.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Jackson 序列化反序列化配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 16:25 <br/>
 */
@Data
public class Jackson {
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
}
