package org.clever.core.proxy;

import java.lang.reflect.Method;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/19 22:58 <br/>
 */
@FunctionalInterface
public interface MethodInterceptor {
    /**
     * @param rawObj 被代理的原始对象
     * @param proxy  调用该方法的代理实例
     * @param method 与在代理实例上调用的接口方法对应的 Method 实例。 Method 对象的声明类将是声明该方法的接口，该接口可能是代理类通过其继承该方法的代理接口的超接口
     * @param args   一个对象数组，包含在代理实例的方法调用中传递的参数值，如果接口方法不带参数，则为 null。原始类型的参数被包装在适当的原始包装器类的实例中，例如 java.lang.Integer 或 java.lang.Boolean
     */
    Object invoke(Object rawObj, Object proxy, Method method, Object[] args) throws Throwable;
}
