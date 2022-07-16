package org.clever.core.annotation;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 包装注解属性值的类，本质是一个{@code Map<属性名, 属性值>}，继承{@link LinkedHashMap}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:25 <br/>
 */
public class AnnotationAttributes extends LinkedHashMap<String, Object> {
    /**
     * 未知的注解名称值
     */
    private static final String UNKNOWN = "unknown";
    /**
     * 注解类型
     */
    private final Class<? extends Annotation> annotationType;
    /**
     * 注解名称
     */
    final String displayName;
    /**
     * 注解属性是否已经验证过
     */
    boolean validated = false;

    /**
     * 创建一个空的{@link AnnotationAttributes}实例
     */
    public AnnotationAttributes() {
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    /**
     * 创建一个空的{@link AnnotationAttributes}实例
     *
     * @param initialCapacity 初始容量
     */
    public AnnotationAttributes(int initialCapacity) {
        super(initialCapacity);
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    /**
     * 基于一个Map创建一个{@link AnnotationAttributes}实例
     *
     * @see #fromMap(Map)
     */
    public AnnotationAttributes(Map<String, Object> map) {
        super(map);
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    /**
     * 基于一个AnnotationAttributes创建一个{@link AnnotationAttributes}实例
     *
     * @see #fromMap(Map)
     */
    public AnnotationAttributes(AnnotationAttributes other) {
        super(other);
        this.annotationType = other.annotationType;
        this.displayName = other.displayName;
        this.validated = other.validated;
    }

    /**
     * 基于一个注解类型创建一个{@link AnnotationAttributes}实例
     */
    public AnnotationAttributes(Class<? extends Annotation> annotationType) {
        Assert.notNull(annotationType, "'annotationType' must not be null");
        this.annotationType = annotationType;
        this.displayName = annotationType.getName();
    }

    /**
     * 基于一个注解类型创建一个{@link AnnotationAttributes}实例
     *
     * @param annotationType 注解类型
     * @param validated      注解属性是否已经验证过
     */
    AnnotationAttributes(Class<? extends Annotation> annotationType, boolean validated) {
        Assert.notNull(annotationType, "'annotationType' must not be null");
        this.annotationType = annotationType;
        this.displayName = annotationType.getName();
        this.validated = validated;
    }

    /**
     * 基于一个注解类型创建一个{@link AnnotationAttributes}实例
     *
     * @param annotationType 注解类型名
     * @param classLoader    对应的类加载器
     */
    public AnnotationAttributes(String annotationType, ClassLoader classLoader) {
        Assert.notNull(annotationType, "'annotationType' must not be null");
        this.annotationType = getAnnotationType(annotationType, classLoader);
        this.displayName = annotationType;
    }

    /**
     * 根据注解名获取注解类型
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> getAnnotationType(String annotationType, ClassLoader classLoader) {
        if (classLoader != null) {
            try {
                return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
            } catch (ClassNotFoundException ex) {
                // Annotation Class not resolvable
            }
        }
        return null;
    }

    /**
     * 获取此AnnotationAttributes表示的注解类型
     */
    public Class<? extends Annotation> annotationType() {
        return this.annotationType;
    }

    /**
     * 获取注解的属性值(String类型)
     */
    public String getString(String attributeName) {
        return getRequiredAttribute(attributeName, String.class);
    }

    /**
     * 获取注解的属性值(String[]类型)
     */
    public String[] getStringArray(String attributeName) {
        return getRequiredAttribute(attributeName, String[].class);
    }

    /**
     * 获取注解的属性值(boolean类型)
     */
    public boolean getBoolean(String attributeName) {
        return getRequiredAttribute(attributeName, Boolean.class);
    }

    /**
     * 获取注解的属性值(Number类型)
     */
    @SuppressWarnings("unchecked")
    public <N extends Number> N getNumber(String attributeName) {
        return (N) getRequiredAttribute(attributeName, Number.class);
    }

    /**
     * 获取注解的属性值(Enum类型)
     */
    @SuppressWarnings("unchecked")
    public <E extends Enum<?>> E getEnum(String attributeName) {
        return (E) getRequiredAttribute(attributeName, Enum.class);
    }

    /**
     * 获取注解的属性值(Class类型)
     */
    @SuppressWarnings("unchecked")
    public <T> Class<? extends T> getClass(String attributeName) {
        return getRequiredAttribute(attributeName, Class.class);
    }

    /**
     * 获取注解的属性值(Class[]类型)
     */
    public Class<?>[] getClassArray(String attributeName) {
        return getRequiredAttribute(attributeName, Class[].class);
    }

    /**
     * 获取注解的属性值(AnnotationAttributes类型)
     */
    public AnnotationAttributes getAnnotation(String attributeName) {
        return getRequiredAttribute(attributeName, AnnotationAttributes.class);
    }

    /**
     * 获取注解的属性值(Annotation类型)
     */
    public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
        return getRequiredAttribute(attributeName, annotationType);
    }

    /**
     * 获取注解的属性值(AnnotationAttributes[]类型)
     */
    public AnnotationAttributes[] getAnnotationArray(String attributeName) {
        return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
    }

    /**
     * 获取注解的属性值(Annotation[]类型)
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
        Object array = Array.newInstance(annotationType, 0);
        return (A[]) getRequiredAttribute(attributeName, array.getClass());
    }

    /**
     * 获取注解的属性值
     *
     * @param attributeName 属性名
     * @param expectedType  属性值类型
     * @return the value
     */
    @SuppressWarnings("unchecked")
    private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
        Assert.hasText(attributeName, "'attributeName' must not be null or empty");
        Object value = get(attributeName);
        assertAttributePresence(attributeName, value);
        assertNotException(attributeName, value);
        if (!expectedType.isInstance(value) && expectedType.isArray() && expectedType.getComponentType().isInstance(value)) {
            Object array = Array.newInstance(expectedType.getComponentType(), 1);
            Array.set(array, 0, value);
            value = array;
        }
        assertAttributeType(attributeName, value, expectedType);
        return (T) value;
    }

    private void assertAttributePresence(String attributeName, Object attributeValue) {
        Assert.notNull(
                attributeValue,
                () -> String.format(
                        "Attribute '%s' not found in attributes for annotation [%s]",
                        attributeName,
                        this.displayName
                )
        );
    }

    private void assertNotException(String attributeName, Object attributeValue) {
        if (attributeValue instanceof Throwable) {
            throw new IllegalArgumentException(
                    String.format(
                            "Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
                            attributeName,
                            this.displayName,
                            attributeValue
                    ),
                    (Throwable) attributeValue
            );
        }
    }

    private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
        if (!expectedType.isInstance(attributeValue)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Attribute '%s' is of type %s, but %s was expected in attributes for annotation [%s]",
                            attributeName,
                            attributeValue.getClass().getSimpleName(),
                            expectedType.getSimpleName(),
                            this.displayName
                    )
            );
        }
    }

    @Override
    public String toString() {
        Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
        StringBuilder sb = new StringBuilder("{");
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(valueToString(entry.getValue()));
            if (entries.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private String valueToString(Object value) {
        if (value == this) {
            return "(this Map)";
        }
        if (value instanceof Object[]) {
            return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
        }
        return String.valueOf(value);
    }

    /**
     * 把Map包装为{@link AnnotationAttributes}实例，只包装一次
     */
    public static AnnotationAttributes fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        if (map instanceof AnnotationAttributes) {
            return (AnnotationAttributes) map;
        }
        return new AnnotationAttributes(map);
    }
}
