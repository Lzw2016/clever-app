package org.clever.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * bean实例化失败时引发异常。携带令人反感的bean类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:25 <br/>
 */
public class BeanInstantiationException extends FatalBeanException {
    /**
     * 有问题的bean类
     */
    private final Class<?> beanClass;
    /**
     * 有问题的构造函数
     */
    private final Constructor<?> constructor;
    /**
     * 用于bean构造的委托
     */
    private final Method constructingMethod;

    public BeanInstantiationException(Class<?> beanClass, String msg) {
        this(beanClass, msg, null);
    }

    public BeanInstantiationException(Class<?> beanClass, String msg, Throwable cause) {
        super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
        this.beanClass = beanClass;
        this.constructor = null;
        this.constructingMethod = null;
    }

    public BeanInstantiationException(Constructor<?> constructor, String msg, Throwable cause) {
        super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
        this.beanClass = constructor.getDeclaringClass();
        this.constructor = constructor;
        this.constructingMethod = null;
    }

    public BeanInstantiationException(Method constructingMethod, String msg, Throwable cause) {
        super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
        this.beanClass = constructingMethod.getReturnType();
        this.constructor = null;
        this.constructingMethod = constructingMethod;
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    public Constructor<?> getConstructor() {
        return this.constructor;
    }

    public Method getConstructingMethod() {
        return this.constructingMethod;
    }
}
