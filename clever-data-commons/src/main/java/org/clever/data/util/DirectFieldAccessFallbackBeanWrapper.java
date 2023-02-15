package org.clever.data.util;

import org.clever.beans.BeanWrapperImpl;
import org.clever.beans.NotReadablePropertyException;
import org.clever.beans.NotWritablePropertyException;

import java.lang.reflect.Field;

import static org.clever.util.ReflectionUtils.*;

/**
 * {@link BeanWrapperImpl} 的自定义扩展，在被包装的对象或类型不使用访问器方法的情况下回退到直接字段访问
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/15 22:59 <br/>
 */
public class DirectFieldAccessFallbackBeanWrapper extends BeanWrapperImpl {
    public DirectFieldAccessFallbackBeanWrapper(Object entity) {
        super(entity);
    }

    public DirectFieldAccessFallbackBeanWrapper(Class<?> type) {
        super(type);
    }

    @Override
    public Object getPropertyValue(String propertyName) {
        try {
            return super.getPropertyValue(propertyName);
        } catch (NotReadablePropertyException e) {
            Field field = findField(getWrappedClass(), propertyName);
            if (field == null) {
                throw new NotReadablePropertyException(getWrappedClass(), propertyName, "Could not find field for property during fallback access");
            }
            makeAccessible(field);
            return getField(field, getWrappedInstance());
        }
    }

    @Override
    public void setPropertyValue(String propertyName, Object value) {
        try {
            super.setPropertyValue(propertyName, value);
        } catch (NotWritablePropertyException e) {
            Field field = findField(getWrappedClass(), propertyName);
            if (field == null) {
                throw new NotWritablePropertyException(getWrappedClass(), propertyName, "Could not find field for property during fallback access", e);
            }
            makeAccessible(field);
            setField(field, getWrappedInstance(), value);
        }
    }
}
