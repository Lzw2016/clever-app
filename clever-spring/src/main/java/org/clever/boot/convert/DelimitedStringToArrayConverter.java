package org.clever.boot.convert;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * 转换一个 {@link Delimiter delimited} String 到 Array.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:00 <br/>
 */
final class DelimitedStringToArrayConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    DelimitedStringToArrayConverter(ConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Object[].class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return targetType.getElementTypeDescriptor() == null || this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor());
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        return convert((String) source, sourceType, targetType);
    }

    @SuppressWarnings("DuplicatedCode")
    private Object convert(String source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        Delimiter delimiter = targetType.getAnnotation(Delimiter.class);
        String[] elements = getElements(source, (delimiter != null) ? delimiter.value() : ",");
        TypeDescriptor elementDescriptor = targetType.getElementTypeDescriptor();
        Object target = Array.newInstance(elementDescriptor.getType(), elements.length);
        for (int i = 0; i < elements.length; i++) {
            String sourceElement = elements[i];
            Object targetElement = this.conversionService.convert(sourceElement.trim(), sourceType, elementDescriptor);
            Array.set(target, i, targetElement);
        }
        return target;
    }

    private String[] getElements(String source, String delimiter) {
        return StringUtils.delimitedListToStringArray(source, Delimiter.NONE.equals(delimiter) ? null : delimiter);
    }
}
