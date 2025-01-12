package org.clever.core.converter;

import org.apache.commons.lang3.ClassUtils;
import org.clever.core.DateUtils;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2019/12/19 14:31 <br/>
 */
public class StringToDateConverter implements TypeConverter {
    @Override
    public boolean support(Object source, Class<?> targetType) {
        if (source == null) {
            return false;
        }
        return ClassUtils.isAssignable(source.getClass(), String.class, true)
            && ClassUtils.isAssignable(targetType, Date.class, true);
    }

    @Override
    public Object convert(Object source, Class<?> targetType, Object context) {
        return DateUtils.parseDate(source);
    }
}
