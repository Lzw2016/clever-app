package org.clever.task.core.support;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.AppContextHolder;
import org.clever.core.tuples.TupleTwo;
import org.clever.task.core.job.JobContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/25 19:09 <br/>
 */
@Slf4j
public class ClassMethodLoader {
    private static final ConcurrentMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * 加载函数对象,不存在返回 null {@code TupleTwo<class, method>}
     *
     * @param className  class 全名
     * @param methodName 函数名
     */
    public static TupleTwo<Class<?>, Method> getMethod(String className, String methodName) {
        boolean useCache = false;
        // 支持热重载
        ClassLoader classLoader = AppContextHolder.getBean("hotReloadClassLoader", ClassLoader.class);
        if (classLoader == null) {
            useCache = true;
            classLoader = ClassMethodLoader.class.getClassLoader();
        }
        return getMethod(className, methodName, classLoader, useCache);
    }

    /**
     * 加载函数对象,不存在返回 null {@code TupleTwo<class, method>}
     *
     * @param className   class 全名
     * @param methodName  函数名
     * @param classLoader ClassLoader对象
     * @param useCache    是否使用缓存
     */
    public static TupleTwo<Class<?>, Method> getMethod(String className, String methodName, ClassLoader classLoader, boolean useCache) {
        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
        final String methodKey = String.format("%s#%s", className, methodName);
        Method method;
        if (useCache) {
            method = METHOD_CACHE.get(methodKey);
            if (method == null || !Objects.equals(clazz.getClassLoader(), classLoader)) {
                synchronized (METHOD_CACHE) {
                    // 二次确认
                    method = METHOD_CACHE.get(methodKey);
                    if (method == null || !Objects.equals(clazz.getClassLoader(), classLoader)) {
                        method = loadMethod(clazz, methodName);
                        if (method != null) {
                            METHOD_CACHE.put(methodKey, method);
                        }
                    }
                }
            }
        } else {
            method = loadMethod(clazz, methodName);
        }
        if (method == null) {
            return null;
        }
        return TupleTwo.creat(clazz, method);
    }

    private static Method loadMethod(Class<?> clazz, String methodName) {
        Method method = getAccessibleMethodByName(clazz, methodName, JobContext.class);
        if (method == null) {
            method = getAccessibleMethodByName(clazz, methodName);
        }
        return method;
    }

    private static Method getAccessibleMethodByName(Class<?> searchType, String methodName, Class<?>... parameterTypes) {
        while (searchType != Object.class) {
            Method method = null;
            try {
                method = searchType.getDeclaredMethod(methodName, parameterTypes);
            } catch (Throwable ignored) {
            }
            if (method != null) {
                // 强制设置方法可以访问(public)
                makeAccessible(method);
                return method;
            }
            // 获取父类类型，继续查找方法
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    private static void makeAccessible(Method method) {
        if (Modifier.isPublic(method.getModifiers()) && Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            return;
        }
        method.setAccessible(true);
    }
}
