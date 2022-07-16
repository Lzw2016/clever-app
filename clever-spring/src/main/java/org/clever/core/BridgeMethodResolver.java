package org.clever.core;

import org.clever.util.ClassUtils;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.ReflectionUtils;
import org.clever.util.ReflectionUtils.MethodFilter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用于将合成桥接方法解析为要桥接的方法的工具类<br/>
 * 给定一个合成桥接方法，该方法返回要桥接的方法。
 * 编译器可以在扩展其方法具有参数化参数的参数化类型时创建桥接方法。
 * 在运行时调用期间，可以通过反射调用或使用桥接方法。
 * 当试图定位方法上的注释时，明智的做法是适当地检查桥接方法并找到桥接方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:55 <br/>
 */
public final class BridgeMethodResolver {
    private static final Map<Method, Method> cache = new ConcurrentReferenceHashMap<>();

    private BridgeMethodResolver() {
    }

    /**
     * 找到提供的桥接方法的原始方法。
     * 在非桥接方法实例中调用此方法是安全的。
     * 在这种情况下，提供的方法实例将直接返回给调用方。
     * 调用方在调用此方法之前无需检查桥接
     */
    public static Method findBridgedMethod(Method bridgeMethod) {
        if (!bridgeMethod.isBridge()) {
            return bridgeMethod;
        }
        Method bridgedMethod = cache.get(bridgeMethod);
        if (bridgedMethod == null) {
            // Gather all methods with matching name and parameter size.
            List<Method> candidateMethods = new ArrayList<>();
            MethodFilter filter = candidateMethod -> isBridgedCandidateFor(candidateMethod, bridgeMethod);
            ReflectionUtils.doWithMethods(bridgeMethod.getDeclaringClass(), candidateMethods::add, filter);
            if (!candidateMethods.isEmpty()) {
                bridgedMethod = candidateMethods.size() == 1 ? candidateMethods.get(0) : searchCandidates(candidateMethods, bridgeMethod);
            }
            if (bridgedMethod == null) {
                // A bridge method was passed in but we couldn't find the bridged method.
                // Let's proceed with the passed-in method and hope for the best...
                bridgedMethod = bridgeMethod;
            }
            cache.put(bridgeMethod, bridgedMethod);
        }
        return bridgedMethod;
    }

    /**
     * 如果提供的'{@code candidateMethod}'可以被视为由提供的桥接方法桥接的方法的验证候选，则返回true。
     * 此方法执行廉价的检查，并可用于快速筛选一组可能的匹配项
     */
    private static boolean isBridgedCandidateFor(Method candidateMethod, Method bridgeMethod) {
        return !candidateMethod.isBridge()
                && !candidateMethod.equals(bridgeMethod)
                && candidateMethod.getName().equals(bridgeMethod.getName())
                && candidateMethod.getParameterCount() == bridgeMethod.getParameterCount();
    }

    /**
     * 在给定候选对象中搜索桥接方法
     *
     * @param candidateMethods 候选方法列表
     * @param bridgeMethod     桥接方法
     */
    private static Method searchCandidates(List<Method> candidateMethods, Method bridgeMethod) {
        if (candidateMethods.isEmpty()) {
            return null;
        }
        Method previousMethod = null;
        boolean sameSig = true;
        for (Method candidateMethod : candidateMethods) {
            if (isBridgeMethodFor(bridgeMethod, candidateMethod, bridgeMethod.getDeclaringClass())) {
                return candidateMethod;
            } else if (previousMethod != null) {
                sameSig = sameSig && Arrays.equals(candidateMethod.getGenericParameterTypes(), previousMethod.getGenericParameterTypes());
            }
            previousMethod = candidateMethod;
        }
        return (sameSig ? candidateMethods.get(0) : null);
    }

    /**
     * 确定桥接方法是否为提供的候选方法的桥接
     */
    static boolean isBridgeMethodFor(Method bridgeMethod, Method candidateMethod, Class<?> declaringClass) {
        if (isResolvedTypeMatch(candidateMethod, bridgeMethod, declaringClass)) {
            return true;
        }
        Method method = findGenericDeclaration(bridgeMethod);
        return (method != null && isResolvedTypeMatch(method, candidateMethod, declaringClass));
    }

    /**
     * 如果提供的泛型方法和具体方法的类型签名在针对declaringType解析所有类型后都相等，则返回true，否则返回false
     */
    private static boolean isResolvedTypeMatch(Method genericMethod, Method candidateMethod, Class<?> declaringClass) {
        Type[] genericParameters = genericMethod.getGenericParameterTypes();
        if (genericParameters.length != candidateMethod.getParameterCount()) {
            return false;
        }
        Class<?>[] candidateParameters = candidateMethod.getParameterTypes();
        for (int i = 0; i < candidateParameters.length; i++) {
            ResolvableType genericParameter = ResolvableType.forMethodParameter(genericMethod, i, declaringClass);
            Class<?> candidateParameter = candidateParameters[i];
            if (candidateParameter.isArray()) {
                // An array type: compare the component type.
                if (!candidateParameter.getComponentType().equals(genericParameter.getComponentType().toClass())) {
                    return false;
                }
            }
            // A non-array type: compare the type itself.
            if (!ClassUtils.resolvePrimitiveIfNecessary(candidateParameter).equals(ClassUtils.resolvePrimitiveIfNecessary(genericParameter.toClass()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 搜索其擦除签名与提供的桥接方法的签名匹配的泛型方法声明
     *
     * @throws IllegalStateException 如果找不到泛型声明
     */
    private static Method findGenericDeclaration(Method bridgeMethod) {
        // Search parent types for method that has same signature as bridge.
        Class<?> superclass = bridgeMethod.getDeclaringClass().getSuperclass();
        while (superclass != null && Object.class != superclass) {
            Method method = searchForMatch(superclass, bridgeMethod);
            if (method != null && !method.isBridge()) {
                return method;
            }
            superclass = superclass.getSuperclass();
        }

        Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bridgeMethod.getDeclaringClass());
        return searchInterfaces(interfaces, bridgeMethod);
    }

    private static Method searchInterfaces(Class<?>[] interfaces, Method bridgeMethod) {
        for (Class<?> ifc : interfaces) {
            Method method = searchForMatch(ifc, bridgeMethod);
            if (method != null && !method.isBridge()) {
                return method;
            } else {
                method = searchInterfaces(ifc.getInterfaces(), bridgeMethod);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * 如果提供的类有一个声明的方法，其签名与提供的方法的签名匹配，则返回此匹配的方法，否则返回null
     */
    private static Method searchForMatch(Class<?> type, Method bridgeMethod) {
        try {
            return type.getDeclaredMethod(bridgeMethod.getName(), bridgeMethod.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * 比较桥接方法和桥接方法的签名<br/>
     * 如果参数和返回类型相同，则这是Java 6中引入的“可见性”桥接方法，用于修复 https://bugs.java.com/view_bug.do?bug_id=6342411<br/>
     * 另请参见: https://stas-blogspot.blogspot.com/2010/03/java-bridge-methods-explained.html<br/>
     *
     * @return 签名是否匹配
     */
    public static boolean isVisibilityBridgeMethodPair(Method bridgeMethod, Method bridgedMethod) {
        if (bridgeMethod == bridgedMethod) {
            return true;
        }
        return bridgeMethod.getReturnType().equals(bridgedMethod.getReturnType())
                && bridgeMethod.getParameterCount() == bridgedMethod.getParameterCount()
                && Arrays.equals(bridgeMethod.getParameterTypes(), bridgedMethod.getParameterTypes());
    }
}
