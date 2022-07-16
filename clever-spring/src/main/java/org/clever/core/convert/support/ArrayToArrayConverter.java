package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 将数组转换为另一个数组
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:53 <br/>
 */
final class ArrayToArrayConverter implements ConditionalGenericConverter {
    private final CollectionToArrayConverter helperConverter;
    private final ConversionService conversionService;

    public ArrayToArrayConverter(ConversionService conversionService) {
        this.helperConverter = new CollectionToArrayConverter(conversionService);
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object[].class, Object[].class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.helperConverter.matches(sourceType, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (this.conversionService instanceof GenericConversionService) {
            TypeDescriptor targetElement = targetType.getElementTypeDescriptor();
            if (targetElement != null
                    && ((GenericConversionService) this.conversionService).canBypassConvert(sourceType.getElementTypeDescriptor(), targetElement)) {
                return source;
            }
        }
        List<Object> sourceList = Arrays.asList(ObjectUtils.toObjectArray(source));
        return this.helperConverter.convert(sourceList, sourceType, targetType);
    }
}
