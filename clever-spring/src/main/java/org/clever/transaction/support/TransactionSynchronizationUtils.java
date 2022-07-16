package org.clever.transaction.support;

import org.clever.aop.scope.ScopedObject;
import org.clever.core.InfrastructureProxy;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 用于在所有当前注册的同步上触发特定{@link TransactionSynchronization}回调方法的实用程序方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:33 <br/>
 *
 * @see TransactionSynchronization
 * @see TransactionSynchronizationManager#getSynchronizations()
 */
public abstract class TransactionSynchronizationUtils {
    private static final Logger logger = LoggerFactory.getLogger(TransactionSynchronizationUtils.class);
    private static final boolean aopAvailable = ClassUtils.isPresent("org.clever.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());

    /**
     * 检查给定的资源事务管理器是否引用给定的（基础）资源工厂
     *
     * @see ResourceTransactionManager#getResourceFactory()
     * @see InfrastructureProxy#getWrappedObject()
     */
    public static boolean sameResourceFactory(ResourceTransactionManager tm, Object resourceFactory) {
        return unwrapResourceIfNecessary(tm.getResourceFactory()).equals(unwrapResourceIfNecessary(resourceFactory));
    }

    /**
     * 如有必要，展开给定的资源句柄；否则，按原样返回给定句柄。
     *
     * @see InfrastructureProxy#getWrappedObject()
     */
    public static Object unwrapResourceIfNecessary(Object resource) {
        Assert.notNull(resource, "Resource must not be null");
        Object resourceRef = resource;
        // unwrap infrastructure proxy
        if (resourceRef instanceof InfrastructureProxy) {
            resourceRef = ((InfrastructureProxy) resourceRef).getWrappedObject();
        }
        if (aopAvailable) {
            // now unwrap scoped proxy
            resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
        }
        return resourceRef;
    }

    /**
     * 触发 {@code flush} 回调所有当前注册的同步
     *
     * @throws RuntimeException 如果由刷新回调引发
     * @see TransactionSynchronization#flush()
     */
    public static void triggerFlush() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.flush();
        }
    }

    /**
     * 触发 {@code beforeCommit} 回调所有当前注册的同步。
     *
     * @param readOnly 事务是否定义为只读事务
     * @throws RuntimeException 如果由 {@code beforeCommit} 回调引发
     * @see TransactionSynchronization#beforeCommit(boolean)
     */
    public static void triggerBeforeCommit(boolean readOnly) {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.beforeCommit(readOnly);
        }
    }

    /**
     * 触发 {@code beforeCompletion} 回调所有当前注册的同步。
     *
     * @see TransactionSynchronization#beforeCompletion()
     */
    public static void triggerBeforeCompletion() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            try {
                synchronization.beforeCompletion();
            } catch (Throwable ex) {
                logger.debug("TransactionSynchronization.beforeCompletion threw exception", ex);
            }
        }
    }

    /**
     * 触发 {@code afterCommit} 回调所有当前注册的同步。
     *
     * @throws RuntimeException 如果由 {@code afterCommit} 回调引发
     * @see TransactionSynchronizationManager#getSynchronizations()
     * @see TransactionSynchronization#afterCommit()
     */
    public static void triggerAfterCommit() {
        invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());
    }

    /**
     * 实际调用给定TransactionSynchronization对象的 {@code afterCommit} 方法。
     *
     * @param synchronizations TransactionSynchronization对象的列表
     * @see TransactionSynchronization#afterCommit()
     */
    public static void invokeAfterCommit(List<TransactionSynchronization> synchronizations) {
        if (synchronizations != null) {
            for (TransactionSynchronization synchronization : synchronizations) {
                synchronization.afterCommit();
            }
        }
    }

    /**
     * 触发 {@code afterCompletion} 回调所有当前注册的同步。
     *
     * @param completionStatus 根据TransactionSynchronization接口中的常量的完成状态
     * @see TransactionSynchronizationManager#getSynchronizations()
     * @see TransactionSynchronization#afterCompletion(int)
     * @see TransactionSynchronization#STATUS_COMMITTED
     * @see TransactionSynchronization#STATUS_ROLLED_BACK
     * @see TransactionSynchronization#STATUS_UNKNOWN
     */
    public static void triggerAfterCompletion(int completionStatus) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        invokeAfterCompletion(synchronizations, completionStatus);
    }

    /**
     * 实际调用给定TransactionSynchronization对象的 {@code afterCompletion} 方法。
     *
     * @param synchronizations TransactionSynchronization对象的列表
     * @param completionStatus 根据TransactionSynchronization接口中的常量的完成状态
     * @see TransactionSynchronization#afterCompletion(int)
     * @see TransactionSynchronization#STATUS_COMMITTED
     * @see TransactionSynchronization#STATUS_ROLLED_BACK
     * @see TransactionSynchronization#STATUS_UNKNOWN
     */
    public static void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
        if (synchronizations != null) {
            for (TransactionSynchronization synchronization : synchronizations) {
                try {
                    synchronization.afterCompletion(completionStatus);
                } catch (Throwable ex) {
                    logger.debug("TransactionSynchronization.afterCompletion threw exception", ex);
                }
            }
        }
    }

    /**
     * 内部类，以避免对AOP模块的硬编码依赖。
     */
    private static class ScopedProxyUnwrapper {
        public static Object unwrapIfNecessary(Object resource) {
            if (resource instanceof ScopedObject) {
                return ((ScopedObject) resource).getTargetObject();
            } else {
                return resource;
            }
        }
    }
}
