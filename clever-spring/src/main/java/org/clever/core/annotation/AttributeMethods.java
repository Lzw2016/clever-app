package org.clever.core.annotation;

import org.clever.util.Assert;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * 提供了一种快速访问注解的属性方法(具有一致的顺序)的方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:38 <br/>
 */
final class AttributeMethods {
    /**
     * 表示AttributeMethods的null值，防止{@link #cache}报空指针异常
     */
    static final AttributeMethods NONE = new AttributeMethods(null, new Method[0]);
    /**
     * 已解析的注解的缓存，{@code Map<注解类型, AttributeMethods>}
     */
    private static final Map<Class<? extends Annotation>, AttributeMethods> cache = new ConcurrentReferenceHashMap<>();

    /**
     * 注解属性排序，根据属性名称排序
     */
    private static final Comparator<Method> methodComparator = (m1, m2) -> {
        if (m1 != null && m2 != null) {
            return m1.getName().compareTo(m2.getName());
        }
        return m1 != null ? -1 : 1;
    };

    /**
     * 注解类型
     */
    private final Class<? extends Annotation> annotationType;
    /**
     * 注解的属性方法
     */
    private final Method[] attributeMethods;
    /**
     * 注解方法没有设置时是否抛出异常
     */
    private final boolean[] canThrowTypeNotPresentException;
    /**
     * 是否有默认值方法
     */
    private final boolean hasDefaultValueMethod;
    /**
     * 是否有嵌套注解
     */
    private final boolean hasNestedAnnotation;

    private AttributeMethods(Class<? extends Annotation> annotationType, Method[] attributeMethods) {
        this.annotationType = annotationType;
        this.attributeMethods = attributeMethods;
        this.canThrowTypeNotPresentException = new boolean[attributeMethods.length];
        boolean foundDefaultValueMethod = false;
        boolean foundNestedAnnotation = false;
        for (int i = 0; i < attributeMethods.length; i++) {
            Method method = this.attributeMethods[i];
            Class<?> type = method.getReturnType();
            if (!foundDefaultValueMethod && (method.getDefaultValue() != null)) {
                foundDefaultValueMethod = true;
            }
            if (!foundNestedAnnotation && (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation()))) {
                foundNestedAnnotation = true;
            }
            ReflectionUtils.makeAccessible(method);
            this.canThrowTypeNotPresentException[i] = (type == Class.class || type == Class[].class || type.isEnum());
        }
        this.hasDefaultValueMethod = foundDefaultValueMethod;
        this.hasNestedAnnotation = foundNestedAnnotation;
    }

    /**
     * 确定此实例是否只包含一个名为value的属性
     */
    boolean hasOnlyValueAttribute() {
        return (this.attributeMethods.length == 1 && MergedAnnotation.VALUE.equals(this.attributeMethods[0].getName()));
    }

    /**
     * 确定是否可以安全访问给定注解中的值，而不会导致任何 {@link TypeNotPresentException}
     *
     * @param annotation 要检查的注解
     */
    boolean isValid(Annotation annotation) {
        assertAnnotation(annotation);
        for (int i = 0; i < size(); i++) {
            if (canThrowTypeNotPresentException(i)) {
                try {
                    get(i).invoke(annotation);
                } catch (Throwable ex) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查给定注解中的值是否可以安全访问，而不会导致任何{@link TypeNotPresentException}<br/>
     * 如果不能安全访问直接抛出异常
     *
     * @param annotation 要验证的注解
     * @see #isValid(Annotation)
     */
    void validate(Annotation annotation) {
        assertAnnotation(annotation);
        for (int i = 0; i < size(); i++) {
            if (canThrowTypeNotPresentException(i)) {
                try {
                    get(i).invoke(annotation);
                } catch (Throwable ex) {
                    throw new IllegalStateException(
                            "Could not obtain annotation attribute value for " + get(i).getName() + " declared on " + annotation.annotationType(),
                            ex
                    );
                }
            }
        }
    }

    /**
     * 断言注解类型
     */
    private void assertAnnotation(Annotation annotation) {
        Assert.notNull(annotation, "Annotation must not be null");
        if (this.annotationType != null) {
            Assert.isInstanceOf(this.annotationType, annotation);
        }
    }

    /**
     * 获取具有指定名称的属性，如果不存在匹配的属性，则返回null
     */
    Method get(String name) {
        int index = indexOf(name);
        return index != -1 ? this.attributeMethods[index] : null;
    }

    /**
     * 获取指定索引处的属性
     */
    Method get(int index) {
        return this.attributeMethods[index];
    }

    /**
     * 确定指定索引处的属性在被访问时是否会引发{@link TypeNotPresentException}
     */
    boolean canThrowTypeNotPresentException(int index) {
        return this.canThrowTypeNotPresentException[index];
    }

    /**
     * 获取具有指定名称属性的索引，如果不存在则返回-1
     */
    int indexOf(String name) {
        for (int i = 0; i < this.attributeMethods.length; i++) {
            if (this.attributeMethods[i].getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取指定属性的索引，如果不存在则返回-1
     */
    int indexOf(Method attribute) {
        for (int i = 0; i < this.attributeMethods.length; i++) {
            if (this.attributeMethods[i].equals(attribute)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取此集合中的属性数
     */
    int size() {
        return this.attributeMethods.length;
    }

    /**
     * 是否有默认值方法
     */
    boolean hasDefaultValueMethod() {
        return this.hasDefaultValueMethod;
    }

    /**
     * 是否有嵌套注解
     */
    boolean hasNestedAnnotation() {
        return this.hasNestedAnnotation;
    }

    /**
     * 获取给定注解类型的属性方法
     *
     * @param annotationType 注解类型
     */
    static AttributeMethods forAnnotationType(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return NONE;
        }
        return cache.computeIfAbsent(annotationType, AttributeMethods::compute);
    }

    /**
     * 解析注解类型，包装为AttributeMethods
     *
     * @param annotationType 注解类型
     */
    private static AttributeMethods compute(Class<? extends Annotation> annotationType) {
        Method[] methods = annotationType.getDeclaredMethods();
        int size = methods.length;
        for (int i = 0; i < methods.length; i++) {
            if (!isAttributeMethod(methods[i])) {
                methods[i] = null;
                size--;
            }
        }
        if (size == 0) {
            return NONE;
        }
        Arrays.sort(methods, methodComparator);
        Method[] attributeMethods = Arrays.copyOf(methods, size);
        return new AttributeMethods(annotationType, attributeMethods);
    }

    /**
     * 判断{@code Method}是否是属性方法
     */
    private static boolean isAttributeMethod(Method method) {
        return (method.getParameterCount() == 0 && method.getReturnType() != void.class);
    }

    /**
     * 为给定的属性方法创建一个描述，适合在异常消息和日志中使用
     */
    static String describe(Method attribute) {
        if (attribute == null) {
            return "(none)";
        }
        return describe(attribute.getDeclaringClass(), attribute.getName());
    }

    /**
     * 为给定的属性方法创建一个描述，适合在异常消息和日志中使用
     *
     * @param annotationType 注解类型
     * @param attributeName  属性名
     */
    static String describe(Class<?> annotationType, String attributeName) {
        if (attributeName == null) {
            return "(none)";
        }
        String in = (annotationType != null ? " in annotation [" + annotationType.getName() + "]" : "");
        return "attribute '" + attributeName + "'" + in;
    }
}
