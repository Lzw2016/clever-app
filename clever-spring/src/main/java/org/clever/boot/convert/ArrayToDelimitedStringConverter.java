package org.clever.boot.convert;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 转换 array 到 delimited String.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:59 <br/>
 */
final class ArrayToDelimitedStringConverter implements ConditionalGenericConverter {
    private final CollectionToDelimitedStringConverter delegate;

    ArrayToDelimitedStringConverter(ConversionService conversionService) {
        this.delegate = new CollectionToDelimitedStringConverter(conversionService);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object[].class, String.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.delegate.matches(sourceType, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        List<Object> list = Arrays.asList(ObjectUtils.toObjectArray(source));
        return this.delegate.convert(list, sourceType, targetType);
    }
}
