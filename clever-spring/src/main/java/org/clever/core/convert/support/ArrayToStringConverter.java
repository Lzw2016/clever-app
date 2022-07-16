package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.ObjectUtils;

import java.util.Collections;
import java.util.Set;

/**
 * 将数组转换为逗号分隔的字符串
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:00 <br/>
 */
final class ArrayToStringConverter implements ConditionalGenericConverter {
    private final CollectionToStringConverter helperConverter;

    public ArrayToStringConverter(ConversionService conversionService) {
        this.helperConverter = new CollectionToStringConverter(conversionService);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object[].class, String.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.helperConverter.matches(sourceType, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.helperConverter.convert(Collections.singletonList(ObjectUtils.toObjectArray(source)), sourceType, targetType);
    }
}
