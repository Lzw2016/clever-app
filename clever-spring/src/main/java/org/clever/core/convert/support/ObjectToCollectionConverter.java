package org.clever.core.convert.support;

import org.clever.core.CollectionFactory;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 将对象转换为包含单个该对象的集合
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:11 <br/>
 */
final class ObjectToCollectionConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public ObjectToCollectionConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, Collection.class));
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
        TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
        Collection<Object> target = CollectionFactory.createCollection(targetType.getType(), (elementDesc != null ? elementDesc.getType() : null), 1);
        if (elementDesc == null || elementDesc.isCollection()) {
            target.add(source);
        } else {
            Object singleElement = this.conversionService.convert(source, sourceType, elementDesc);
            target.add(singleElement);
        }
        return target;
    }
}
