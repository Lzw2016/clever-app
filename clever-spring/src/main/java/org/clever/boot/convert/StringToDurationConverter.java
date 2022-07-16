package org.clever.boot.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.util.ObjectUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

/**
 * 将字符串转换为{@link Duration}的{@link Converter}。支持{@link Duration#parse(CharSequence)}以及更具可读性的{@code 10s}格式。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:37 <br/>
 *
 * @see DurationFormat
 * @see DurationUnit
 */
final class StringToDurationConverter implements GenericConverter {
    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Duration.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        return convert(source.toString(), getStyle(targetType), getDurationUnit(targetType));
    }

    private DurationStyle getStyle(TypeDescriptor targetType) {
        DurationFormat annotation = targetType.getAnnotation(DurationFormat.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private ChronoUnit getDurationUnit(TypeDescriptor targetType) {
        DurationUnit annotation = targetType.getAnnotation(DurationUnit.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private Duration convert(String source, DurationStyle style, ChronoUnit unit) {
        style = (style != null) ? style : DurationStyle.detect(source);
        return style.parse(source, unit);
    }
}
