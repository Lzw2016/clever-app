package org.clever.core;

import org.clever.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 用于检测Kotlin的存在和识别Kotlin类型
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 11:19 <br/>
 */
@SuppressWarnings("unchecked")
public abstract class KotlinDetector {
    private static final Class<? extends Annotation> kotlinMetadata;
    private static final boolean kotlinReflectPresent;

    static {
        Class<?> metadata;
        ClassLoader classLoader = KotlinDetector.class.getClassLoader();
        try {
            metadata = ClassUtils.forName("kotlin.Metadata", classLoader);
        } catch (ClassNotFoundException ex) {
            // Kotlin API not available - no Kotlin support
            metadata = null;
        }
        kotlinMetadata = (Class<? extends Annotation>) metadata;
        kotlinReflectPresent = ClassUtils.isPresent("kotlin.reflect.full.KClasses", classLoader);
    }

    /**
     * 确定Kotlin是否存在(kotlin.Metadata)
     */
    public static boolean isKotlinPresent() {
        return (kotlinMetadata != null);
    }

    /**
     * 确定是否存在Kotlin反射(kotlin.reflect)
     */
    public static boolean isKotlinReflectPresent() {
        return kotlinReflectPresent;
    }

    /**
     * 确定给定类是否为Kotlin类型(其上存在Kotlin元数据)
     */
    public static boolean isKotlinType(Class<?> clazz) {
        return (kotlinMetadata != null && clazz.getDeclaredAnnotation(kotlinMetadata) != null);
    }

    /**
     * 如果方法是挂起函数，则返回true
     */
    public static boolean isSuspendingFunction(Method method) {
        if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
            Class<?>[] types = method.getParameterTypes();
            return types.length > 0 && "kotlin.coroutines.Continuation".equals(types[types.length - 1].getName());
        }
        return false;
    }
}
