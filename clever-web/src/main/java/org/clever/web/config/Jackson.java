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
     * Date format string or a fully-qualified date format class name. For instance,
     * 'yyyy-MM-dd HH:mm:ss'.
     */
    private String dateFormat;

    /**
     * One of the constants on Jackson's PropertyNamingStrategy. Can also be a
     * fully-qualified class name of a PropertyNamingStrategy subclass.
     */
    private String propertyNamingStrategy;

    /**
     * Jackson visibility thresholds that can be used to limit which methods (and fields)
     * are auto-detected.
     */
    private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility = new EnumMap<>(PropertyAccessor.class);

    /**
     * Jackson on/off features that affect the way Java objects are serialized.
     */
    private final Map<SerializationFeature, Boolean> serialization = new EnumMap<>(SerializationFeature.class);

    /**
     * Jackson on/off features that affect the way Java objects are deserialized.
     */
    private final Map<DeserializationFeature, Boolean> deserialization = new EnumMap<>(DeserializationFeature.class);

    /**
     * Jackson general purpose on/off features.
     */
    private final Map<MapperFeature, Boolean> mapper = new EnumMap<>(MapperFeature.class);

    /**
     * Jackson on/off features for parsers.
     */
    private final Map<JsonParser.Feature, Boolean> parser = new EnumMap<>(JsonParser.Feature.class);

    /**
     * Jackson on/off features for generators.
     */
    private final Map<JsonGenerator.Feature, Boolean> generator = new EnumMap<>(JsonGenerator.Feature.class);

    /**
     * Controls the inclusion of properties during serialization. Configured with one of
     * the values in Jackson's JsonInclude.Include enumeration.
     */
    private JsonInclude.Include defaultPropertyInclusion;

    /**
     * Time zone used when formatting dates. For instance, "America/Los_Angeles" or
     * "GMT+10".
     */
    private TimeZone timeZone = null;

    /**
     * Locale used for formatting.
     */
    private Locale locale;
}
