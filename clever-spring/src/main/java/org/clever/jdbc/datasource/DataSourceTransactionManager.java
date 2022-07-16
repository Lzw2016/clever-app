package org.clever.jdbc.datasource;

import org.clever.transaction.CannotCreateTransactionException;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.TransactionSystemException;
import org.clever.transaction.support.*;
import org.clever.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * {@link org.clever.transaction.PlatformTransactionManager} 单个JDBC的实现 {@link javax.sql.DataSource}.
 * 此类能够在任何环境中使用任何JDBC驱动程序, 只要安装程序使用 {@code javax.sql.DataSource} 作为其 {@code Connection} 工厂机制。
 * 将指定数据源的JDBC连接绑定到当前线程，可能允许每个数据源有一个线程绑定连接。
 *
 * <p>注意：此事务管理器操作的数据源需要返回独立的连接。
 * 连接可能来自池（典型情况），但数据源不能返回线程范围的请求范围的连接等。
 * 此事务管理器将根据指定的传播行为将连接与线程绑定的事务本身相关联。
 * 它假设即使在正在进行的事务中也可以获得独立的连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:47 <br/>
 *
 * @see #setNestedTransactionAllowed
 * @see java.sql.Savepoint
 * @see DataSourceUtils#getConnection(javax.sql.DataSource)
 * @see DataSourceUtils#applyTransactionTimeout
 * @see DataSourceUtils#releaseConnection
 * @see TransactionAwareDataSourceProxy
 */
public class DataSourceTransactionManager extends AbstractPlatformTransactionManager implements ResourceTransactionManager {
    private DataSource dataSource;
    private boolean enforceReadOnly = false;

    /**
     * 创建新的DataSourceTransactionManager实例。
     * 必须设置数据源才能使用它。
     *
     * @see #setDataSource
     */
    public DataSourceTransactionManager() {
        setNestedTransactionAllowed(true);
    }

    /**
     * 创建新的DataSourceTransactionManager实例。
     *
     * @param dataSource 用于管理事务的JDBC数据源
     */
    public DataSourceTransactionManager(DataSource dataSource) {
        this();
        setDataSource(dataSource);
        afterPropertiesSet();
    }

    /**
     * 设置此实例应为其管理事务的JDBC数据源。
     * <p>这通常是本地定义的数据源，例如Apache Commons DBCP连接池。
     * 或者，您还可以驱动从JNDI获取的非XA J2EE数据源的事务。
     * 对于XA数据源，请使用JtaTransactionManager。
     * <p>此处指定的数据源应该是要为其管理事务的目标数据源，而不是TransactionAwareDataSourceProxy。
     * 只有数据访问代码可以使用TransactionAwareDataSourceProxy，而事务管理器需要使用底层目标数据源。
     * 如果仍然传入了TransactionAwareDataSourceProxy，则将对其展开以提取其目标数据源。
     * <p>此处传入的数据源需要返回独立的连接。连接可能来自池（典型情况），但数据源不能返回线程范围/请求范围的连接等。
     *
     * @see TransactionAwareDataSourceProxy
     */
    public void setDataSource(DataSource dataSource) {
        if (dataSource instanceof TransactionAwareDataSourceProxy) {
            // If we got a TransactionAwareDataSourceProxy, we need to perform transactions
            // for its underlying target DataSource, else data access code won't see
            // properly exposed transactions (i.e. transactions for the target DataSource).
            this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
        } else {
            this.dataSource = dataSource;
        }
    }

    /**
     * 返回此实例为其管理事务的JDBC数据源。
     */
    public DataSource getDataSource() {
        return this.dataSource;
    }

    /**
     * 获取数据源以供实际使用。
     *
     * @return 数据来源 (从不为 {@code null})
     * @throws IllegalStateException 如果未设置数据源
     */
    protected DataSource obtainDataSource() {
        DataSource dataSource = getDataSource();
        Assert.state(dataSource != null, "No DataSource set");
        return dataSource;
    }

    /**
     * 指定是否强制事务的只读性质，如{@link TransactionDefinition#isReadOnly()}所示，
     * 通过事务连接上的显式语句：“SET transaction read only”，如Oracle、MySQL和Postgres所理解。
     * <p>可以通过{@link #prepareTransactionalConnection}自定义确切的处理方法，包括在连接上执行的任何SQL语句。
     * <p>这种只读处理模式超出了连接的范围{@link Connection#setReadOnly}提示默认应用。
     * 与标准JDBC提示不同，“SET TRANSACTION READ ONLY”强制执行类似于隔离级别的连接模式，其中严格禁止数据操作语句。
     * 此外，在Oracle上，此只读模式为整个事务提供了读取一致性。
     * 请注意，旧的Oracle JDBC驱动程序（9i、10g）甚至用于连接，也用于强制执行此只读模式{@code Connection.setReadOnly(true)}。
     * 但是，对于最近的驱动程序，需要明确应用这种强大的强制，例如通过此标志。
     *
     * @see #prepareTransactionalConnection
     */
    public void setEnforceReadOnly(boolean enforceReadOnly) {
        this.enforceReadOnly = enforceReadOnly;
    }

    /**
     * 返回是否通过事务连接上的显式语句强制事务的只读性质
     *
     * @see #setEnforceReadOnly
     */
    public boolean isEnforceReadOnly() {
        return this.enforceReadOnly;
    }

    public void afterPropertiesSet() {
        if (getDataSource() == null) {
            throw new IllegalArgumentException("Property 'dataSource' is required");
        }
    }

    @Override
    public Object getResourceFactory() {
        return obtainDataSource();
    }

    @Override
    protected Object doGetTransaction() {
        DataSourceTransactionObject txObject = new DataSourceTransactionObject();
        txObject.setSavepointAllowed(isNestedTransactionAllowed());
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
        txObject.setConnectionHolder(conHolder, false);
        return txObject;
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        Connection con = null;
        try {
            if (!txObject.hasConnectionHolder() || txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
                Connection newCon = obtainDataSource().getConnection();
                if (logger.isDebugEnabled()) {
                    logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
                }
                txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
            }
            txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
            con = txObject.getConnectionHolder().getConnection();
            Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
            txObject.setPreviousIsolationLevel(previousIsolationLevel);
            txObject.setReadOnly(definition.isReadOnly());
            // Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
            // so we don't want to do it unnecessarily (for example if we've explicitly
            // configured the connection pool to set it already).
            if (con.getAutoCommit()) {
                txObject.setMustRestoreAutoCommit(true);
                if (logger.isDebugEnabled()) {
                    logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
                }
                con.setAutoCommit(false);
            }
            prepareTransactionalConnection(con, definition);
            txObject.getConnectionHolder().setTransactionActive(true);
            int timeout = determineTimeout(definition);
            if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
                txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
            }
            // Bind the connection holder to the thread.
            if (txObject.isNewConnectionHolder()) {
                TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
            }
        } catch (Throwable ex) {
            if (txObject.isNewConnectionHolder()) {
                DataSourceUtils.releaseConnection(con, obtainDataSource());
                txObject.setConnectionHolder(null, false);
            }
            throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
        }
    }

    @Override
    protected Object doSuspend(Object transaction) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        txObject.setConnectionHolder(null);
        return TransactionSynchronizationManager.unbindResource(obtainDataSource());
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();
        if (status.isDebug()) {
            logger.debug("Committing JDBC transaction on Connection [" + con + "]");
        }
        try {
            con.commit();
        } catch (SQLException ex) {
            throw translateException("JDBC commit", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();
        if (status.isDebug()) {
            logger.debug("Rolling back JDBC transaction on Connection [" + con + "]");
        }
        try {
            con.rollback();
        } catch (SQLException ex) {
            throw translateException("JDBC rollback", ex);
        }
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
        if (status.isDebug()) {
            logger.debug(
                    "Setting JDBC transaction [" + txObject.getConnectionHolder().getConnection() + "] rollback-only"
            );
        }
        txObject.setRollbackOnly();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        // Remove the connection holder from the thread, if exposed.
        if (txObject.isNewConnectionHolder()) {
            TransactionSynchronizationManager.unbindResource(obtainDataSource());
        }
        // Reset connection.
        Connection con = txObject.getConnectionHolder().getConnection();
        try {
            if (txObject.isMustRestoreAutoCommit()) {
                con.setAutoCommit(true);
            }
            DataSourceUtils.resetConnectionAfterTransaction(
                    con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly()
            );
        } catch (Throwable ex) {
            logger.debug("Could not reset JDBC Connection after transaction", ex);
        }
        if (txObject.isNewConnectionHolder()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Releasing JDBC Connection [" + con + "] after transaction");
            }
            DataSourceUtils.releaseConnection(con, this.dataSource);
        }
        txObject.getConnectionHolder().clear();
    }

    /**
     * 在事务开始后立即准备事务连接。
     * <p>如果“enforceReadOnly”标志设置为true，并且事务定义指示只读事务，则默认实现执行“SET TRANSACTION ReadOnly”语句。
     * <p>Oracle、MySQL和Postgres都理解“SET TRANSACTION READ ONLY”，也可以与其他数据库一起使用。
     * 如果您想调整此处理，请相应地重写此方法。
     *
     * @param con        事务性JDBC连接
     * @param definition 当前交易定义
     * @throws SQLException 如果由JDBC API引发
     * @see #setEnforceReadOnly
     */
    protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition) throws SQLException {
        if (isEnforceReadOnly() && definition.isReadOnly()) {
            try (Statement stmt = con.createStatement()) {
                // noinspection SqlDialectInspection,SqlNoDataSourceInspection
                stmt.executeUpdate("SET TRANSACTION READ ONLY");
            }
        }
    }

    /**
     * 将给定的JDBC commit/rollback异常转换为公共异常，以便从{@link #commit}/{@link #rollback}调用传播。
     * <p>默认实现引发{@link TransactionSystemException}。子类可以专门标识并发故障等。
     *
     * @param task 任务描述（提交或回滚）
     * @param ex   commit/rollback引发的SQLException
     * @return 要引发的转换异常，可以是 {@link org.clever.dao.DataAccessException} 或 {@link org.clever.transaction.TransactionException}
     */
    protected RuntimeException translateException(String task, SQLException ex) {
        return new TransactionSystemException(task + " failed", ex);
    }

    /**
     * 数据源事务对象，表示ConnectionHolder。
     * DataSourceTransactionManager将其用作事务对象。
     */
    private static class DataSourceTransactionObject extends JdbcTransactionObjectSupport {
        private boolean newConnectionHolder;
        private boolean mustRestoreAutoCommit;

        public void setConnectionHolder(ConnectionHolder connectionHolder, boolean newConnectionHolder) {
            super.setConnectionHolder(connectionHolder);
            this.newConnectionHolder = newConnectionHolder;
        }

        public boolean isNewConnectionHolder() {
            return this.newConnectionHolder;
        }

        public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
            this.mustRestoreAutoCommit = mustRestoreAutoCommit;
        }

        public boolean isMustRestoreAutoCommit() {
            return this.mustRestoreAutoCommit;
        }

        public void setRollbackOnly() {
            getConnectionHolder().setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            return getConnectionHolder().isRollbackOnly();
        }

        @Override
        public void flush() {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationUtils.triggerFlush();
            }
        }
    }
}
