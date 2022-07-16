package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.converter.Converter;

/**
 * 调用{@link Enum#ordinal()}将源枚举转换为整数。<br/>
 * 此转换器不会将枚举与可转换的接口匹配
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:11 <br/>
 */
final class EnumToIntegerConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, Integer> {
    public EnumToIntegerConverter(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public Integer convert(Enum<?> source) {
        return source.ordinal();
    }
}
