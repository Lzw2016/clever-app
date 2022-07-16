package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.converter.Converter;

/**
 * 调用{@link Enum#name()}将源枚举转换为字符串。<br/>
 * 此转换器不会将枚举与可转换的接口匹配
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:08 <br/>
 */
final class EnumToStringConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, String> {
    public EnumToStringConverter(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public String convert(Enum<?> source) {
        return source.name();
    }
}
