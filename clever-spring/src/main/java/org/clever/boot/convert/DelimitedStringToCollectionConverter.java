package org.clever.boot.convert;

import org.clever.core.CollectionFactory;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 转换一个 {@link Delimiter delimited} String 到 Collection.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:01 <br/>
 */
final class DelimitedStringToCollectionConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    DelimitedStringToCollectionConverter(ConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
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

    private Object convert(String source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        Delimiter delimiter = targetType.getAnnotation(Delimiter.class);
        String[] elements = getElements(source, (delimiter != null) ? delimiter.value() : ",");
        TypeDescriptor elementDescriptor = targetType.getElementTypeDescriptor();
        Collection<Object> target = createCollection(targetType, elementDescriptor, elements.length);
        Stream<Object> stream = Arrays.stream(elements).map(String::trim);
        if (elementDescriptor != null) {
            stream = stream.map((element) -> this.conversionService.convert(element, sourceType, elementDescriptor));
        }
        stream.forEach(target::add);
        return target;
    }

    private Collection<Object> createCollection(TypeDescriptor targetType, TypeDescriptor elementDescriptor, int length) {
        return CollectionFactory.createCollection(
                targetType.getType(),
                (elementDescriptor != null) ? elementDescriptor.getType() : null, length
        );
    }

    private String[] getElements(String source, String delimiter) {
        return StringUtils.delimitedListToStringArray(
                source,
                Delimiter.NONE.equals(delimiter) ? null : delimiter
        );
    }
}
