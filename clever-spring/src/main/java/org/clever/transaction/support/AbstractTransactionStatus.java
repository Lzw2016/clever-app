package org.clever.transaction.support;

import org.clever.transaction.*;

/**
 * 的抽象基实现 {@link org.clever.transaction.TransactionStatus} 接口.
 * <p>预实现仅本地回滚和已完成标志的处理，以及对底层{@link org.clever.transaction.SavepointManager}的委托。
 * 还提供了在事务中保存保存点的选项。
 * <p>不假定任何特定的内部事务处理，例如底层事务对象，并且没有事务同步机制。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:26 <br/>
 *
 * @see #setRollbackOnly()
 * @see #isRollbackOnly()
 * @see #setCompleted()
 * @see #isCompleted()
 * @see #getSavepointManager()
 * @see DefaultTransactionStatus
 */
public abstract class AbstractTransactionStatus implements TransactionStatus {
    private boolean rollbackOnly = false;
    private boolean completed = false;
    private Object savepoint;

    //---------------------------------------------------------------------
    // Implementation of TransactionExecution
    //---------------------------------------------------------------------

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    /**
     * 通过检查此TransactionStatus的本地仅回滚标志和基础事务的全局仅回滚标志（如果有），确定仅回滚标志。
     *
     * @see #isLocalRollbackOnly()
     * @see #isGlobalRollbackOnly()
     */
    @Override
    public boolean isRollbackOnly() {
        return (isLocalRollbackOnly() || isGlobalRollbackOnly());
    }

    /**
     * 通过检查此TransactionStatus确定仅回滚标志。
     * <p>仅当应用程序对此TransactionStatus对象调用{@code setRollbackOnly}时，才会返回“true”。
     */
    public boolean isLocalRollbackOnly() {
        return this.rollbackOnly;
    }

    /**
     * 用于确定基础事务（如果有）的仅全局回滚标志的模板方法。
     * <p>此实现始终返回 {@code false}.
     */
    public boolean isGlobalRollbackOnly() {
        return false;
    }

    /**
     * 将此事务标记为已完成，即已提交或已回滚。
     */
    public void setCompleted() {
        this.completed = true;
    }

    @Override
    public boolean isCompleted() {
        return this.completed;
    }

    //---------------------------------------------------------------------
    // Handling of current savepoint state
    //---------------------------------------------------------------------

    @Override
    public boolean hasSavepoint() {
        return (this.savepoint != null);
    }

    /**
     * 为此事务设置保存点。适用于 PROPAGATION_NESTED.
     *
     * @see org.clever.transaction.TransactionDefinition#PROPAGATION_NESTED
     */
    protected void setSavepoint(Object savepoint) {
        this.savepoint = savepoint;
    }

    /**
     * 获取此事务的保存点（如果有）
     */
    protected Object getSavepoint() {
        return this.savepoint;
    }

    /**
     * 创建一个保存点并为事务保留它
     *
     * @throws org.clever.transaction.NestedTransactionNotSupportedException 如果基础事务不支持保存点
     */
    public void createAndHoldSavepoint() throws TransactionException {
        setSavepoint(getSavepointManager().createSavepoint());
    }

    /**
     * 回滚到为事务保留的保存点，然后立即释放该保存点
     */
    public void rollbackToHeldSavepoint() throws TransactionException {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionUsageException("Cannot roll back to savepoint - no savepoint associated with current transaction");
        }
        getSavepointManager().rollbackToSavepoint(savepoint);
        getSavepointManager().releaseSavepoint(savepoint);
        setSavepoint(null);
    }

    /**
     * 释放为事务保留的保存点
     */
    public void releaseHeldSavepoint() throws TransactionException {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionUsageException("Cannot release savepoint - no savepoint associated with current transaction");
        }
        getSavepointManager().releaseSavepoint(savepoint);
        setSavepoint(null);
    }

    //---------------------------------------------------------------------
    // Implementation of SavepointManager
    //---------------------------------------------------------------------

    /**
     * 如果可能，此实现将基础事务委托给SavepointManager
     *
     * @see #getSavepointManager()
     * @see SavepointManager#createSavepoint()
     */
    @Override
    public Object createSavepoint() throws TransactionException {
        return getSavepointManager().createSavepoint();
    }

    /**
     * 如果可能，此实现将基础事务委托给SavepointManager。
     *
     * @see #getSavepointManager()
     * @see SavepointManager#rollbackToSavepoint(Object)
     */
    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().rollbackToSavepoint(savepoint);
    }

    /**
     * 如果可能，此实现将基础事务委托给SavepointManager。
     *
     * @see #getSavepointManager()
     * @see SavepointManager#releaseSavepoint(Object)
     */
    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().releaseSavepoint(savepoint);
    }

    /**
     * 如果可能，返回基础事务的SavepointManager。
     * <p>默认实现总是抛出 NestedTransactionNotSupportedException.
     *
     * @throws org.clever.transaction.NestedTransactionNotSupportedException 如果基础事务不支持保存点
     */
    protected SavepointManager getSavepointManager() {
        throw new NestedTransactionNotSupportedException("This transaction does not support savepoints");
    }

    //---------------------------------------------------------------------
    // Flushing support
    //---------------------------------------------------------------------

    /**
     * 此实现为空，将flush视为no-op
     */
    @Override
    public void flush() {
    }
}
