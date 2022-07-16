package org.clever.core.convert.support;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;

/**
 * 只调用{@link Object#toString()}将任何受支持的对象转换为字符串<br/>
 * 支持{@link CharSequence}, {@link StringWriter}和任何具有字符串构造函数或以下静态工厂方法之一的类：{@code valueOf(String)}, {@code of(String)}, {@code from(String)}<br/>
 * 如果没有注册其他显式到字符串转换器，{@link DefaultConversionService}将其用作回退
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:42 <br/>
 */
final class FallbackObjectToStringConverter implements ConditionalGenericConverter {
    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, String.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        Class<?> sourceClass = sourceType.getObjectType();
        if (String.class == sourceClass) {
            // no conversion required
            return false;
        }
        return (CharSequence.class.isAssignableFrom(sourceClass) ||
                StringWriter.class.isAssignableFrom(sourceClass) ||
                ObjectToObjectConverter.hasConversionMethodOrConstructor(sourceClass, String.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return (source != null ? source.toString() : null);
    }
}

