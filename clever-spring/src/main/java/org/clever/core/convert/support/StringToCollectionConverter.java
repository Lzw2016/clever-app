package org.clever.core.convert.support;

import org.clever.core.CollectionFactory;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 将逗号分隔的字符串转换为集合。
 * 如果声明了目标集合元素类型，仅当元素为{@code String.class}类型才能转换
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:09 <br/>
 */
final class StringToCollectionConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public StringToCollectionConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return (targetType.getElementTypeDescriptor() == null || this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor()));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        String string = (String) source;
        String[] fields = StringUtils.commaDelimitedListToStringArray(string);
        TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
        Collection<Object> target = CollectionFactory.createCollection(targetType.getType(), (elementDesc != null ? elementDesc.getType() : null), fields.length);
        if (elementDesc == null) {
            for (String field : fields) {
                target.add(field.trim());
            }
        } else {
            for (String field : fields) {
                Object targetElement = this.conversionService.convert(field.trim(), sourceType, elementDesc);
                target.add(targetElement);
            }
        }
        return target;
    }
}
