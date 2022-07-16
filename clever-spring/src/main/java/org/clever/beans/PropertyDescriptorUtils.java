package org.clever.beans;

import org.clever.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;

/**
 * 内部{@link PropertyDescriptor}实现的常见委托方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:42 <br/>
 */
abstract class PropertyDescriptorUtils {
    /**
     * 参考 {@link java.beans.FeatureDescriptor}.
     */
    public static void copyNonMethodProperties(PropertyDescriptor source, PropertyDescriptor target) {
        target.setExpert(source.isExpert());
        target.setHidden(source.isHidden());
        target.setPreferred(source.isPreferred());
        target.setName(source.getName());
        target.setShortDescription(source.getShortDescription());
        target.setDisplayName(source.getDisplayName());
        // Copy all attributes (emulating behavior of private FeatureDescriptor#addTable)
        Enumeration<String> keys = source.attributeNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            target.setValue(key, source.getValue(key));
        }
        // See java.beans.PropertyDescriptor#PropertyDescriptor(PropertyDescriptor)
        target.setPropertyEditorClass(source.getPropertyEditorClass());
        target.setBound(source.isBound());
        target.setConstrained(source.isConstrained());
    }

    /**
     * 参考 {@link java.beans.PropertyDescriptor#findPropertyType(Method, Method)}.
     */
    @SuppressWarnings({"StatementWithEmptyBody", "JavadocReference"})
    public static Class<?> findPropertyType(Method readMethod, Method writeMethod) throws IntrospectionException {
        Class<?> propertyType = null;
        if (readMethod != null) {
            if (readMethod.getParameterCount() != 0) {
                throw new IntrospectionException("Bad read method arg count: " + readMethod);
            }
            propertyType = readMethod.getReturnType();
            if (propertyType == Void.TYPE) {
                throw new IntrospectionException("Read method returns void: " + readMethod);
            }
        }
        if (writeMethod != null) {
            Class<?>[] params = writeMethod.getParameterTypes();
            if (params.length != 1) {
                throw new IntrospectionException("Bad write method arg count: " + writeMethod);
            }
            if (propertyType != null) {
                if (propertyType.isAssignableFrom(params[0])) {
                    // Write method's property type potentially more specific
                    propertyType = params[0];
                } else if (params[0].isAssignableFrom(propertyType)) {
                    // Proceed with read method's property type
                } else {
                    throw new IntrospectionException("Type mismatch between read and write methods: " + readMethod + " - " + writeMethod);
                }
            } else {
                propertyType = params[0];
            }
        }
        return propertyType;
    }

    /**
     * See {@link java.beans.IndexedPropertyDescriptor#findIndexedPropertyType(Method, Method)}.
     */
    @SuppressWarnings({"StatementWithEmptyBody", "JavadocReference", "SingleElementAnnotation"})
    public static Class<?> findIndexedPropertyType(String name,
                                                   Class<?> propertyType,
                                                   Method indexedReadMethod,
                                                   Method indexedWriteMethod) throws IntrospectionException {
        Class<?> indexedPropertyType = null;
        if (indexedReadMethod != null) {
            Class<?>[] params = indexedReadMethod.getParameterTypes();
            if (params.length != 1) {
                throw new IntrospectionException("Bad indexed read method arg count: " + indexedReadMethod);
            }
            if (params[0] != Integer.TYPE) {
                throw new IntrospectionException("Non int index to indexed read method: " + indexedReadMethod);
            }
            indexedPropertyType = indexedReadMethod.getReturnType();
            if (indexedPropertyType == Void.TYPE) {
                throw new IntrospectionException("Indexed read method returns void: " + indexedReadMethod);
            }
        }
        if (indexedWriteMethod != null) {
            Class<?>[] params = indexedWriteMethod.getParameterTypes();
            if (params.length != 2) {
                throw new IntrospectionException("Bad indexed write method arg count: " + indexedWriteMethod);
            }
            if (params[0] != Integer.TYPE) {
                throw new IntrospectionException("Non int index to indexed write method: " + indexedWriteMethod);
            }
            if (indexedPropertyType != null) {
                if (indexedPropertyType.isAssignableFrom(params[1])) {
                    // Write method's property type potentially more specific
                    indexedPropertyType = params[1];
                } else if (params[1].isAssignableFrom(indexedPropertyType)) {
                    // Proceed with read method's property type
                } else {
                    throw new IntrospectionException("Type mismatch between indexed read and write methods: " + indexedReadMethod + " - " + indexedWriteMethod);
                }
            } else {
                indexedPropertyType = params[1];
            }
        }
        if (propertyType != null && (!propertyType.isArray() || propertyType.getComponentType() != indexedPropertyType)) {
            throw new IntrospectionException("Type mismatch between indexed and non-indexed methods: " + indexedReadMethod + " - " + indexedWriteMethod);
        }
        return indexedPropertyType;
    }

    /**
     * 比较给定的PropertyDescriptor，如果它们等效，则返回true，即它们的读方法、写方法、属性类型、属性编辑器和标志等效
     *
     * @see java.beans.PropertyDescriptor#equals(Object)
     */
    public static boolean equals(PropertyDescriptor pd, PropertyDescriptor otherPd) {
        return ObjectUtils.nullSafeEquals(pd.getReadMethod(), otherPd.getReadMethod())
                && ObjectUtils.nullSafeEquals(pd.getWriteMethod(), otherPd.getWriteMethod())
                && ObjectUtils.nullSafeEquals(pd.getPropertyType(), otherPd.getPropertyType())
                && ObjectUtils.nullSafeEquals(pd.getPropertyEditorClass(), otherPd.getPropertyEditorClass())
                && pd.isBound() == otherPd.isBound() && pd.isConstrained() == otherPd.isConstrained();
    }
}
