package org.clever.jdbc.datasource;

import org.clever.transaction.*;
import org.clever.transaction.support.SmartTransactionObject;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * 用于支持JDBC的事务对象的方便基类。
 * 可以包含带有JDBC连接的{@link ConnectionHolder}，
 * 并基于该{@link SavepointManager}实现{@code ConnectionHolder}接口。
 *
 * <p>允许对JDBC {@link java.sql.Savepoint Savepoints}进行编程管理。
 * {@link org.clever.transaction.support.DefaultTransactionStatus}会自动委托给它，
 * 因为它会自动检测实现{@link SavepointManager}接口的事务对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 23:01 <br/>
 *
 * @see DataSourceTransactionManager
 */
public abstract class JdbcTransactionObjectSupport implements SavepointManager, SmartTransactionObject {
    private static final Logger logger = LoggerFactory.getLogger(JdbcTransactionObjectSupport.class);

    private ConnectionHolder connectionHolder;
    private Integer previousIsolationLevel;
    private boolean readOnly = false;
    private boolean savepointAllowed = false;

    /**
     * 设置此事务对象的ConnectionHolder。
     */
    public void setConnectionHolder(ConnectionHolder connectionHolder) {
        this.connectionHolder = connectionHolder;
    }

    /**
     * 返回此事务对象的ConnectionHolder。
     */
    public ConnectionHolder getConnectionHolder() {
        Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
        return this.connectionHolder;
    }

    /**
     * 检查此事务对象是否具有ConnectionHolder。
     */
    public boolean hasConnectionHolder() {
        return (this.connectionHolder != null);
    }

    /**
     * 将以前的隔离级别设置为保留（如果有）。
     */
    public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
        this.previousIsolationLevel = previousIsolationLevel;
    }

    /**
     * 返回保留的先前隔离级别（如果有）。
     */
    public Integer getPreviousIsolationLevel() {
        return this.previousIsolationLevel;
    }

    /**
     * 设置此事务的只读状态。默认值为 {@code false}.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * 返回此事务的只读状态。
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * 设置此事务中是否允许保存点。默认值为 {@code false}.
     */
    public void setSavepointAllowed(boolean savepointAllowed) {
        this.savepointAllowed = savepointAllowed;
    }

    /**
     * 返回此事务中是否允许保存点。
     */
    public boolean isSavepointAllowed() {
        return this.savepointAllowed;
    }

    @Override
    public void flush() {
        // no-op
    }

    //---------------------------------------------------------------------
    // Implementation of SavepointManager
    //---------------------------------------------------------------------

    /**
     * 此实现创建一个JDBC 3.0保存点并返回它。
     *
     * @see java.sql.Connection#setSavepoint
     */
    @Override
    public Object createSavepoint() throws TransactionException {
        ConnectionHolder conHolder = getConnectionHolderForSavepoint();
        try {
            if (!conHolder.supportsSavepoints()) {
                throw new NestedTransactionNotSupportedException(
                        "Cannot create a nested transaction because savepoints are not supported by your JDBC driver"
                );
            }
            if (conHolder.isRollbackOnly()) {
                throw new CannotCreateTransactionException(
                        "Cannot create savepoint for transaction which is already marked as rollback-only"
                );
            }
            return conHolder.createSavepoint();
        } catch (SQLException ex) {
            throw new CannotCreateTransactionException("Could not create JDBC savepoint", ex);
        }
    }

    /**
     * 此实现回滚到给定的JDBC 3.0保存点。
     *
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        ConnectionHolder conHolder = getConnectionHolderForSavepoint();
        try {
            conHolder.getConnection().rollback((Savepoint) savepoint);
            conHolder.resetRollbackOnly();
        } catch (Throwable ex) {
            throw new TransactionSystemException("Could not roll back to JDBC savepoint", ex);
        }
    }

    /**
     * 此实现发布了给定的JDBC 3.0保存点。
     *
     * @see java.sql.Connection#releaseSavepoint
     */
    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        ConnectionHolder conHolder = getConnectionHolderForSavepoint();
        try {
            conHolder.getConnection().releaseSavepoint((Savepoint) savepoint);
        } catch (Throwable ex) {
            logger.debug("Could not explicitly release JDBC savepoint", ex);
        }
    }

    protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
        if (!isSavepointAllowed()) {
            throw new NestedTransactionNotSupportedException(
                    "Transaction manager does not allow nested transactions"
            );
        }
        if (!hasConnectionHolder()) {
            throw new TransactionUsageException(
                    "Cannot create nested transaction when not exposing a JDBC transaction"
            );
        }
        return getConnectionHolder();
    }
}
