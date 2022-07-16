package org.clever.jdbc.datasource;

import org.clever.jdbc.CannotGetJdbcConnectionException;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.support.TransactionSynchronization;
import org.clever.transaction.support.TransactionSynchronizationManager;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper类，提供从{@link javax.sql.DataSource}获取JDBC连接的静态方法。
 * 包括对框架管理的事务连接的特殊支持，例如由{@link DataSourceTransactionManager}或{@code JtaTransactionManager}。
 * <p>内部使用。{@code JdbcTemplate}JDBC操作对象和JDBC数据源事务管理器。也可以直接在应用程序代码中使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:47 <br/>
 *
 * @see #getConnection
 * @see #releaseConnection
 * @see DataSourceTransactionManager
 * @see org.clever.transaction.support.TransactionSynchronizationManager
 */
public abstract class DataSourceUtils {
    /**
     * 清除JDBC连接的TransactionSynchronization对象的Order值
     */
    public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;
    private static final Logger logger = LoggerFactory.getLogger(DataSourceUtils.class);

    /**
     * 从给定的数据源获取连接。将SQLExceptions转换为未经检查的通用数据访问异常的层次结构，从而简化调用代码并使抛出的任何异常更有意义。
     * <p>知道绑定到当前线程的相应连接，例如在使用{@link DataSourceTransactionManager}时。
     * 如果事务同步处于活动状态（例如在JTA事务中运行），则将连接绑定到线程）。
     *
     * @param dataSource 要从中获取连接的数据源
     * @return 来自给定数据源的JDBC连接
     * @throws org.clever.jdbc.CannotGetJdbcConnectionException 如果尝试获取连接失败
     * @see #releaseConnection(Connection, DataSource)
     * @see #isConnectionTransactional(Connection, DataSource)
     */
    public static Connection getConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
        try {
            return doGetConnection(dataSource);
        } catch (SQLException ex) {
            throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
        } catch (IllegalStateException ex) {
            throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection: " + ex.getMessage());
        }
    }

    /**
     * 实际上，从给定的数据源获取JDBC连接。与{@link #getConnection}相同，但引发原始SQLException。
     * <p>知道绑定到当前线程的相应连接，例如在使用{@link DataSourceTransactionManager}时。
     * 如果事务同步处于活动状态（例如，如果在JTA事务中），则将连接绑定到线程。
     * <p>{@link TransactionAwareDataSourceProxy}直接访问。
     *
     * @param dataSource 要从中获取连接的数据源
     * @return 来自给定数据源的JDBC连接
     * @throws SQLException 如果由JDBC方法引发
     * @see #doReleaseConnection
     */
    public static Connection doGetConnection(DataSource dataSource) throws SQLException {
        Assert.notNull(dataSource, "No DataSource specified");
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
            conHolder.requested();
            if (!conHolder.hasConnection()) {
                logger.debug("Fetching resumed JDBC Connection from DataSource");
                conHolder.setConnection(fetchConnection(dataSource));
            }
            return conHolder.getConnection();
        }
        // Else we either got no holder or an empty thread-bound holder here.
        logger.debug("Fetching JDBC Connection from DataSource");
        Connection con = fetchConnection(dataSource);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                // Use same Connection for further JDBC actions within the transaction.
                // Thread-bound object will get removed by synchronization at transaction completion.
                ConnectionHolder holderToUse = conHolder;
                if (holderToUse == null) {
                    holderToUse = new ConnectionHolder(con);
                } else {
                    holderToUse.setConnection(con);
                }
                holderToUse.requested();
                TransactionSynchronizationManager.registerSynchronization(
                        new ConnectionSynchronization(holderToUse, dataSource)
                );
                holderToUse.setSynchronizedWithTransaction(true);
                if (holderToUse != conHolder) {
                    TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
                }
            } catch (RuntimeException ex) {
                // Unexpected exception from external delegation call -> close Connection and rethrow.
                releaseConnection(con, dataSource);
                throw ex;
            }
        }
        return con;
    }

    /**
     * 实际上，从给定的{@link DataSource}获取{@link Connection}，从数据源中防御性地返回意外的null值。
     * {@link DataSource#getConnection()}进入{@link IllegalStateException}
     *
     * @param dataSource 要从中获取连接的数据源
     * @return 来自给定数据源的JDBC连接(从不为null)
     * @throws SQLException          如果由JDBC方法引发
     * @throws IllegalStateException 如果数据源返回空值
     * @see DataSource#getConnection()
     */
    private static Connection fetchConnection(DataSource dataSource) throws SQLException {
        Connection con = dataSource.getConnection();
        if (con == null) {
            throw new IllegalStateException("DataSource returned null from getConnection(): " + dataSource);
        }
        return con;
    }

    /**
     * 使用给定的事务语义准备给定的连接
     *
     * @param con        要准备的连接
     * @param definition 要应用的事务定义
     * @return 以前的隔离级别（如果有）
     * @throws SQLException 如果由JDBC方法引发
     * @see #resetConnectionAfterTransaction
     * @see Connection#setTransactionIsolation
     * @see Connection#setReadOnly
     */
    public static Integer prepareConnectionForTransaction(Connection con, TransactionDefinition definition) throws SQLException {
        Assert.notNull(con, "No Connection specified");
        boolean debugEnabled = logger.isDebugEnabled();
        // Set read-only flag.
        if (definition != null && definition.isReadOnly()) {
            try {
                if (debugEnabled) {
                    logger.debug("Setting JDBC Connection [" + con + "] read-only");
                }
                con.setReadOnly(true);
            } catch (SQLException | RuntimeException ex) {
                Throwable exToCheck = ex;
                while (exToCheck != null) {
                    if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
                        // Assume it's a connection timeout that would otherwise get lost: e.g. from JDBC 4.0
                        throw ex;
                    }
                    exToCheck = exToCheck.getCause();
                }
                // "read-only not supported" SQLException -> ignore, it's just a hint anyway
                logger.debug("Could not set JDBC Connection read-only", ex);
            }
        }
        // Apply specific isolation level, if any.
        Integer previousIsolationLevel = null;
        if (definition != null && definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
            if (debugEnabled) {
                logger.debug(
                        "Changing isolation level of JDBC Connection [" + con + "] to " + definition.getIsolationLevel()
                );
            }
            int currentIsolation = con.getTransactionIsolation();
            if (currentIsolation != definition.getIsolationLevel()) {
                previousIsolationLevel = currentIsolation;
                // noinspection MagicConstant
                con.setTransactionIsolation(definition.getIsolationLevel());
            }
        }
        return previousIsolationLevel;
    }

    /**
     * 关于只读标志和隔离级别，在事务后重置给定连接。
     *
     * @param con                    要重置的连接
     * @param previousIsolationLevel 要恢复的隔离级别（如果有）
     * @param resetReadOnly          是否重置连接的只读标志
     * @see #prepareConnectionForTransaction
     * @see Connection#setTransactionIsolation
     * @see Connection#setReadOnly
     */
    public static void resetConnectionAfterTransaction(Connection con, Integer previousIsolationLevel, boolean resetReadOnly) {
        Assert.notNull(con, "No Connection specified");
        boolean debugEnabled = logger.isDebugEnabled();
        try {
            // Reset transaction isolation to previous value, if changed for the transaction.
            if (previousIsolationLevel != null) {
                if (debugEnabled) {
                    logger.debug(
                            "Resetting isolation level of JDBC Connection [" + con + "] to " + previousIsolationLevel
                    );
                }
                con.setTransactionIsolation(previousIsolationLevel);
            }
            // Reset read-only flag if we originally switched it to true on transaction begin.
            if (resetReadOnly) {
                if (debugEnabled) {
                    logger.debug("Resetting read-only flag of JDBC Connection [" + con + "]");
                }
                con.setReadOnly(false);
            }
        } catch (Throwable ex) {
            logger.debug("Could not reset JDBC Connection after transaction", ex);
        }
    }

    /**
     * 确定给定的JDBC连接是否是事务性的，即通过事务设施绑定到当前线程。
     *
     * @param con        要检查的连接
     * @param dataSource 获取连接的数据源（可能是{@code null}）
     * @return 连接是否为事务性连接
     * @see #getConnection(DataSource)
     */
    public static boolean isConnectionTransactional(Connection con, DataSource dataSource) {
        if (dataSource == null) {
            return false;
        }
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        return (conHolder != null && connectionEquals(conHolder, con));
    }

    /**
     * 将当前事务超时（如果有）应用于给定的JDBC语句对象。
     *
     * @param stmt       JDBC语句对象
     * @param dataSource 从中获取连接的数据源
     * @throws SQLException 如果由JDBC方法引发
     * @see java.sql.Statement#setQueryTimeout
     */
    public static void applyTransactionTimeout(Statement stmt, DataSource dataSource) throws SQLException {
        applyTimeout(stmt, dataSource, -1);
    }

    /**
     * 将指定的超时（由当前事务超时覆盖，如果有）应用于给定的JDBC语句对象。
     *
     * @param stmt       JDBC语句对象
     * @param dataSource 从中获取连接的数据源
     * @param timeout    要应用的超时（或0表示事务外无超时）
     * @throws SQLException 如果由JDBC方法引发
     * @see java.sql.Statement#setQueryTimeout
     */
    public static void applyTimeout(Statement stmt, DataSource dataSource, int timeout) throws SQLException {
        Assert.notNull(stmt, "No Statement specified");
        ConnectionHolder holder = null;
        if (dataSource != null) {
            holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        }
        if (holder != null && holder.hasTimeout()) {
            // Remaining transaction timeout overrides specified value.
            stmt.setQueryTimeout(holder.getTimeToLiveInSeconds());
        } else if (timeout >= 0) {
            // No current transaction timeout -> apply specified value.
            stmt.setQueryTimeout(timeout);
        }
    }

    /**
     * 如果给定连接没有外部管理（也就是说，没有绑定到线程），请关闭从给定数据源获得的给定连接。
     *
     * @param con        必要时要关闭的连接（如果这是{@code null}，调用将被忽略）
     * @param dataSource 获取连接的数据源（可能是{@code null}）
     * @see #getConnection
     */
    public static void releaseConnection(Connection con, DataSource dataSource) {
        try {
            doReleaseConnection(con, dataSource);
        } catch (SQLException ex) {
            logger.debug("Could not close JDBC Connection", ex);
        } catch (Throwable ex) {
            logger.debug("Unexpected exception on closing JDBC Connection", ex);
        }
    }

    /**
     * 实际关闭从给定数据源获取的给定连接。
     * 与{@link #releaseConnection}相同，但引发原始SQLException。
     * <p>{@link TransactionAwareDataSourceProxy}直接访问。
     *
     * @param con        必要时要关闭的连接（如果这是{@code null}，调用将被忽略）
     * @param dataSource 获取连接的数据源（可能是{@code null}）
     * @throws SQLException 如果由JDBC方法引发
     * @see #doGetConnection
     */
    public static void doReleaseConnection(Connection con, DataSource dataSource) throws SQLException {
        if (con == null) {
            return;
        }
        if (dataSource != null) {
            ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
            if (conHolder != null && connectionEquals(conHolder, con)) {
                // It's the transactional Connection: Don't close it.
                conHolder.released();
                return;
            }
        }
        doCloseConnection(con, dataSource);
    }

    /**
     * 关闭连接，除非{@link SmartDataSource}不希望我们这样做。
     *
     * @param con        必要时关闭连接
     * @param dataSource 从中获取连接的数据源
     * @throws SQLException 如果由JDBC方法引发
     * @see Connection#close()
     * @see SmartDataSource#shouldClose(Connection)
     */
    public static void doCloseConnection(Connection con, DataSource dataSource) throws SQLException {
        if (!(dataSource instanceof SmartDataSource) || ((SmartDataSource) dataSource).shouldClose(con)) {
            con.close();
        }
    }

    /**
     * 确定给定的两个连接是否相等，如果是代理，则询问目标连接。
     * 用于检测相等性，即使用户传入了原始目标连接，而保持的连接是代理。
     *
     * @param conHolder   所保持连接的ConnectionHolder（可能是代理）
     * @param passedInCon 用户传入的连接（可能是没有代理的目标连接）
     * @return 给定连接是否相等
     * @see #getTargetConnection
     */
    private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
        if (!conHolder.hasConnection()) {
            return false;
        }
        Connection heldCon = conHolder.getConnection();
        // Explicitly check for identity too: for Connection handles that do not implement
        // "equals" properly, such as the ones Commons DBCP exposes).
        return (heldCon == passedInCon || heldCon.equals(passedInCon) || getTargetConnection(heldCon).equals(passedInCon));
    }

    /**
     * 返回给定连接的最内层目标连接。
     * 如果给定的连接是一个代理，它将被展开，直到找到一个非代理连接。
     * 否则，传入的连接将按原样返回。
     *
     * @param con 要展开的连接代理
     * @return 最内部的目标连接，如果没有代理，则为传入的目标连接
     * @see ConnectionProxy#getTargetConnection()
     */
    public static Connection getTargetConnection(Connection con) {
        Connection conToUse = con;
        while (conToUse instanceof ConnectionProxy) {
            conToUse = ((ConnectionProxy) conToUse).getTargetConnection();
        }
        return conToUse;
    }

    /**
     * 确定用于给定数据源的连接同步顺序。
     * 通过DelegatingDataSource嵌套级别进行检查，数据源具有的每个嵌套级别都会减少。
     *
     * @param dataSource 要检查的数据源
     * @return 要使用的连接同步顺序
     * @see #CONNECTION_SYNCHRONIZATION_ORDER
     */
    private static int getConnectionSynchronizationOrder(DataSource dataSource) {
        int order = CONNECTION_SYNCHRONIZATION_ORDER;
        DataSource currDs = dataSource;
        while (currDs instanceof DelegatingDataSource) {
            order--;
            currDs = ((DelegatingDataSource) currDs).getTargetDataSource();
        }
        return order;
    }

    /**
     * 在非本机JDBC事务结束时（例如，当参与JtaTransactionManager事务时）回调以进行资源清理。
     */
    private static class ConnectionSynchronization implements TransactionSynchronization {
        private final ConnectionHolder connectionHolder;
        private final DataSource dataSource;
        private final int order;
        private boolean holderActive = true;

        public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
            this.connectionHolder = connectionHolder;
            this.dataSource = dataSource;
            this.order = getConnectionSynchronizationOrder(dataSource);
        }

        @Override
        public int getOrder() {
            return this.order;
        }

        @Override
        public void suspend() {
            if (this.holderActive) {
                TransactionSynchronizationManager.unbindResource(this.dataSource);
                if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
                    // Release Connection on suspend if the application doesn't keep
                    // a handle to it anymore. We will fetch a fresh Connection if the
                    // application accesses the ConnectionHolder again after resume,
                    // assuming that it will participate in the same transaction.
                    releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
                    this.connectionHolder.setConnection(null);
                }
            }
        }

        @Override
        public void resume() {
            if (this.holderActive) {
                TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
            }
        }

        @Override
        public void beforeCompletion() {
            // Release Connection early if the holder is not open anymore
            // (that is, not used by another resource like a Hibernate Session
            // that has its own cleanup via transaction synchronization),
            // to avoid issues with strict JTA implementations that expect
            // the close call before transaction completion.
            if (!this.connectionHolder.isOpen()) {
                TransactionSynchronizationManager.unbindResource(this.dataSource);
                this.holderActive = false;
                if (this.connectionHolder.hasConnection()) {
                    releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
                }
            }
        }

        @Override
        public void afterCompletion(int status) {
            // If we haven't closed the Connection in beforeCompletion,
            // close it now. The holder might have been used for other
            // cleanup in the meantime, for example by a Hibernate Session.
            if (this.holderActive) {
                // The thread-bound ConnectionHolder might not be available anymore,
                // since afterCompletion might get called from a different thread.
                TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
                this.holderActive = false;
                if (this.connectionHolder.hasConnection()) {
                    releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
                    // Reset the ConnectionHolder: It might remain bound to the thread.
                    this.connectionHolder.setConnection(null);
                }
            }
            this.connectionHolder.reset();
        }
    }
}
