package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * 将数组转换为对象，通过在将数组转换为所需的目标类型后返回第一个数组元素实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:07 <br/>
 */
final class ArrayToObjectConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public ArrayToObjectConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object[].class, Object.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        if (sourceType.isAssignableTo(targetType)) {
            return source;
        }
        if (Array.getLength(source) == 0) {
            return null;
        }
        Object firstElement = Array.get(source, 0);
        return this.conversionService.convert(firstElement, sourceType.elementTypeDescriptor(firstElement), targetType);
    }
}

