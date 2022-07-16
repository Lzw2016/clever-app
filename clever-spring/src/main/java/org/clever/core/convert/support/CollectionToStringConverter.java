package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 将集合转换为逗号分隔的字符串
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:01 <br/>
 */
final class CollectionToStringConverter implements ConditionalGenericConverter {
    private static final String DELIMITER = ",";
    private final ConversionService conversionService;

    public CollectionToStringConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
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
        Collection<?> sourceCollection = (Collection<?>) source;
        if (sourceCollection.isEmpty()) {
            return "";
        }
        StringJoiner sj = new StringJoiner(DELIMITER);
        for (Object sourceElement : sourceCollection) {
            Object targetElement = this.conversionService.convert(sourceElement, sourceType.elementTypeDescriptor(sourceElement), targetType);
            sj.add(String.valueOf(targetElement));
        }
        return sj.toString();
    }
}
