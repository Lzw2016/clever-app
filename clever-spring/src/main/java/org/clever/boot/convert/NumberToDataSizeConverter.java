package org.clever.boot.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.util.unit.DataSize;

import java.util.Collections;
import java.util.Set;

/**
 * {@link Converter} 把 {@link Number} 转换成 {@link DataSize}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:53 <br/>
 *
 * @see DataSizeUnit
 */
final class NumberToDataSizeConverter implements GenericConverter {
    private final StringToDataSizeConverter delegate = new StringToDataSizeConverter();

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Number.class, DataSize.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.delegate.convert((source != null) ? source.toString() : null, TypeDescriptor.valueOf(String.class), targetType);
    }
}
