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
 * {@link Converter}将字符串转换为{@link Period}。支持{@link Period#parse(CharSequence)}以及更可读的表单。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:46 <br/>
 *
 * @see PeriodFormat
 * @see PeriodUnit
 */
final class StringToPeriodConverter implements GenericConverter {
    @Override
    public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Period.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        return convert(source.toString(), getStyle(targetType), getPeriodUnit(targetType));
    }

    private PeriodStyle getStyle(TypeDescriptor targetType) {
        PeriodFormat annotation = targetType.getAnnotation(PeriodFormat.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private ChronoUnit getPeriodUnit(TypeDescriptor targetType) {
        PeriodUnit annotation = targetType.getAnnotation(PeriodUnit.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private Period convert(String source, PeriodStyle style, ChronoUnit unit) {
        style = (style != null) ? style : PeriodStyle.detect(source);
        return style.parse(source, unit);
    }
}
