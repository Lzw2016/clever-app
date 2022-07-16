package org.clever.boot.convert;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.util.ObjectUtils;
import org.clever.util.unit.DataSize;
import org.clever.util.unit.DataUnit;

import java.util.Collections;
import java.util.Set;

/**
 * {@link Converter}把{@link String}转换成{@link DataSize}. 支持 {@link DataSize#parse(CharSequence)}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:50 <br/>
 *
 * @see DataSizeUnit
 */
final class StringToDataSizeConverter implements GenericConverter {
    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, DataSize.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        return convert(source.toString(), getDataUnit(targetType));
    }

    private DataUnit getDataUnit(TypeDescriptor targetType) {
        DataSizeUnit annotation = targetType.getAnnotation(DataSizeUnit.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private DataSize convert(String source, DataUnit unit) {
        return DataSize.parse(source, unit);
    }
}
