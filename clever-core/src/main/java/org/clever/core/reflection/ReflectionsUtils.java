package org.clever.core.reflection;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 反射工具类(自己实现的)<br/>
 * 1.提供调用getter/setter方法<br/>
 * 2.访问私有变量<br/>
 * 3.调用私有方法<br/>
 * 4.获取泛型类型Class<br/>
 * 5.被AOP过的真实类等工具函数<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-4-29 23:56 <br/>
 */
@Slf4j
public class ReflectionsUtils {
    /**
     * set方法前缀
     */
    private static final String SETTER_PREFIX = "set";
    /**
     * get方法前缀
     */
    private static final String GETTER_PREFIX = "get";
    private static final String CGLIB_CLASS_SEPARATOR = "$$";

    /**
     * 改变private/protected的方法为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
     *
     * @param method 目标方法实例
     */
    public static void makeAccessible(final Method method, final Object obj) {
        if (Modifier.isPublic(method.getModifiers()) && Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            return;
        }
        if (!method.canAccess(obj)) {
            method.setAccessible(true);
        }
    }

    /**
     * 改变private/protected的成员变量为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
     *
     * @param field 成员变量实例
     */
    private static void makeAccessible(final Field field, final Object obj) {
        if (Modifier.isPublic(field.getModifiers()) && Modifier.isPublic(field.getDeclaringClass().getModifiers()) && Modifier.isFinal(field.getModifiers())) {
            return;
        }
        if (!field.canAccess(obj)) {
            field.setAccessible(true);
        }
    }

    /**
     * 在给定的对象中查找指定的方法，获取不到就在其父类中找(循环向上转型)，获取到方法之后强行设置成public返回<br/>
     * 如向上转型到Object仍无法找到, 返回null<br/>
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)<br/>
     *
     * @param obj            目标对象
     * @param methodName     方法名称
     * @param parameterTypes 方法签名参数类型
     * @return 方法实例，获取失败返回null
     */
    public static Method getAccessibleMethod(final Object obj, final String methodName, final Class<?>... parameterTypes) {
        Class<?> searchType = obj.getClass();
        while (searchType != Object.class) {
            try {
                Method method = searchType.getDeclaredMethod(methodName, parameterTypes);
                // noinspection ConstantConditions
                if (method != null) {
                    // 强制设置方法可以访问(public)
                    makeAccessible(method, obj);
                    return method;
                } else {
                    continue;
                }
            } catch (Exception ignored) {
                // Method不在当前类定义,继续向上转型
            }
            // 获取父类类型，继续查找方法
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * 在给定的对象中查找指定的方法，获取不到就在其父类中找(循环向上转型)，获取到方法之后强行设置成public返回<br/>
     * 如向上转型到Object仍无法找到, 返回null<br/>
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)<br/>
     *
     * @param obj        目标对象
     * @param methodName 方法名称
     * @return 方法实例，获取失败返回null
     */
    public static Method getAccessibleMethodByName(final Object obj, final String methodName) {
        Class<?> searchType = obj.getClass();
        while (searchType != Object.class) {
            Method[] methods = searchType.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    // 强制设置方法可以访问(public)
                    makeAccessible(method, obj);
                    return method;
                }
            }
            // 获取父类类型，继续查找方法
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * 在给定的对象中查找指定的成员变量，获取不到就在其父类中找(循环向上转型)，获取到之后强行设置成public返回<br/>
     *
     * @param obj       目标对象
     * @param fieldName 成员变量名称
     * @return 成员变量实例
     */
    public static Field getAccessibleField(final Object obj, final String fieldName) {
        Class<?> superClass = obj.getClass();
        while (superClass != Object.class) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                // noinspection ConstantConditions
                if (field != null) {
                    // 强制设置成员变量可以访问(public)
                    makeAccessible(field, obj);
                    return field;
                } else {
                    continue;
                }
            } catch (Exception ignored) {
                // Field不在当前类定义,继续向上转型
            }
            // 获取父类类型，继续查找字段
            superClass = superClass.getSuperclass();
        }
        return null;
    }

    /**
     * 直接调用对象方法, 无视private/protected修饰符<br/>
     * 用于一次性调用的情况，否则应使用getAccessibleMethod()函数获得Method后反复调用<br/>
     *
     * @param obj            目标对象
     * @param methodName     方法名称
     * @param parameterTypes 方法签名参数类型
     * @param args           调用方法参数值
     * @return 返回方法调用返回结果
     */
    @SneakyThrows
    public static Object invokeMethod(final Object obj, final String methodName, final Class<?>[] parameterTypes, final Object[] args) {
        Method method = getAccessibleMethod(obj, methodName, parameterTypes);
        if (method == null) {
            throw new IllegalArgumentException("invokeMethod-在对象[" + obj + "]中找不到方法 [" + methodName + "]");
        }
        return method.invoke(obj, args);
    }

    /**
     * 直接调用对象方法, 无视private/protected修饰符<br/>
     * 用于一次性调用的情况，否则应使用getAccessibleMethod()函数获得Method后反复调用<br/>
     * 只匹配方法名，如果有多个相同的方法名，则只调用第一个<br/>
     *
     * @param obj        目标对象
     * @param methodName 方法名称
     * @param args       方法签名参数类型
     * @return 返回方法调用返回结果
     */
    @SneakyThrows
    public static Object invokeMethodByName(final Object obj, final String methodName, final Object[] args) {
        Method method = getAccessibleMethodByName(obj, methodName);
        if (method == null) {
            throw new IllegalArgumentException("invokeMethod-在对象[" + obj + "]中找不到方法 [" + methodName + "]");
        }
        return method.invoke(obj, args);
    }

    /**
     * 反射调用调用Getter方法
     *
     * @param obj          目标对象
     * @param propertyName 属性名，支持多级，如：对象名.对象名.方法
     * @return 调用结果
     */
    public static Object invokeGetter(Object obj, String propertyName) {
        Object object = obj;
        String[] propertyArray = StringUtils.split(propertyName, ".");
        for (String name : propertyArray) {
            String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(name);
            object = invokeMethod(object, getterMethodName, new Class[]{}, new Object[]{});
        }
        return object;
    }

    /**
     * 反射调用Setter方法
     *
     * @param obj          目标对象
     * @param propertyName 属性名，支持多级，如：对象名.对象名.方法
     * @param value        参数值
     */
    public static void invokeSetter(Object obj, String propertyName, Object value) {
        Object object = obj;
        String[] names = StringUtils.split(propertyName, ".");
        for (int i = 0; i < names.length; i++) {
            if (i < names.length - 1) {
                String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(names[i]);
                object = invokeMethod(object, getterMethodName, new Class[]{}, new Object[]{});
            } else {
                String setterMethodName = SETTER_PREFIX + StringUtils.capitalize(names[i]);
                invokeMethodByName(object, setterMethodName, new Object[]{value});
            }
        }
    }

    /**
     * 通过反射直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数
     *
     * @param obj       目标对象
     * @param fieldName 属性名，不支持：属性名.属性名.属性名...
     * @param required  如果不存在就抛出异常
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <T> T getFieldValue(final Object obj, final String fieldName, boolean required) {
        if (obj == null) {
            return null;
        }
        Field field = getAccessibleField(obj, fieldName);
        if (field == null) {
            if (required) {
                throw new IllegalArgumentException("getFieldValue-在对象[" + obj + "]中找不到字段[" + fieldName + "]");
            }
            return null;
        }
        return (T) field.get(obj);
    }

    /**
     * 通过反射直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数
     *
     * @param obj       目标对象
     * @param fieldName 属性名，不支持：属性名.属性名.属性名...
     * @return 属性值
     */
    @SneakyThrows
    public static <T> T getFieldValue(final Object obj, final String fieldName) {
        return getFieldValue(obj, fieldName, true);
    }

    /**
     * 通过反射直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数
     *
     * @param obj       目标对象
     * @param fieldName 属性名，不支持：属性名.属性名.属性名...
     * @param value     属性值
     * @param required  如果不存在就抛出异常
     */
    @SneakyThrows
    public static void setFieldValue(final Object obj, final String fieldName, final Object value, boolean required) {
        Field field = getAccessibleField(obj, fieldName);
        if (field == null) {
            if (required) {
                throw new IllegalArgumentException("getFieldValue-在对象[" + obj + "]中找不到字段[" + fieldName + "]");
            }
            return;
        }
        field.set(obj, value);
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
        setFieldValue(obj, fieldName, value, true);
    }

    /**
     * 通过反射, 获得Class定义中声明的父类的泛型参数的类型<br/>
     * 如无法找到, 返回Object.class<br/>
     * 如：public UserDao extends HibernateDao&lt;User&gt;
     *
     * @param clazz 目标Class
     * @param index 泛型类型所处的位置，例如：直接父类的泛型使用0
     * @return the 返回父类层级中的泛型类型，如无法找到, 返回Object.class
     */
    public static Class<?> getClassGenericType(final Class<?> clazz, final int index) {
        Type genType = clazz.getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            return Object.class;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            return Object.class;
        }
        return (Class<?>) params[index];
    }

    /**
     * 通过反射, 获得Class定义中声明的泛型参数的类型, 注意泛型必须定义在父类处<br/>
     * 如无法找到, 返回Object.class<br/>
     * 如：public UserDao extends HibernateDao&lt;User&gt;
     *
     * @param clazz 目标Class
     * @return 返回父类中的泛型类型，如无法找到, 返回Object.class
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClassGenericType(final Class<?> clazz) {
        return (Class<T>) getClassGenericType(clazz, 0);
    }

    /**
     * 获取实际使用的类，获取被AOP过的真实类
     *
     * @param instance 目标对象
     * @return 返回实际使用的类
     */
    public static Class<?> getUserClass(Object instance) {
        Class<?> clazz = instance.getClass();
        if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && !Object.class.equals(superClass)) {
                return superClass;
            }
        }
        return clazz;
    }

    /**
     * 获取实体的字段
     *
     * @param entityClass 实体类型
     * @param fieldName   字段名称
     * @return 该字段名称对应的字段, 如果没有则返回null.
     */
    public static Field getField(Class<?> entityClass, String fieldName) {
        try {
            return entityClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
        }
        return null;
    }

    /**
     * 获取Class的所有字段，以及父类字段
     */
    public static Field[] getAllField(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        Class<?> tempClass = clazz;
        while (tempClass != null) {
            fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            tempClass = tempClass.getSuperclass();
        }
        Field[] result = new Field[fieldList.size()];
        return fieldList.toArray(result);
    }

    /**
     * 获取Class的所有Method，以及父类Method
     */
    public static Method[] getAllMethod(Class<?> clazz) {
        List<Method> methodList = new ArrayList<>();
        Class<?> tempClass = clazz;
        while (tempClass != null) {
            methodList.addAll(Arrays.asList(tempClass.getDeclaredMethods()));
            tempClass = tempClass.getSuperclass();
        }
        Method[] result = new Method[methodList.size()];
        return methodList.toArray(result);
    }

    public static Method getMethod(Class<?> clazz, String methodName) {
        return getMethod(clazz, methodName, false);
    }

    public static Method getMethod(Class<?> clazz, String methodName, boolean mustOnlyOne) {
        List<Method> methods = getMethods(clazz, methodName);
        String msg = String.format("class=%s 包含%s个 method=%s", clazz.getName(), methods.size(), methodName);
        return getMethod(methods, mustOnlyOne, msg);
    }

    public static List<Method> getMethods(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        return Arrays.stream(methods).filter(m -> Objects.equals(methodName, m.getName())).collect(Collectors.toList());
    }

    public static Method getStaticMethod(Class<?> clazz, String methodName) {
        return getStaticMethod(clazz, methodName, false);
    }

    public static Method getStaticMethod(Class<?> clazz, String methodName, boolean mustOnlyOne) {
        List<Method> methods = getStaticMethods(clazz, methodName);
        String msg = String.format("class=%s 包含%s个 static method=%s", clazz.getName(), methods.size(), methodName);
        return getMethod(methods, mustOnlyOne, msg);
    }

    public static List<Method> getStaticMethods(Class<?> clazz, String methodName) {
        return getMethods(clazz, methodName).stream()
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .collect(Collectors.toList());
    }

    private static Method getMethod(List<Method> methods, boolean mustOnlyOne, String errMsg) {
        if (mustOnlyOne) {
            Assert.isTrue(methods.size() == 1, errMsg);
            return methods.get(0);
        }
        Method method = null;
        if (!methods.isEmpty()) {
            method = methods.get(0);
        }
        if (methods.size() > 1) {
            log.warn(errMsg);
        }
        return method;
    }
}
