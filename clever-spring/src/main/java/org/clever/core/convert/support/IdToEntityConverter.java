package org.clever.core.convert.support;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;

/**
 * 通过对目标实体类型调用静态查找器方法，将实体标识符转换为实体引用。
 * 为了匹配此转换器，finder方法必须是静态的，具有签名{@code find[EntityName]([IdType])}，并返回所需实体类型的实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:40 <br/>
 */
final class IdToEntityConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public IdToEntityConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        Method finder = getFinder(targetType.getType());
        return (finder != null && this.conversionService.canConvert(sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0])));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        Method finder = getFinder(targetType.getType());
        Assert.state(finder != null, "No finder method");
        Object id = this.conversionService.convert(source, sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0]));
        return ReflectionUtils.invokeMethod(finder, source, id);
    }

    private Method getFinder(Class<?> entityClass) {
        String finderMethod = "find" + getEntityName(entityClass);
        Method[] methods;
        boolean localOnlyFiltered;
        try {
            methods = entityClass.getDeclaredMethods();
            localOnlyFiltered = true;
        } catch (SecurityException ex) {
            // Not allowed to access non-public methods...
            // Fallback: check locally declared public methods only.
            methods = entityClass.getMethods();
            localOnlyFiltered = false;
        }
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.getName().equals(finderMethod)
                    && method.getParameterCount() == 1
                    && method.getReturnType().equals(entityClass)
                    && (localOnlyFiltered
                    || method.getDeclaringClass().equals(entityClass))) {
                return method;
            }
        }
        return null;
    }

    private String getEntityName(Class<?> entityClass) {
        String shortName = ClassUtils.getShortName(entityClass);
        int lastDot = shortName.lastIndexOf('.');
        if (lastDot != -1) {
            return shortName.substring(lastDot + 1);
        } else {
            return shortName;
        }
    }
}
