package org.clever.core.converter;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;

/**
 * 作者：lizw <br/>
 * 创建时间：2019/12/19 15:31 <br/>
 */
public class StringToNumberConverter implements TypeConverter {
    @Override
    public boolean support(Object source, Class<?> targetType) {
        if (source == null) {
            return false;
        }
        return ClassUtils.isAssignable(source.getClass(), String.class, true) && (
            ClassUtils.isAssignable(targetType, Number.class, true)
                || ClassUtils.isAssignable(targetType, Byte.class, true)
                || ClassUtils.isAssignable(targetType, Short.class, true)
                || ClassUtils.isAssignable(targetType, Integer.class, true)
                || ClassUtils.isAssignable(targetType, Long.class, true)
                || ClassUtils.isAssignable(targetType, Float.class, true)
                || ClassUtils.isAssignable(targetType, Double.class, true)
                || ClassUtils.isAssignable(targetType, BigDecimal.class, true)
        );
    }

    @Override
    public Object convert(Object source, Class<?> targetType, Object context) {
        if (source == null) {
            return null;
        }
        String str = String.valueOf(source);
        if (ClassUtils.isAssignable(targetType, Byte.class, true)) {
            return NumberUtils.toByte(str);
        }
        if (ClassUtils.isAssignable(targetType, Short.class, true)) {
            return NumberUtils.toShort(str);
        }
        if (ClassUtils.isAssignable(targetType, Integer.class, true)) {
            return NumberUtils.toInt(str);
        }
        if (ClassUtils.isAssignable(targetType, Long.class, true)) {
            return NumberUtils.toLong(str);
        }
        if (ClassUtils.isAssignable(targetType, Float.class, true)) {
            return NumberUtils.toFloat(str);
        }
        if (ClassUtils.isAssignable(targetType, Double.class, true)) {
            return NumberUtils.toDouble(str);
        }
        if (ClassUtils.isAssignable(targetType, BigDecimal.class, true)) {
            return new BigDecimal(str);
        }
        return null;
    }
}
