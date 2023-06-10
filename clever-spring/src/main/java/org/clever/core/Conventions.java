package org.clever.core;

import org.clever.util.Assert;
import org.clever.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;

/**
 * 提供方法来支持整个框架中使用的各种命名和其他约定。主要供框架内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/09 16:11 <br/>
 */
public final class Conventions {
    /**
     * 使用数组时添加到名称的后缀
     */
    private static final String PLURAL_SUFFIX = "List";

    private Conventions() {
    }

    /**
     * 根据其具体类型为提供的 {@code Object} 挖掘常规变量名称。
     * 根据 JavaBeans 属性命名规则，使用的约定是返回 {@code Class} 的非大写短名称。
     * <p>例如：<br>
     * {@code com.myapp.Product} becomes {@code "product"}<br>
     * {@code com.myapp.MyProduct} becomes {@code "myProduct"}<br>
     * {@code com.myapp.UKProduct} becomes {@code "UKProduct"}<br>
     * <p>对于数组，使用数组组件类型的复数形式。
     * 对于 {@code Collection}，尝试“peek ahead”以确定组件类型并返回其复数版本。
     *
     * @param value 为其生成变量名的值
     * @return 生成的变量名
     */
    public static String getVariableName(Object value) {
        Assert.notNull(value, "Value must not be null");
        Class<?> valueClass;
        boolean pluralize = false;
        if (value.getClass().isArray()) {
            valueClass = value.getClass().getComponentType();
            pluralize = true;
        } else if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                throw new IllegalArgumentException("Cannot generate variable name for an empty Collection");
            }
            Object valueToCheck = peekAhead(collection);
            valueClass = getClassForValue(valueToCheck);
            pluralize = true;
        } else {
            valueClass = getClassForValue(value);
        }
        String name = ClassUtils.getShortNameAsProperty(valueClass);
        return (pluralize ? pluralize(name) : name);
    }

    /**
     * 考虑通用集合类型（如果有），确定给定参数的常规变量名称。
     * <p>此方法不支持反应类型：<br>
     * {@code Mono<com.myapp.Product>} 成为 {@code "productMono"}<br>
     * {@code Flux<com.myapp.MyProduct>} 成为 {@code "myProductFlux"}<br>
     * {@code Observable<com.myapp.MyProduct>} 成为 {@code "myProductObservable"}<br>
     *
     * @param parameter 方法或构造函数参数
     * @return 生成的变量名
     */
    public static String getVariableNameForParameter(MethodParameter parameter) {
        Assert.notNull(parameter, "MethodParameter must not be null");
        Class<?> valueClass;
        boolean pluralize = false;
        String reactiveSuffix = "";
        if (parameter.getParameterType().isArray()) {
            valueClass = parameter.getParameterType().getComponentType();
            pluralize = true;
        } else if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
            valueClass = ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
            if (valueClass == null) {
                throw new IllegalArgumentException("Cannot generate variable name for non-typed Collection parameter type");
            }
            pluralize = true;
        } else {
            valueClass = parameter.getParameterType();
        }
        String name = ClassUtils.getShortNameAsProperty(valueClass);
        return (pluralize ? pluralize(name) : name + reactiveSuffix);
    }

    /**
     * 确定给定方法的返回类型的常规变量名称，同时考虑泛型集合类型（如果有）。
     *
     * @param method 生成变量名的方法
     * @return 生成的变量名
     */
    public static String getVariableNameForReturnType(Method method) {
        return getVariableNameForReturnType(method, method.getReturnType(), null);
    }

    /**
     * 确定给定方法的返回类型的常规变量名称，考虑通用集合类型（如果有的话），
     * 如果方法声明不够具体，则返回给定的实际返回值，例如{@code Object} 返回类型或无类型集合。
     *
     * @param method 生成变量名的方法
     * @param value  返回值（如果不可用则可能是 {@code null}）
     * @return 生成的变量名
     */
    public static String getVariableNameForReturnType(Method method, Object value) {
        return getVariableNameForReturnType(method, method.getReturnType(), value);
    }

    /**
     * 确定给定方法的返回类型的常规变量名称，考虑泛型集合类型（如果有的话），
     * 如果方法声明不够具体，则回退到给定的返回值，例如{@code Object} 返回类型或无类型集合。
     * <p>此方法不支持反应类型：<br>
     * {@code Mono<com.myapp.Product>} 成为 {@code "productMono"}<br>
     * {@code Flux<com.myapp.MyProduct>} 成为 {@code "myProductFlux"}<br>
     * {@code Observable<com.myapp.MyProduct>} 成为 {@code "myProductObservable"}<br>
     *
     * @param method       生成变量名的方法
     * @param resolvedType 方法的解析返回类型
     * @param value        返回值（如果不可用则可能是 {@code null}）
     * @return 生成的变量名
     */
    public static String getVariableNameForReturnType(Method method, Class<?> resolvedType, Object value) {
        Assert.notNull(method, "Method must not be null");
        if (Object.class == resolvedType) {
            if (value == null) {
                throw new IllegalArgumentException("Cannot generate variable name for an Object return type with null value");
            }
            return getVariableName(value);
        }
        Class<?> valueClass;
        boolean pluralize = false;
        String reactiveSuffix = "";
        if (resolvedType.isArray()) {
            valueClass = resolvedType.getComponentType();
            pluralize = true;
        } else if (Collection.class.isAssignableFrom(resolvedType)) {
            valueClass = ResolvableType.forMethodReturnType(method).asCollection().resolveGeneric();
            if (valueClass == null) {
                if (!(value instanceof Collection)) {
                    throw new IllegalArgumentException("Cannot generate variable name " + "for non-typed Collection return type and a non-Collection value");
                }
                Collection<?> collection = (Collection<?>) value;
                if (collection.isEmpty()) {
                    throw new IllegalArgumentException("Cannot generate variable name " + "for non-typed Collection return type and an empty Collection value");
                }
                Object valueToCheck = peekAhead(collection);
                valueClass = getClassForValue(valueToCheck);
            }
            pluralize = true;
        } else {
            valueClass = resolvedType;
        }
        String name = ClassUtils.getShortNameAsProperty(valueClass);
        return (pluralize ? pluralize(name) : name + reactiveSuffix);
    }

    /**
     * 将属性名称格式（例如小写、连字符分隔单词）的 {@code String} 转换为属性名称格式（驼峰式）。
     * 例如 {@code transaction-manager} 变成 {@code "transactionManager"}。
     */
    public static String attributeNameToPropertyName(String attributeName) {
        Assert.notNull(attributeName, "'attributeName' must not be null");
        if (!attributeName.contains("-")) {
            return attributeName;
        }
        char[] result = new char[attributeName.length() - 1]; // 不完全准确但很好的猜测
        int currPos = 0;
        boolean upperCaseNext = false;
        for (int i = 0; i < attributeName.length(); i++) {
            char c = attributeName.charAt(i);
            if (c == '-') {
                upperCaseNext = true;
            } else if (upperCaseNext) {
                result[currPos++] = Character.toUpperCase(c);
                upperCaseNext = false;
            } else {
                result[currPos++] = c;
            }
        }
        return new String(result, 0, currPos);
    }

    /**
     * 返回由给定的封闭 {@link Class} 限定的属性名称。
     * 例如，由 {@link Class} '{@code com.myapp.SomeClass}' 限定的属性名称 '{@code foo}' 将是 '{@code com.myapp.SomeClass.foo}'
     */
    public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
        Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
        Assert.notNull(attributeName, "'attributeName' must not be null");
        return enclosingClass.getName() + '.' + attributeName;
    }

    /**
     * 确定用于命名包含给定值的变量的类。
     * <p>将返回给定值的类，除非遇到 JDK 代理，在这种情况下它将确定该代理实现的“主要”接口。
     *
     * @param value 要检查的值
     * @return 用于命名变量的类
     */
    private static Class<?> getClassForValue(Object value) {
        Class<?> valueClass = value.getClass();
        if (Proxy.isProxyClass(valueClass)) {
            Class<?>[] ifcs = valueClass.getInterfaces();
            for (Class<?> ifc : ifcs) {
                if (!ClassUtils.isJavaLanguageInterface(ifc)) {
                    return ifc;
                }
            }
        } else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
            // 类名中有 '$' 但没有内部类 - 假设它是一个特殊的子类（例如，通过 OpenJPA）
            valueClass = valueClass.getSuperclass();
        }
        return valueClass;
    }

    /**
     * 复数化给定的名称
     */
    private static String pluralize(String name) {
        return name + PLURAL_SUFFIX;
    }

    /**
     * 检索 {@code Collection} 中元素的 {@code Class}。检索 {@code Class} 的确切元素将取决于具体的 {@code Collection} 实现。
     */
    private static <E> E peekAhead(Collection<E> collection) {
        Iterator<E> it = collection.iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("Unable to peek ahead in non-empty collection - no element found");
        }
        E value = it.next();
        if (value == null) {
            throw new IllegalStateException("Unable to peek ahead in non-empty collection - only null element found");
        }
        return value;
    }
}
