package org.clever.boot.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.util.ObjectUtils;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

/**
 * {@link Converter} 把 {@link Period} 转换成 {@link String}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:49 <br/>
 *
 * @see PeriodFormat
 * @see PeriodUnit
 */
final class PeriodToStringConverter implements GenericConverter {
    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Period.class, String.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        return convert((Period) source, getPeriodStyle(sourceType), getPeriodUnit(sourceType));
    }

    private PeriodStyle getPeriodStyle(TypeDescriptor sourceType) {
        PeriodFormat annotation = sourceType.getAnnotation(PeriodFormat.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private String convert(Period source, PeriodStyle style, ChronoUnit unit) {
        style = (style != null) ? style : PeriodStyle.ISO8601;
        return style.print(source, unit);
    }

    private ChronoUnit getPeriodUnit(TypeDescriptor sourceType) {
        PeriodUnit annotation = sourceType.getAnnotation(PeriodUnit.class);
        return (annotation != null) ? annotation.value() : null;
    }
}
