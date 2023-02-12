package org.clever.transaction.support;

import org.clever.transaction.PlatformTransactionManager;

/**
 * {@link PlatformTransactionManager}接口的扩展，指示在单个目标资源上操作的本机资源事务管理器。
 * 此类事务管理器与JTA事务管理器的不同之处在于，它们不对开放的资源数量使用XA事务登记，而是侧重于利用单个目标资源的本机功能和简单性。
 * <p>此接口主要用于事务管理器的抽象内省，向客户机提示他们已经获得了什么类型的事务管理器，以及事务管理器正在操作什么具体资源。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:21 <br/>
 *
 * @see TransactionSynchronizationManager
 */
public interface ResourceTransactionManager extends PlatformTransactionManager {
    /**
     * 返回此事务管理器操作的资源工厂，例如JDBC数据源或JMS连接工厂。
     * <p>此目标资源工厂通常用作{@link TransactionSynchronizationManager}每个线程的资源绑定
     *
     * @return 目标资源工厂(从不为null)
     * @see TransactionSynchronizationManager#bindResource
     * @see TransactionSynchronizationManager#getResource
     */
    Object getResourceFactory();
}
