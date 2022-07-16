package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.Assert;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * 将对象转换为包含单个该对象的数组
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:07 <br/>
 */
final class ObjectToArrayConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public ObjectToArrayConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, Object[].class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(), this.conversionService);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
        Assert.state(targetElementType != null, "No target element type");
        Object target = Array.newInstance(targetElementType.getType(), 1);
        Object targetElement = this.conversionService.convert(source, sourceType, targetElementType);
        Array.set(target, 0, targetElement);
        return target;
    }
}
