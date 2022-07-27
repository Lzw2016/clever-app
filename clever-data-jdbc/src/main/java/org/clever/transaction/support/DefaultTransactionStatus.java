package org.clever.transaction.support;

import org.clever.transaction.NestedTransactionNotSupportedException;
import org.clever.transaction.SavepointManager;
import org.clever.util.Assert;

/**
 * 组织的默认实现{@link org.clever.transaction.TransactionStatus}接口，
 * 由{@link AbstractPlatformTransactionManager}使用。基于底层“事务对象”的概念。
 * <p>保存{@link AbstractPlatformTransactionManager}内部需要的所有状态信息，
 * 包括由具体事务管理器实现确定的通用事务对象。
 * <p>支持将与保存点相关的方法委派给实现{@link SavepointManager}接口的事务对象。
 * <p>注意：这不适用于其他PlatformTransactionManager实现，尤其不适用于测试环境中的模拟事务管理器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:29 <br/>
 *
 * @see AbstractPlatformTransactionManager
 * @see SavepointManager
 * @see #getTransaction
 * @see #createSavepoint
 * @see #rollbackToSavepoint
 * @see #releaseSavepoint
 */
public class DefaultTransactionStatus extends AbstractTransactionStatus {
    private final Object transaction;
    private final boolean newTransaction;
    private final boolean newSynchronization;
    private final boolean readOnly;
    private final boolean debug;
    private final Object suspendedResources;

    /**
     * 创建新的{@code DefaultTransactionStatus}实例
     *
     * @param transaction        可以保存内部事务实现状态的基础事务对象
     * @param newTransaction     如果交易是新的，则参与现有交易
     * @param newSynchronization 如果已为给定事务打开新事务同步
     * @param readOnly           事务是否标记为只读
     * @param debug              是否应为处理此事务启用调试日志记录？将其缓存在此处可以防止重复调用以询问日志记录系统是否应启用调试日志记录。
     * @param suspendedResources 本交易暂停的资源持有人（如有）
     */
    public DefaultTransactionStatus(Object transaction,
                                    boolean newTransaction,
                                    boolean newSynchronization,
                                    boolean readOnly,
                                    boolean debug,
                                    Object suspendedResources) {
        this.transaction = transaction;
        this.newTransaction = newTransaction;
        this.newSynchronization = newSynchronization;
        this.readOnly = readOnly;
        this.debug = debug;
        this.suspendedResources = suspendedResources;
    }

    /**
     * 返回基础事务对象
     *
     * @throws IllegalStateException 如果没有激活的事务
     */
    public Object getTransaction() {
        Assert.state(this.transaction != null, "No transaction active");
        return this.transaction;
    }

    /**
     * 返回是否有实际事务处于活动状态
     */
    public boolean hasTransaction() {
        return (this.transaction != null);
    }

    @Override
    public boolean isNewTransaction() {
        return (hasTransaction() && this.newTransaction);
    }

    /**
     * 如果已为此事务打开新的事务同步，则返回
     */
    public boolean isNewSynchronization() {
        return this.newSynchronization;
    }

    /**
     * 如果此事务定义为只读事务，则返回
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * 返回是否调试此事务的进度{@link AbstractPlatformTransactionManager}将其用作优化，以防止重复调用记录器。
     * {@code logger.isDebugEnabled()}。并非真正用于客户端代码。
     */
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * 返回已为此事务暂停的资源的持有人（如果有）
     */
    public Object getSuspendedResources() {
        return this.suspendedResources;
    }

    //---------------------------------------------------------------------
    // Enable functionality through underlying transaction object
    //---------------------------------------------------------------------

    /**
     * 通过检查事务对象确定仅回滚标志，前提是后者实现了{@link SmartTransactionObject}接口。
     * <p>如果全局事务本身仅由事务协调器标记为回滚，则返回true，例如在超时的情况下。
     *
     * @see SmartTransactionObject#isRollbackOnly()
     */
    @Override
    public boolean isGlobalRollbackOnly() {
        return ((this.transaction instanceof SmartTransactionObject) && ((SmartTransactionObject) this.transaction).isRollbackOnly());
    }

    /**
     * 此实现公开了基础事务对象（如果有）的{@link SavepointManager}接口
     *
     * @throws NestedTransactionNotSupportedException 如果不支持保存点
     * @see #isTransactionSavepointManager()
     */
    @Override
    protected SavepointManager getSavepointManager() {
        Object transaction = this.transaction;
        if (!(transaction instanceof SavepointManager)) {
            throw new NestedTransactionNotSupportedException(
                    "Transaction object [" + this.transaction + "] does not support savepoints"
            );
        }
        return (SavepointManager) transaction;
    }

    /**
     * 返回基础事务是否实现了{@link SavepointManager}接口，因此是否支持保存点
     *
     * @see #getTransaction()
     * @see #getSavepointManager()
     */
    public boolean isTransactionSavepointManager() {
        return (this.transaction instanceof SavepointManager);
    }

    /**
     * 将刷新委托给事务对象，前提是后者实现了{@link SmartTransactionObject}接口
     *
     * @see SmartTransactionObject#flush()
     */
    @Override
    public void flush() {
        if (this.transaction instanceof SmartTransactionObject) {
            ((SmartTransactionObject) this.transaction).flush();
        }
    }
}
