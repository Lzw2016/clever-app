package org.clever.core.proxy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.clever.core.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/19 22:53 <br/>
 */
@EqualsAndHashCode
@ToString
@Getter
public class JdkProxyFactory {
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Object target;
    private final List<Class<?>> interfaces = new ArrayList<>();
    private InvocationHandler interceptor;

    public JdkProxyFactory() {
    }

    public JdkProxyFactory(Object target) {
        this.target = target;
    }

    public JdkProxyFactory setClassLoader(ClassLoader classLoader) {
        Assert.notNull(classLoader, "参数 classLoader 不能为 null");
        this.classLoader = classLoader;
        return this;
    }

    /**
     * 设置被代理的对象
     */
    public JdkProxyFactory setTarget(Object target) {
        Assert.notNull(target, "参数 target 不能为 null");
        this.target = target;
        return this;
    }

    /**
     * 添加一个新的代理接口
     */
    public JdkProxyFactory addInterface(Class<?> intf) {
        Assert.notNull(intf, "参数 intf 不能为 null");
        Assert.isTrue(intf.isInterface(), "参数 intf 必须是 interface, intf当前类型: " + intf.getName() + "");
        this.interfaces.add(intf);
        return this;
    }

    /**
     * 添加多个新的代理接口
     */
    public JdkProxyFactory addAllInterface(Class<?>[] intfs) {
        if (intfs != null) {
            for (Class<?> intf : intfs) {
                addInterface(intf);
            }
        }
        return this;
    }

    /**
     * 添加多个新的代理接口
     */
    public JdkProxyFactory addAllInterface(Collection<Class<?>> intfs) {
        if (intfs != null) {
            for (Class<?> intf : intfs) {
                addInterface(intf);
            }
        }
        return this;
    }

    /**
     * 设置代理逻辑
     */
    public JdkProxyFactory setInterceptor(InvocationHandler interceptor) {
        Assert.notNull(interceptor, "参数 interceptor 不能为 null");
        this.interceptor = interceptor;
        return this;
    }

    /**
     * 设置代理逻辑
     */
    public JdkProxyFactory setInterceptor(MethodInterceptor interceptor) {
        Assert.notNull(interceptor, "参数 interceptor 不能为 null");
        this.interceptor = convertHandler(interceptor);
        return this;
    }

    /**
     * 创建新的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy() {
        Assert.notNull(interceptor, "未设置 interceptor");
        return (T) Proxy.newProxyInstance(
                classLoader,
                interfaces.toArray(new Class[0]),
                interceptor
        );
    }

    /**
     * 创建新的代理对象
     */
    public <T> T createProxy(Object target) {
        Assert.notNull(target, "未设置 target");
        this.target = target;
        return createProxy();
    }

    /**
     * 创建新的代理对象
     */
    public <T> T createProxy(InvocationHandler interceptor) {
        Assert.notNull(interceptor, "未设置 interceptor");
        this.interceptor = interceptor;
        return createProxy();
    }

    /**
     * 创建新的代理对象
     */
    public <T> T createProxy(MethodInterceptor interceptor) {
        Assert.notNull(interceptor, "未设置 interceptor");
        this.interceptor = convertHandler(interceptor);
        return createProxy();
    }

    private InvocationHandler convertHandler(MethodInterceptor interceptor) {
        return (proxy, method, args) -> interceptor.invoke(target, proxy, method, args);
    }
}
