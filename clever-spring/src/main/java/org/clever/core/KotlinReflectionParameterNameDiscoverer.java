package org.clever.core;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link ParameterNameDiscoverer}实现，它使用Kotlin的反射功能来内省参数名称。
 * 与{@link StandardReflectionParameterNameDiscoverer}相比，它还允许在不需要Java 8参数编译器标志的情况下确定接口参数名称
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:23 <br/>
 */
public class KotlinReflectionParameterNameDiscoverer implements ParameterNameDiscoverer {
    @Override
    public String[] getParameterNames(Method method) {
        if (!KotlinDetector.isKotlinType(method.getDeclaringClass())) {
            return null;
        }
        try {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
            return (function != null ? getParameterNames(function.getParameters()) : null);
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        if (ctor.getDeclaringClass().isEnum() || !KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
            return null;
        }
        try {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(ctor);
            return (function != null ? getParameterNames(function.getParameters()) : null);
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    private String[] getParameterNames(List<KParameter> parameters) {
        List<KParameter> filteredParameters = parameters
                .stream()
                // Extension receivers of extension methods must be included as they appear as normal method parameters in Java
                .filter(p -> KParameter.Kind.VALUE.equals(p.getKind()) || KParameter.Kind.EXTENSION_RECEIVER.equals(p.getKind()))
                .collect(Collectors.toList());
        String[] parameterNames = new String[filteredParameters.size()];
        for (int i = 0; i < filteredParameters.size(); i++) {
            KParameter parameter = filteredParameters.get(i);
            // extension receivers are not explicitly named, but require a name for Java interoperability
            // $receiver is not a valid Kotlin identifier, but valid in Java, so it can be used here
            String name = KParameter.Kind.EXTENSION_RECEIVER.equals(parameter.getKind()) ? "$receiver" : parameter.getName();
            if (name == null) {
                return null;
            }
            parameterNames[i] = name;
        }
        return parameterNames;
    }
}
