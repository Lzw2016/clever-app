package org.clever.core.convert.support;

import org.clever.core.convert.ConversionFailedException;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.GenericConverter;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;

/**
 * 转换包的内部实用程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:10 <br/>
 */
abstract class ConversionUtils {
    /**
     * 执行对象类型转换，包装异常类型
     *
     * @param converter  转换器
     * @param source     源对象
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     */
    public static Object invokeConverter(GenericConverter converter, Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        try {
            return converter.convert(source, sourceType, targetType);
        } catch (ConversionFailedException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new ConversionFailedException(sourceType, targetType, source, ex);
        }
    }

    public static boolean canConvertElements(TypeDescriptor sourceElementType, TypeDescriptor targetElementType, ConversionService conversionService) {
        if (targetElementType == null) {
            // yes
            return true;
        }
        if (sourceElementType == null) {
            // maybe
            return true;
        }
        if (conversionService.canConvert(sourceElementType, targetElementType)) {
            // yes
            return true;
        }
        // noinspection RedundantIfStatement
        if (ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType())) {
            // maybe
            return true;
        }
        // no
        return false;
    }

    public static Class<?> getEnumType(Class<?> targetType) {
        Assert.notNull(targetType, "target type must not be null");
        Class<?> enumType = targetType;
        while (enumType != null && !enumType.isEnum()) {
            enumType = enumType.getSuperclass();
        }
        Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
        return enumType;
    }
}
