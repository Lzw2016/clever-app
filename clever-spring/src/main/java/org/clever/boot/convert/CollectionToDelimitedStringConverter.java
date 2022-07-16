package org.clever.boot.convert;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 把 Collection 转换成 delimited String.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:59 <br/>
 */
final class CollectionToDelimitedStringConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    CollectionToDelimitedStringConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
        if (targetType == null || sourceElementType == null) {
            return true;
        }
        return this.conversionService.canConvert(sourceElementType, targetType) || sourceElementType.getType().isAssignableFrom(targetType.getType());
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        Collection<?> sourceCollection = (Collection<?>) source;
        return convert(sourceCollection, sourceType, targetType);
    }

    private Object convert(Collection<?> source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source.isEmpty()) {
            return "";
        }
        return source.stream()
                .map((element) -> convertElement(element, sourceType, targetType))
                .collect(Collectors.joining(getDelimiter(sourceType)));
    }

    private CharSequence getDelimiter(TypeDescriptor sourceType) {
        Delimiter annotation = sourceType.getAnnotation(Delimiter.class);
        return (annotation != null) ? annotation.value() : ",";
    }

    private String convertElement(Object element, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return String.valueOf(this.conversionService.convert(
                element,
                sourceType.elementTypeDescriptor(element),
                targetType
        ));
    }
}
