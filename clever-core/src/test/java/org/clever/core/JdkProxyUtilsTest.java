package org.clever.core;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.proxy.JdkProxyFactory;
import org.clever.core.reflection.ReflectionsUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/13 21:57 <br/>
 */
@Slf4j
public class JdkProxyUtilsTest {

    interface A {
        void a(String a);
    }

    interface B extends A {
        void b(String b);
    }

    interface C extends B {
        void c(String c);
    }

    static class AImpl implements A {
        @Override
        public void a(String a) {
            log.info("CImpl a->{}", a);
        }
    }

    static class CImpl implements C {
        @Override
        public void a(String a) {
            log.info("CImpl a->{}", a);
        }

        @Override
        public void b(String b) {
            log.info("CImpl b->{}", b);
        }

        @Override
        public void c(String c) {
            log.info("CImpl c->{}", c);
        }

        public void d(String d) {
            log.info("CImpl d->{}", d);
        }
    }

    @Test
    public void t01() {
        InvocationHandler handler = new InvocationHandler() {
            final CImpl cImpl = new CImpl();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                log.info("---> JDK前置处理 | method={}", method);
                Object obj = method.invoke(cImpl, args);
                log.info("JDK后置处理 <---");
                return obj;
            }
        };
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        log.info("###-----------------------------------------------------------------");
        C proxy = (C) Proxy.newProxyInstance(loader, new Class[]{C.class}, handler);
        // noinspection ResultOfMethodCallIgnored
        proxy.toString();
        proxy.a("aaa");
        proxy.b("bbb");
        proxy.c("ccc");
        try {
            ReflectionsUtils.invokeMethodByName(proxy, "d", new Object[]{"ddd"});
        } catch (Exception e) {
            // 在对象[org.clever.core.JdkProxyUtilsTest$CImpl@429bd883]中找不到方法 [d]
            log.warn(e.getMessage());
        }
        log.info("###-----------------------------------------------------------------");
        A proxyA = (A) Proxy.newProxyInstance(loader, new Class[]{A.class}, handler);
        proxyA.a("aaa");
        try {
            ReflectionsUtils.invokeMethodByName(proxyA, "b", new Object[]{"bbb"});
        } catch (Exception e) {
            // 在对象[org.clever.core.JdkProxyUtilsTest$CImpl@4d49af10]中找不到方法 [b]
            log.warn(e.getMessage());
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void t02() {
        InvocationHandler handler = new InvocationHandler() {
            final AImpl aImpl = new AImpl();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                log.info("---> JDK前置处理 | method={}", method);
                Object obj = method.invoke(aImpl, args);
                log.info("JDK后置处理 <---");
                return obj;
            }
        };
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        B proxy = (B) Proxy.newProxyInstance(loader, new Class[]{B.class}, handler);
        proxy.a("aaa");
        try {
            proxy.b("bbb");
        } catch (Exception e) {
            // object is not an instance of declaring class
            log.warn(e.getMessage());
        }
        log.info("proxy instanceof A --> {}", proxy instanceof A); // true
        log.info("proxy instanceof B --> {}", proxy instanceof B); // true
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void t03() {
        final AImpl aImpl = new AImpl();
        JdkProxyFactory proxyFactory = new JdkProxyFactory(aImpl);
        B proxy = proxyFactory.addInterface(B.class)
            .setInterceptor((rawObj, proxy1, method, args) -> {
                log.info("---> JDK前置处理 | method={}", method);
                Object obj = method.invoke(aImpl, args);
                log.info("JDK后置处理 <---");
                return obj;
            })
            .createProxy();
        proxy.a("aaa");
        try {
            proxy.b("bbb");
        } catch (Exception e) {
            // object is not an instance of declaring class
            log.warn(e.getMessage());
        }
        log.info("proxy instanceof A --> {}", proxy instanceof A); // true
        log.info("proxy instanceof B --> {}", proxy instanceof B); // true
    }
}
