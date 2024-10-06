package org.clever.spring.shim;

import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 通过反射使用 Spring 框架的内部类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/09/20 10:35 <br/>
 */
abstract class ReflectionsUtils {
    /**
     * 获取默认的 ClassLoader
     */
    public static ClassLoader getDefClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 获取class的构造函数，并设置可以访问
     *
     * @param classLoader    ClassLoader对象
     * @param classFullName  class全名
     * @param parameterTypes 构造函数的参数类型
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <T> Constructor<T> getAccessibleConstructor(final ClassLoader classLoader, final String classFullName, final Class<?>... parameterTypes) {
        Class<?> clazz = Class.forName(classFullName, true, classLoader);
        Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
        if (!constructor.canAccess(null)) {
            constructor.setAccessible(true);
        }
        return (Constructor<T>) constructor;
    }

    /**
     * 在给定的对象中查找指定的方法，获取不到就在其父类中找(循环向上转型)
     *
     * @param clazz          class对象
     * @param methodName     方法名
     * @param parameterTypes 方法参数类型
     */
    public static Method getAccessibleMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        Class<?> searchType = clazz;
        while (searchType != Object.class) {
            try {
                return searchType.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
            // 获取父类类型，继续查找方法
            searchType = searchType.getSuperclass();
        }
        throw new IllegalArgumentException("Can't find method " + methodName + " in class " + clazz);
    }

    /**
     * 在给定的对象中查找指定的成员变量，获取不到就在其父类中找(循环向上转型)
     *
     * @param clazz     class对象
     * @param fieldName 成员变量名称
     */
    public static Field getAccessibleField(final Class<?> clazz, final String fieldName) {
        Class<?> searchType = clazz;
        while (searchType != Object.class) {
            try {
                return searchType.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
            // 获取父类类型，继续查找字段
            searchType = searchType.getSuperclass();
        }
        throw new IllegalArgumentException("Can't find field " + fieldName + " in class " + clazz);
    }

    /**
     * 实例化指定的类型
     *
     * @param classLoader    ClassLoader对象
     * @param classFullName  class全名
     * @param parameterTypes 构造函数的参数类型
     * @param args           构造函数的参数值
     */
    @SneakyThrows
    public static <T> T newInstance(final ClassLoader classLoader, final String classFullName, final Class<?>[] parameterTypes, final Object[] args) {
        Constructor<T> constructor = getAccessibleConstructor(classLoader, classFullName, parameterTypes);
        return constructor.newInstance(args);
    }

    /**
     * 实例化指定的类型
     *
     * @param classFullName  class全名
     * @param parameterTypes 构造函数的参数类型
     * @param args           构造函数的参数值
     */
    public static <T> T newInstance(final String classFullName, final Class<?>[] parameterTypes, final Object[] args) {
        return newInstance(getDefClassLoader(), classFullName, parameterTypes, args);
    }

    /**
     * 直接调用对象方法, 无视private/protected修饰符
     *
     * @param obj            目标对象
     * @param methodName     方法名称
     * @param parameterTypes 方法签名参数类型
     * @param args           调用方法参数值
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <T> T invokeMethod(final Object obj, final String methodName, final Class<?>[] parameterTypes, final Object[] args) {
        Method method = getAccessibleMethod(obj.getClass(), methodName, parameterTypes);
        if (!method.canAccess(obj)) {
            method.setAccessible(true);
        }
        return (T) method.invoke(obj, args);
    }

    /**
     * 通过反射直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数
     *
     * @param obj       目标对象
     * @param fieldName 属性名，不支持：属性名.属性名.属性名...
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <T> T getFieldValue(final Object obj, final String fieldName) {
        if (obj == null) {
            return null;
        }
        Field field = getAccessibleField(obj.getClass(), fieldName);
        if (!field.canAccess(obj)) {
            field.setAccessible(true);
        }
        return (T) field.get(obj);
    }

    /**
     * 通过反射直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数
     *
     * @param obj       目标对象
     * @param fieldName 属性名，不支持：属性名.属性名.属性名...
     * @param value     属性值
     */
    @SneakyThrows
    public static void setFieldValue(final Object obj, final String fieldName, final Object value) {
        Field field = getAccessibleField(obj.getClass(), fieldName);
        if (!field.canAccess(obj)) {
            field.setAccessible(true);
        }
        field.set(obj, value);
    }
}
