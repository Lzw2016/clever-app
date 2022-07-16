package org.clever.boot.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * {@link Converter} 把 {@link Number} 转换成 {@link Duration}. 支持 {@link Duration#parse(CharSequence)} 以及更具可读性的 {@code 10s}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:45 <br/>
 *
 * @see DurationFormat
 * @see DurationUnit
 */
final class NumberToDurationConverter implements GenericConverter {
    private final StringToDurationConverter delegate = new StringToDurationConverter();

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Number.class, Duration.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.delegate.convert((source != null) ? source.toString() : null, TypeDescriptor.valueOf(String.class), targetType);
    }
}
