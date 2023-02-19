package org.clever.core.proxy;

/**
 * JDK 动态代理工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/13 21:22 <br/>
 */
public abstract class JdkProxyUtils {
    public static JdkProxyFactory create(Object target) {
        return new JdkProxyFactory(target);
    }

    public static JdkProxyFactory create() {
        return new JdkProxyFactory();
    }
}
