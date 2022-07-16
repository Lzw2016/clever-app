package org.clever.core.convert;

import org.clever.core.MethodParameter;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ObjectUtils;
import org.clever.util.ReflectionUtils;
import org.clever.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JavaBean属性的描述，避免对{@code java.beans.PropertyDescriptor}依赖。<br/>
 * {@code java.beans}包在许多环境中都不可用(例如Android、Java ME)。<br/>
 * 用于从{@link Property}构建{@link TypeDescriptor}。然后可以使用{@code TypeDescriptor}与{@code Property}相互转换
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 12:58 <br/>
 *
 * @see TypeDescriptor#TypeDescriptor(Property)
 * @see TypeDescriptor#nested(Property, int)
 */
public final class Property {
    private static final Map<Property, Annotation[]> annotationCache = new ConcurrentReferenceHashMap<>();

    private final Class<?> objectType;
    private final Method readMethod;
    private final Method writeMethod;
    private final String name;
    private final MethodParameter methodParameter;
    private Annotation[] annotations;

    public Property(Class<?> objectType, Method readMethod, Method writeMethod) {
        this(objectType, readMethod, writeMethod, null);
    }

    public Property(Class<?> objectType, Method readMethod, Method writeMethod, String name) {
        this.objectType = objectType;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.methodParameter = resolveMethodParameter();
        this.name = (name != null ? name : resolveName());
    }

    /**
     * 声明此属性的对象，直接声明或在父类中声明
     */
    public Class<?> getObjectType() {
        return this.objectType;
    }

    /**
     * 属性名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 属性类型，如: {@code java.lang.String}
     */
    public Class<?> getType() {
        return this.methodParameter.getParameterType();
    }

    /**
     * 属性getter方法
     */
    public Method getReadMethod() {
        return this.readMethod;
    }

    /**
     * 属性setter方法
     */
    public Method getWriteMethod() {
        return this.writeMethod;
    }

    // Package private

    MethodParameter getMethodParameter() {
        return this.methodParameter;
    }

    Annotation[] getAnnotations() {
        if (this.annotations == null) {
            this.annotations = resolveAnnotations();
        }
        return this.annotations;
    }

    // Internal helpers

    private String resolveName() {
        if (this.readMethod != null) {
            int index = this.readMethod.getName().indexOf("get");
            if (index != -1) {
                index += 3;
            } else {
                index = this.readMethod.getName().indexOf("is");
                if (index != -1) {
                    index += 2;
                } else {
                    // Record-style plain accessor method, e.g. name()
                    index = 0;
                }
            }
            return StringUtils.uncapitalize(this.readMethod.getName().substring(index));
        } else if (this.writeMethod != null) {
            int index = this.writeMethod.getName().indexOf("set");
            if (index == -1) {
                throw new IllegalArgumentException("Not a setter method");
            }
            index += 3;
            return StringUtils.uncapitalize(this.writeMethod.getName().substring(index));
        } else {
            throw new IllegalStateException("Property is neither readable nor writeable");
        }
    }

    private MethodParameter resolveMethodParameter() {
        MethodParameter read = resolveReadMethodParameter();
        MethodParameter write = resolveWriteMethodParameter();
        if (write == null) {
            if (read == null) {
                throw new IllegalStateException("Property is neither readable nor writeable");
            }
            return read;
        }
        if (read != null) {
            Class<?> readType = read.getParameterType();
            Class<?> writeType = write.getParameterType();
            if (!writeType.equals(readType) && writeType.isAssignableFrom(readType)) {
                return read;
            }
        }
        return write;
    }

    private MethodParameter resolveReadMethodParameter() {
        if (getReadMethod() == null) {
            return null;
        }
        return new MethodParameter(getReadMethod(), -1).withContainingClass(getObjectType());
    }

    private MethodParameter resolveWriteMethodParameter() {
        if (getWriteMethod() == null) {
            return null;
        }
        return new MethodParameter(getWriteMethod(), 0).withContainingClass(getObjectType());
    }

    private Annotation[] resolveAnnotations() {
        Annotation[] annotations = annotationCache.get(this);
        if (annotations == null) {
            Map<Class<? extends Annotation>, Annotation> annotationMap = new LinkedHashMap<>();
            addAnnotationsToMap(annotationMap, getReadMethod());
            addAnnotationsToMap(annotationMap, getWriteMethod());
            addAnnotationsToMap(annotationMap, getField());
            annotations = annotationMap.values().toArray(new Annotation[0]);
            annotationCache.put(this, annotations);
        }
        return annotations;
    }

    private void addAnnotationsToMap(Map<Class<? extends Annotation>, Annotation> annotationMap, AnnotatedElement object) {
        if (object != null) {
            for (Annotation annotation : object.getAnnotations()) {
                annotationMap.put(annotation.annotationType(), annotation);
            }
        }
    }

    private Field getField() {
        String name = getName();
        if (!StringUtils.hasLength(name)) {
            return null;
        }
        Field field = null;
        Class<?> declaringClass = declaringClass();
        if (declaringClass != null) {
            field = ReflectionUtils.findField(declaringClass, name);
            if (field == null) {
                // Same lenient fallback checking as in CachedIntrospectionResults...
                field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
                if (field == null) {
                    field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
                }
            }
        }
        return field;
    }

    private Class<?> declaringClass() {
        if (getReadMethod() != null) {
            return getReadMethod().getDeclaringClass();
        } else if (getWriteMethod() != null) {
            return getWriteMethod().getDeclaringClass();
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Property)) {
            return false;
        }
        Property otherProperty = (Property) other;
        return (ObjectUtils.nullSafeEquals(this.objectType, otherProperty.objectType) && ObjectUtils.nullSafeEquals(this.name, otherProperty.name) && ObjectUtils.nullSafeEquals(this.readMethod, otherProperty.readMethod) && ObjectUtils.nullSafeEquals(this.writeMethod, otherProperty.writeMethod));
    }

    @Override
    public int hashCode() {
        return (ObjectUtils.nullSafeHashCode(this.objectType) * 31 + ObjectUtils.nullSafeHashCode(this.name));
    }
}
