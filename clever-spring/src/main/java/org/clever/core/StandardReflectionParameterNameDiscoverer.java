package org.clever.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * {@link ParameterNameDiscoverer}实现，它使用JDK 8的反射功能来内省参数名称(基于"-parameters"编译器标志)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:23 <br/>
 */
public class StandardReflectionParameterNameDiscoverer implements ParameterNameDiscoverer {
    @Override
    public String[] getParameterNames(Method method) {
        return getParameterNames(method.getParameters());
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        return getParameterNames(ctor.getParameters());
    }

    private String[] getParameterNames(Parameter[] parameters) {
        String[] parameterNames = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (!param.isNamePresent()) {
                return null;
            }
            parameterNames[i] = param.getName();
        }
        return parameterNames;
    }
}
