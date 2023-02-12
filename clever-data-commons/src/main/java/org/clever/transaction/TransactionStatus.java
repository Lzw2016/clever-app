package org.clever.transaction;

import java.io.Flushable;

/**
 * 事务状态的表示。
 * <p>事务代码可以使用它来检索状态信息，并以编程方式请求回滚（而不是引发导致隐式回滚的异常）。
 * <p>包括{@link SavepointManager}接口，以提供对存储点管理工具的访问。
 * 请注意，只有在底层事务管理器支持的情况下，保存点管理才可用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:17 <br/>
 *
 * @see #setRollbackOnly()
 * @see PlatformTransactionManager#getTransaction
 */
public interface TransactionStatus extends TransactionExecution, SavepointManager, Flushable {
    /**
     * 返回此事务是否在内部携带保存点，即是否已基于保存点创建为嵌套事务。
     * <p>此方法主要用于诊断目的，还有{@link #isNewTransaction()}。
     * 对于自定义保存点的编程处理，请使用{@link SavepointManager}提供的操作。
     *
     * @see #isNewTransaction()
     * @see #createSavepoint()
     * @see #rollbackToSavepoint(Object)
     * @see #releaseSavepoint(Object)
     */
    boolean hasSavepoint();

    /**
     * 如果适用，将底层会话刷新到数据存储：例如，所有受影响的Hibernate/JPA会话。
     * <p>这实际上只是一个提示，如果底层事务管理器没有flush概念，那么这可能是一个no-op。
     * 刷新信号可能应用于主资源或事务同步，具体取决于基础资源。
     */
    @Override
    void flush();
}
