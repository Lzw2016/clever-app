//package org.clever.groovy.utils;
//
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.clever.core.reflection.ReflectionsUtils;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2022/03/07 09:42 <br/>
// */
//@Slf4j
//public class GroovyInvokeUtils {
//    private static volatile YvanGroovyHandleMethodResolver yvanGroovyHandleMethodResolver;
//
//    @SuppressWarnings("UnusedReturnValue")
//    private static YvanGroovyHandleMethodResolver getYvanGroovyHandleMethodResolver() {
//        if (yvanGroovyHandleMethodResolver == null) {
//            yvanGroovyHandleMethodResolver = SpringContextHolder.getBean(YvanGroovyHandleMethodResolver.class);
//        }
//        return yvanGroovyHandleMethodResolver;
//    }
//
//    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
//    @SneakyThrows
//    public static <T> T invoke(String className, String methodName, Object... args) {
//        try {
//            if (yvanGroovyHandleMethodResolver == null) {
//                getYvanGroovyHandleMethodResolver();
//            }
//            Class<?> clazz = yvanGroovyHandleMethodResolver.loadGroovyClass(className);
//            Method[] methods = clazz.getDeclaredMethods();
//            List<Method> methodList = Arrays.stream(methods)
//                    .filter(m -> Objects.equals(methodName, m.getName()))
//                    .filter(m -> Modifier.isStatic(m.getModifiers()))
//                    .collect(Collectors.toList());
//            if (methodList.isEmpty()) {
//                throw new RuntimeException("class=" + className + "中不包含method=" + methodName);
//            }
//            Method method;
//            method = methodList.get(0);
//            if (methodList.size() > 1) {
//                log.warn("class={} 包含{}个 method={}", className, methodList.size(), methodName);
//            }
//            ReflectionsUtils.makeAccessible(method);
//            return (T) method.invoke(null, args);
//        } catch (Exception e) {
//            // log.error(e.getMessage(), e);
//            throw e;
//        }
//    }
//
//    public static Class<?> loadGroovyClass(String className) {
//        if (yvanGroovyHandleMethodResolver == null) {
//            getYvanGroovyHandleMethodResolver();
//        }
//        return yvanGroovyHandleMethodResolver.loadGroovyClass(className);
//    }
//}
