package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 将对象转换为{@code java.util.Optional<T>}(如有必要)，使用ConversionService将源对象转换为通用类型的Optional(如已知)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:43 <br/>
 */
final class ObjectToOptionalConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public ObjectToOptionalConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        Set<ConvertiblePair> convertibleTypes = new LinkedHashSet<>(4);
        convertibleTypes.add(new ConvertiblePair(Collection.class, Optional.class));
        convertibleTypes.add(new ConvertiblePair(Object[].class, Optional.class));
        convertibleTypes.add(new ConvertiblePair(Object.class, Optional.class));
        return convertibleTypes;
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType.getResolvableType().hasGenerics()) {
            return this.conversionService.canConvert(sourceType, new GenericTypeDescriptor(targetType));
        } else {
            return true;
        }
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return Optional.empty();
        } else if (source instanceof Optional) {
            return source;
        } else if (targetType.getResolvableType().hasGenerics()) {
            Object target = this.conversionService.convert(source, sourceType, new GenericTypeDescriptor(targetType));
            if (target == null
                    || (target.getClass().isArray() && Array.getLength(target) == 0)
                    || (target instanceof Collection && ((Collection<?>) target).isEmpty())) {
                return Optional.empty();
            }
            return Optional.of(target);
        } else {
            return Optional.of(source);
        }
    }

    private static class GenericTypeDescriptor extends TypeDescriptor {
        public GenericTypeDescriptor(TypeDescriptor typeDescriptor) {
            super(typeDescriptor.getResolvableType().getGeneric(), null, typeDescriptor.getAnnotations());
        }
    }
}
