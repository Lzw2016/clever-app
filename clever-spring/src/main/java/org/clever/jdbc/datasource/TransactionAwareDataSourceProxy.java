package org.clever.jdbc.datasource;

import org.clever.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 目标JDBC {@link javax.sql.DataSource}的代理，增加了对管理的事务的感知。
 * 类似于Java EE服务器提供的事务性JNDI数据源。
 *
 * <p>应该不知道的数据访问支持的数据访问代码可以使用此代理无缝地参与管理的事务。
 * 请注意，事务管理器（例如{@link DataSourceTransactionManager}）仍然需要使用底层数据源，而不是此代理。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:45 <br/>
 *
 * @see javax.sql.DataSource#getConnection()
 * @see java.sql.Connection#close()
 * @see DataSourceUtils#doGetConnection
 * @see DataSourceUtils#applyTransactionTimeout
 * @see DataSourceUtils#doReleaseConnection
 */
public class TransactionAwareDataSourceProxy extends DelegatingDataSource {
    private boolean reobtainTransactionalConnections = false;

    /**
     * @see #setTargetDataSource
     */
    public TransactionAwareDataSourceProxy() {
    }

    /**
     * @param targetDataSource 目标数据源
     */
    public TransactionAwareDataSourceProxy(DataSource targetDataSource) {
        super(targetDataSource);
    }

    /**
     * 指定是否为事务中执行的每个操作重新获取目标连接。
     * <p>默认值为“false”。指定“true”以重新获得连接代理上每个调用的事务连接；如果您在JBoss上保持跨事务边界的连接句柄，则建议您这样做。
     * <p>此设置的效果类似于“hibernate.connection.release_mode” 的值 “after_statement”。
     */
    public void setReobtainTransactionalConnections(boolean reobtainTransactionalConnections) {
        this.reobtainTransactionalConnections = reobtainTransactionalConnections;
    }

    /**
     * 委托DataSourceUtils自动参与clever管理的事务。抛出原始SQLException（如果有）。
     * <p>返回的连接句柄实现ConnectionProxy接口，允许检索底层目标连接。
     *
     * @return 一个事务连接（如果有），另一个新连接
     * @see DataSourceUtils#doGetConnection
     * @see ConnectionProxy#getTargetConnection
     */
    @Override
    public Connection getConnection() {
        return getTransactionAwareConnectionProxy(obtainTargetDataSource());
    }

    /**
     * 使用代理包装给定的连接，该代理将每个方法调用委托给它，但将{@code close()}调用委托给DataSourceUtils。
     *
     * @param targetDataSource 连接来自的数据源
     * @return 包裹的连接
     * @see java.sql.Connection#close()
     * @see DataSourceUtils#doReleaseConnection
     */
    protected Connection getTransactionAwareConnectionProxy(DataSource targetDataSource) {
        return (Connection) Proxy.newProxyInstance(
                ConnectionProxy.class.getClassLoader(),
                new Class<?>[]{ConnectionProxy.class},
                new TransactionAwareInvocationHandler(targetDataSource)
        );
    }

    /**
     * 确定是为代理获取固定目标连接，还是为每个操作重新获取目标连接。
     * <p>对于所有标准情况，默认实现都返回{@code true}。
     * 这可以通过{@link #setReobtainTransactionalConnections "reobtainTransactionalConnections"}标志覆盖，
     * 该标志在活动事务中强制非固定目标连接。请注意，非事务性访问将始终使用固定连接。
     *
     * @param targetDataSource 目标数据源
     */
    protected boolean shouldObtainFixedConnection(DataSource targetDataSource) {
        return (!TransactionSynchronizationManager.isSynchronizationActive() || !this.reobtainTransactionalConnections);
    }

    /**
     * 调用处理程序，将JDBC连接上的close调用委托给DataSourceUtils，以了解线程绑定的事务。
     */
    private class TransactionAwareInvocationHandler implements InvocationHandler {
        private final DataSource targetDataSource;
        private Connection target;
        private boolean closed = false;

        public TransactionAwareInvocationHandler(DataSource targetDataSource) {
            this.targetDataSource = targetDataSource;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Invocation on ConnectionProxy interface coming in...
            switch (method.getName()) {
                case "equals":
                    // Only considered as equal when proxies are identical.
                    return (proxy == args[0]);
                case "hashCode":
                    // Use hashCode of Connection proxy.
                    return System.identityHashCode(proxy);
                case "toString":
                    // Allow for differentiating between the proxy and the raw Connection.
                    StringBuilder sb = new StringBuilder("Transaction-aware proxy for target Connection ");
                    if (this.target != null) {
                        sb.append('[').append(this.target).append(']');
                    } else {
                        sb.append(" from DataSource [").append(this.targetDataSource).append(']');
                    }
                    return sb.toString();
                case "close":
                    // Handle close method: only close if not within a transaction.
                    DataSourceUtils.doReleaseConnection(this.target, this.targetDataSource);
                    this.closed = true;
                    return null;
                case "isClosed":
                    return this.closed;
                case "unwrap":
                    if (((Class<?>) args[0]).isInstance(proxy)) {
                        return proxy;
                    }
                    break;
                case "isWrapperFor":
                    if (((Class<?>) args[0]).isInstance(proxy)) {
                        return true;
                    }
                    break;
            }
            if (this.target == null) {
                if (method.getName().equals("getWarnings") || method.getName().equals("clearWarnings")) {
                    // Avoid creation of target Connection on pre-close cleanup (e.g. Hibernate Session)
                    return null;
                }
                if (this.closed) {
                    throw new SQLException("Connection handle already closed");
                }
                if (shouldObtainFixedConnection(this.targetDataSource)) {
                    this.target = DataSourceUtils.doGetConnection(this.targetDataSource);
                }
            }
            Connection actualTarget = this.target;
            if (actualTarget == null) {
                actualTarget = DataSourceUtils.doGetConnection(this.targetDataSource);
            }
            if (method.getName().equals("getTargetConnection")) {
                // Handle getTargetConnection method: return underlying Connection.
                return actualTarget;
            }
            // Invoke method on target Connection.
            try {
                Object retVal = method.invoke(actualTarget, args);
                // If return value is a Statement, apply transaction timeout.
                // Applies to createStatement, prepareStatement, prepareCall.
                if (retVal instanceof Statement) {
                    DataSourceUtils.applyTransactionTimeout((Statement) retVal, this.targetDataSource);
                }
                return retVal;
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            } finally {
                if (actualTarget != this.target) {
                    DataSourceUtils.doReleaseConnection(actualTarget, this.targetDataSource);
                }
            }
        }
    }
}
