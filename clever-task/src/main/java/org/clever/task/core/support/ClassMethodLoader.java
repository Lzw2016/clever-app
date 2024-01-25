package org.clever.task.core.support;

import org.clever.core.tuples.TupleTwo;
import org.clever.task.core.GlobalConstant;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/25 19:09 <br/>
 */
public class ClassMethodLoader {
    private static final ConcurrentMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);

    public static TupleTwo<Class<?>, Method> getMethod(String className, String classMethod, ClassLoader classLoader) throws ClassNotFoundException {
        // TODO 不存在返回null
        final Class<?> clazz = classLoader.loadClass(className);
        final String cacheKey = String.format("%s#%s", className, classMethod);
        // TODO 热重载场景???
        Method method = METHOD_CACHE.computeIfAbsent(cacheKey, key -> {
            Method tmp = getAccessibleMethodByName(clazz, classMethod, true);
            if (tmp == null) {
                tmp = getAccessibleMethodByName(clazz, classMethod, false);
            }
            return tmp;
        });
        return TupleTwo.creat(clazz, method);
    }

    public static TupleTwo<Class<?>, Method>  getMethod(String className, String classMethod) throws ClassNotFoundException {
        return getMethod(className, classMethod, Thread.currentThread().getContextClassLoader());
    }

    private static Method getAccessibleMethodByName(Class<?> searchType, String methodName, boolean hasParameter) {
        while (searchType != Object.class) {
            Method[] methods = searchType.getDeclaredMethods();
            for (Method method : methods) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (hasParameter) {
                    // 有参数，参数必须是 LinkedHashMap
                    if (parameterTypes.length != 1 || !Map.class.isAssignableFrom(parameterTypes[0])) {
                        continue;
                    }
                } else {
                    // 无参数
                    if (parameterTypes.length > 0) {
                        continue;
                    }
                }
                if (method.getName().equals(methodName)) {
                    // 强制设置方法可以访问(public)
                    makeAccessible(method);
                    return method;
                }
            }
            // 获取父类类型，继续查找方法
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    private static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }
}
