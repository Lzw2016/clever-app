package org.clever.data.redis.core.convert;

import org.clever.data.mapping.model.SimpleTypeHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用于捕获自定义转换的值对象。这本质上是转换器的 {@link List} 和围绕它们的一些附加逻辑。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:02 <br/>
 *
 * @see org.clever.data.convert.CustomConversions
 * @see SimpleTypeHolder
 */
public class RedisCustomConversions extends org.clever.data.convert.CustomConversions {
    private static final StoreConversions STORE_CONVERSIONS;
    private static final List<Object> STORE_CONVERTERS;

    static {
        List<Object> converters = new ArrayList<>();
        converters.add(new BinaryConverters.StringToBytesConverter());
        converters.add(new BinaryConverters.BytesToStringConverter());
        converters.add(new BinaryConverters.NumberToBytesConverter());
        converters.add(new BinaryConverters.BytesToNumberConverterFactory());
        converters.add(new BinaryConverters.EnumToBytesConverter());
        converters.add(new BinaryConverters.BytesToEnumConverterFactory());
        converters.add(new BinaryConverters.BooleanToBytesConverter());
        converters.add(new BinaryConverters.BytesToBooleanConverter());
        converters.add(new BinaryConverters.DateToBytesConverter());
        converters.add(new BinaryConverters.BytesToDateConverter());
        converters.add(new BinaryConverters.UuidToBytesConverter());
        converters.add(new BinaryConverters.BytesToUuidConverter());
        converters.addAll(Jsr310Converters.getConvertersToRegister());
        STORE_CONVERTERS = Collections.unmodifiableList(converters);
        STORE_CONVERSIONS = StoreConversions.of(SimpleTypeHolder.DEFAULT, STORE_CONVERTERS);
    }

    /**
     * 创建一个空的 {@link RedisCustomConversions} 对象
     */
    public RedisCustomConversions() {
        this(Collections.emptyList());
    }

    /**
     * 创建一个新的 {@link RedisCustomConversions} 实例注册给定的转换器
     */
    public RedisCustomConversions(List<?> converters) {
        super(STORE_CONVERSIONS, converters);
    }
}
