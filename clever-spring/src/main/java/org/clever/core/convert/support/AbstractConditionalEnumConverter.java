package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalConverter;
import org.clever.util.ClassUtils;

/**
 * 基于枚举的转换器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:09 <br/>
 */
abstract class AbstractConditionalEnumConverter implements ConditionalConverter {
    private final ConversionService conversionService;

    protected AbstractConditionalEnumConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClassAsSet(sourceType.getType())) {
            if (this.conversionService.canConvert(TypeDescriptor.valueOf(interfaceType), targetType)) {
                return false;
            }
        }
        return true;
    }
}
