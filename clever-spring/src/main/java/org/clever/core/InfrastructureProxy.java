package org.clever.core;

/**
 * 由透明资源代理实现的接口，需要将其视为与基础资源相等，例如用于一致的查找键比较。
 * 请注意，此接口确实暗示了此类特殊语义，并且不构成通用的mixin！
 *
 * <p>这样的包装将自动展开，以便在中进行关键比较
 * {@link org.clever.transaction.support.TransactionSynchronizationManager}.
 *
 * <p>只有完全透明的代理（例如用于重定向或服务查找）才应该实现此接口。
 * 使用新行为装饰目标对象的代理（如AOP代理）在此不合格！
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:34 <br/>
 *
 * @see org.clever.transaction.support.TransactionSynchronizationManager
 */
public interface InfrastructureProxy {
    /**
     * 返回基础资源（从不{@code null}）
     */
    Object getWrappedObject();
}
