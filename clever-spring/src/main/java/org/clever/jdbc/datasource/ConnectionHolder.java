package org.clever.jdbc.datasource;

import org.clever.transaction.support.ResourceHolderSupport;
import org.clever.util.Assert;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * 资源持有者包装JDBC {@link Connection}。
 * {@link DataSourceTransactionManager}将此类的实例绑定到特定的线程{@link javax.sql.DataSource}。
 * <p>从基类继承对嵌套JDBC事务和引用计数功能的仅回滚支持。
 * <p>注意：这是一个SPI类，不打算由应用程序使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:55 <br/>
 *
 * @see DataSourceTransactionManager
 * @see DataSourceUtils
 */
public class ConnectionHolder extends ResourceHolderSupport {
    /**
     * 保存点名称的前缀
     */
    public static final String SAVEPOINT_NAME_PREFIX = "SAVEPOINT_";

    private ConnectionHandle connectionHandle;
    private Connection currentConnection;
    private boolean transactionActive = false;
    private Boolean savepointsSupported;
    private int savepointCounter = 0;

    /**
     * 为给定ConnectionHandle创建新的ConnectionHolder
     *
     * @param connectionHandle 要保持的ConnectionHandle
     */
    public ConnectionHolder(ConnectionHandle connectionHandle) {
        Assert.notNull(connectionHandle, "ConnectionHandle must not be null");
        this.connectionHandle = connectionHandle;
    }

    /**
     * 假设没有正在进行的事务，为给定的JDBC连接创建一个新的ConnectionHolder，
     * 并用{@link SimpleConnectionHandle}包装它。
     *
     * @param connection 要保持的JDBC连接
     * @see SimpleConnectionHandle
     * @see #ConnectionHolder(java.sql.Connection, boolean)
     */
    public ConnectionHolder(Connection connection) {
        this.connectionHandle = new SimpleConnectionHandle(connection);
    }

    /**
     * 为给定的JDBC连接创建一个新的ConnectionHolder，并用{@link SimpleConnectionHandle}包装它。
     *
     * @param connection        要保持的JDBC连接
     * @param transactionActive 给定连接是否涉及正在进行的事务
     * @see SimpleConnectionHandle
     */
    public ConnectionHolder(Connection connection, boolean transactionActive) {
        this(connection);
        this.transactionActive = transactionActive;
    }

    /**
     * 返回此ConnectionHolder持有的ConnectionHandle
     */
    public ConnectionHandle getConnectionHandle() {
        return this.connectionHandle;
    }

    /**
     * 返回此持有者当前是否有连接
     */
    protected boolean hasConnection() {
        return (this.connectionHandle != null);
    }

    /**
     * 设置此持有者是否表示活动的JDBC管理的事务
     *
     * @see DataSourceTransactionManager
     */
    @SuppressWarnings("SameParameterValue")
    protected void setTransactionActive(boolean transactionActive) {
        this.transactionActive = transactionActive;
    }

    /**
     * 返回此持有者是否表示活动的JDBC管理的事务
     */
    protected boolean isTransactionActive() {
        return this.transactionActive;
    }

    /**
     * 使用给定连接重写现有连接句柄。如果给定null，请重置句柄。
     * <p>用于在挂起时释放连接（使用null参数）和在恢复时设置新连接。
     */
    protected void setConnection(Connection connection) {
        if (this.currentConnection != null) {
            if (this.connectionHandle != null) {
                this.connectionHandle.releaseConnection(this.currentConnection);
            }
            this.currentConnection = null;
        }
        if (connection != null) {
            this.connectionHandle = new SimpleConnectionHandle(connection);
        } else {
            this.connectionHandle = null;
        }
    }

    /**
     * 返回此ConnectionHolder保持的当前连接。
     * <p>在ConnectionHolder上调用{@code released}之前，这将是相同的连接，
     * ConnectionHolder将重置保留的连接，并根据需要获取新连接。
     *
     * @see ConnectionHandle#getConnection()
     * @see #released()
     */
    public Connection getConnection() {
        Assert.notNull(this.connectionHandle, "Active Connection is required");
        if (this.currentConnection == null) {
            this.currentConnection = this.connectionHandle.getConnection();
        }
        return this.currentConnection;
    }

    /**
     * 返回是否支持JDBC 3.0保存点。在此ConnectionHolder的生存期内缓存标志。
     *
     * @throws SQLException 如果由JDBC驱动程序引发
     */
    public boolean supportsSavepoints() throws SQLException {
        if (this.savepointsSupported == null) {
            this.savepointsSupported = getConnection().getMetaData().supportsSavepoints();
        }
        return this.savepointsSupported;
    }

    /**
     * 使用生成的对连接唯一的保存点名称，为当前连接创建新的JDBC 3.0保存点。
     *
     * @return 新的保存点
     * @throws SQLException 如果由JDBC驱动程序引发
     */
    public Savepoint createSavepoint() throws SQLException {
        this.savepointCounter++;
        return getConnection().setSavepoint(SAVEPOINT_NAME_PREFIX + this.savepointCounter);
    }

    /**
     * 释放此ConnectionHolder保持的当前连接。
     * <p>这对于期望“Connection borrowing”的ConnectionHandles是必要的，
     * 其中每个返回的连接只是临时租用的，并且在数据操作完成后需要返回，以使连接可用于同一事务中的其他操作。
     */
    @Override
    public void released() {
        super.released();
        if (!isOpen() && this.currentConnection != null) {
            if (this.connectionHandle != null) {
                this.connectionHandle.releaseConnection(this.currentConnection);
            }
            this.currentConnection = null;
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.transactionActive = false;
        this.savepointsSupported = null;
        this.savepointCounter = 0;
    }
}
