package org.clever.data.redis.core;

import org.clever.data.redis.connection.RedisConnection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 禁止对 {@link RedisConnection} 进行关闭调用的调用处理程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:15 <br/>
 *
 * @see RedisConnection#close()
 */
class CloseSuppressingInvocationHandler implements InvocationHandler {
    private static final String CLOSE = "close";
    private static final String HASH_CODE = "hashCode";
    private static final String EQUALS = "equals";

    private final Object target;

    public CloseSuppressingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case EQUALS:
                // Only consider equal when proxies are identical.
                return (proxy == args[0]);
            case HASH_CODE:
                // Use hashCode of PersistenceManager proxy.
                return System.identityHashCode(proxy);
            case CLOSE:
                // Handle close method: suppress, not valid.
                return null;
        }
        // Invoke method on target RedisConnection.
        try {
            return method.invoke(this.target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
