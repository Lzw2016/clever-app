package org.clever.boot.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;

import java.time.Period;
import java.util.Collections;
import java.util.Set;

/**
 * {@link Converter} 把 {@link Number} 转换成 {@link Period}. 支持 {@link Period#parse(CharSequence)} 以及更具可读性的 {@code 10m}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:50 <br/>
 *
 * @see PeriodFormat
 * @see PeriodUnit
 */
final class NumberToPeriodConverter implements GenericConverter {
    private final StringToPeriodConverter delegate = new StringToPeriodConverter();

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Number.class, Period.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.delegate.convert((source != null) ? source.toString() : null, TypeDescriptor.valueOf(String.class), targetType);
    }
}
