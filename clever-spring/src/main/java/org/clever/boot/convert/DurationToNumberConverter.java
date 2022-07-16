package org.clever.boot.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.util.ReflectionUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

/**
 * {@link Converter} 把 {@link Duration} 转换成 {@link Number}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:46 <br/>
 *
 * @see DurationFormat
 * @see DurationUnit
 */
final class DurationToNumberConverter implements GenericConverter {
    @Override
    public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Duration.class, Number.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        return convert((Duration) source, getDurationUnit(sourceType), targetType.getObjectType());
    }

    private ChronoUnit getDurationUnit(TypeDescriptor sourceType) {
        DurationUnit annotation = sourceType.getAnnotation(DurationUnit.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private Object convert(Duration source, ChronoUnit unit, Class<?> type) {
        try {
            return type.getConstructor(String.class).newInstance(String.valueOf(DurationStyle.Unit.fromChronoUnit(unit).longValue(source)));
        } catch (Exception ex) {
            ReflectionUtils.rethrowRuntimeException(ex);
            throw new IllegalStateException(ex);
        }
    }
}
