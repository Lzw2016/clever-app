package org.clever.data.util;

import org.clever.beans.BeanWrapperImpl;
import org.clever.beans.NotReadablePropertyException;
import org.clever.beans.NotWritablePropertyException;

import java.lang.reflect.Field;

import static org.clever.util.ReflectionUtils.*;

/**
 * Custom extension of {@link BeanWrapperImpl} that falls back to direct field access in case the object or type being
 * wrapped does not use accessor methods.
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

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.BeanWrapperImpl#getPropertyValue(java.lang.String)
     */
    @Override

    public Object getPropertyValue(String propertyName) {

        try {
            return super.getPropertyValue(propertyName);
        } catch (NotReadablePropertyException e) {

            Field field = findField(getWrappedClass(), propertyName);

            if (field == null) {
                throw new NotReadablePropertyException(getWrappedClass(), propertyName,
                        "Could not find field for property during fallback access");
            }

            makeAccessible(field);
            return getField(field, getWrappedInstance());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.BeanWrapperImpl#setPropertyValue(java.lang.String, java.lang.Object)
     */
    @Override
    public void setPropertyValue(String propertyName, Object value) {

        try {
            super.setPropertyValue(propertyName, value);
        } catch (NotWritablePropertyException e) {

            Field field = findField(getWrappedClass(), propertyName);

            if (field == null) {
                throw new NotWritablePropertyException(getWrappedClass(), propertyName,
                        "Could not find field for property during fallback access", e);
            }

            makeAccessible(field);
            setField(field, getWrappedInstance(), value);
        }
    }
}
