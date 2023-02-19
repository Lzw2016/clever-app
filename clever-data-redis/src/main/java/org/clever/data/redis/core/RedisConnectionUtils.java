package org.clever.data.redis.core;

import org.clever.core.proxy.JdkProxyUtils;
import org.clever.dao.DataAccessException;
import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.connection.RedisConnectionFactory;
import org.clever.transaction.support.AbstractPlatformTransactionManager;
import org.clever.transaction.support.ResourceHolderSupport;
import org.clever.transaction.support.TransactionSynchronization;
import org.clever.transaction.support.TransactionSynchronizationManager;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 提供从 {@link RedisConnectionFactory} 获取 {@link RedisConnection} 的静态方法的辅助类。
 * 包括对 Spring 管理的事务性 RedisConnections 的特殊支持，例如由 {@link AbstractPlatformTransactionManager} 管理。
 * <p>
 * {@link RedisTemplate} 在内部使用。也可以直接在应用程序代码中使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 16:02 <br/>
 *
 * @see #getConnection
 * @see #releaseConnection
 * @see TransactionSynchronizationManager
 */
public abstract class RedisConnectionUtils {
    private static final Logger log = LoggerFactory.getLogger(RedisConnectionUtils.class);

    /**
     * 从给定的 {@link RedisConnectionFactory} 获取 {@link RedisConnection} 并将连接绑定到要在闭包范围内使用的当前线程（如果尚未绑定）。
     * 通过重用事务绑定连接来考虑正在进行的事务，并允许重入连接检索。
     * 不将连接绑定到可能正在进行的事务。
     *
     * @param factory 连接工厂
     * @return 没有事务支持的新 Redis 连接
     */
    public static RedisConnection bindConnection(RedisConnectionFactory factory) {
        return doGetConnection(factory, true, true, false);
    }

    /**
     * 从给定的 {@link RedisConnectionFactory} 获取 {@link RedisConnection} 并将连接绑定到要在闭包范围内使用的当前线程（如果尚未绑定）。
     * 通过重用事务绑定连接来考虑正在进行的事务，并允许重入连接检索。
     * 如果启用了 {@code transactionSupport}，则如果尚未绑定任何连接，则还将连接绑定到正在进行的事务。
     *
     * @param factory            连接工厂
     * @param transactionSupport 是否启用事务支持
     * @return 如果需要，支持事务的新 Redis 连接
     */
    @SuppressWarnings("UnusedReturnValue")
    public static RedisConnection bindConnection(RedisConnectionFactory factory, boolean transactionSupport) {
        return doGetConnection(factory, true, true, transactionSupport);
    }

    /**
     * 从给定的 {@link RedisConnectionFactory} 获取 {@link RedisConnection}。
     * 知道绑定到当前事务（使用事务管理器时）或当前线程（将连接绑定到闭包范围时）的现有连接。
     * 不将新创建的连接绑定到正在进行的事务。
     *
     * @param factory 用于创建连接的连接工厂。
     * @return 没有事务管理的活动 Redis 连接。
     */
    public static RedisConnection getConnection(RedisConnectionFactory factory) {
        return getConnection(factory, false);
    }

    /**
     * 从给定的 {@link RedisConnectionFactory} 获取 {@link RedisConnection}。
     * 知道绑定到当前事务（使用事务管理器时）或当前线程（将连接绑定到闭包范围时）的现有连接。
     *
     * @param factory            用于创建连接的连接工厂
     * @param transactionSupport 是否启用事务支持
     * @return 如果需要，与事务管理的活动 Redis 连接
     */
    public static RedisConnection getConnection(RedisConnectionFactory factory, boolean transactionSupport) {
        return doGetConnection(factory, true, false, transactionSupport);
    }

    /**
     * 实际上从给定的 {@link RedisConnectionFactory} 获得一个 {@link RedisConnection}。
     * 知道绑定到当前事务（使用事务管理器时）或当前线程（将连接绑定到闭包范围时）的现有连接。
     * 否则将创建一个新的 {@link RedisConnection}，如果 {@code allowCreate} 是 {@literal true}。
     * 此方法允许重新进入，因为 {@link RedisConnectionHolder} 跟踪引用计数。
     *
     * @param factory            用于创建连接的连接工厂
     * @param allowCreate        当找不到当前线程的连接时，是否应创建新的（未绑定的）连接
     * @param bind               将连接绑定到线程，以防创建一个
     * @param transactionSupport 是否启用事务支持
     * @return 活动的 Redis 连接
     */
    public static RedisConnection doGetConnection(RedisConnectionFactory factory, boolean allowCreate, boolean bind, boolean transactionSupport) {
        Assert.notNull(factory, "No RedisConnectionFactory specified");
        RedisConnectionHolder conHolder = (RedisConnectionHolder) TransactionSynchronizationManager.getResource(factory);
        if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
            conHolder.requested();
            if (!conHolder.hasConnection()) {
                log.debug("Fetching resumed Redis Connection from RedisConnectionFactory");
                conHolder.setConnection(fetchConnection(factory));
            }
            return conHolder.getRequiredConnection();
        }
        // Else we either got no holder or an empty thread-bound holder here.
        if (!allowCreate) {
            throw new IllegalArgumentException("No connection found and allowCreate = false");
        }
        log.debug("Fetching Redis Connection from RedisConnectionFactory");
        RedisConnection connection = fetchConnection(factory);
        boolean bindSynchronization = TransactionSynchronizationManager.isActualTransactionActive() && transactionSupport;
        if (bind || bindSynchronization) {
            if (bindSynchronization && isActualNonReadonlyTransactionActive()) {
                // noinspection ReassignedVariable
                connection = createConnectionSplittingProxy(connection, factory);
            }
            try {
                // Use same RedisConnection for further Redis actions within the transaction.
                // Thread-bound object will get removed by synchronization at transaction completion.
                RedisConnectionHolder holderToUse = conHolder;
                if (holderToUse == null) {
                    holderToUse = new RedisConnectionHolder(connection);
                } else {
                    holderToUse.setConnection(connection);
                }
                holderToUse.requested();
                // Consider callback-scope connection binding vs. transaction scope binding
                if (bindSynchronization) {
                    potentiallyRegisterTransactionSynchronisation(holderToUse, factory);
                }
                if (holderToUse != conHolder) {
                    TransactionSynchronizationManager.bindResource(factory, holderToUse);
                }
            } catch (RuntimeException ex) {
                // Unexpected exception from external delegation call -> close Connection and rethrow.
                releaseConnection(connection, factory);
                throw ex;
            }
            return connection;
        }
        return connection;
    }

    /**
     * 实际上从给定的 {@link RedisConnectionFactory} 创建一个 {@link RedisConnection}。
     *
     * @param factory {@link RedisConnectionFactory} 从中获取 RedisConnections。
     * @return 来自给定 {@link RedisConnectionFactory} 的 Redis 连接（绝不是 {@literal null}）
     * @see RedisConnectionFactory#getConnection()
     */
    private static RedisConnection fetchConnection(RedisConnectionFactory factory) {
        return factory.getConnection();
    }

    private static void potentiallyRegisterTransactionSynchronisation(RedisConnectionHolder connHolder, final RedisConnectionFactory factory) {
        // 实际上应该进入 RedisTransactionManager
        if (!connHolder.isTransactionActive()) {
            connHolder.setTransactionActive(true);
            connHolder.setSynchronizedWithTransaction(true);
            connHolder.requested();
            RedisConnection conn = connHolder.getRequiredConnection();
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            if (!readOnly) {
                conn.multi();
            }
            TransactionSynchronizationManager.registerSynchronization(new RedisTransactionSynchronizer(connHolder, conn, factory, readOnly));
        }
    }

    private static boolean isActualNonReadonlyTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive() && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }

    private static RedisConnection createConnectionSplittingProxy(RedisConnection connection, RedisConnectionFactory factory) {
        return JdkProxyUtils.create()
                .setClassLoader(connection.getClass().getClassLoader())
                .addInterface(RedisConnectionProxy.class)
                .setInterceptor(new ConnectionSplittingInterceptor(connection, factory))
                .createProxy();
    }

    /**
     * 关闭给定的 {@link RedisConnection}，如果没有在外部管理（即未绑定到事务），则通过给定的工厂创建。
     *
     * @param conn    Redis 连接关闭。
     * @param factory 创建连接的 Redis 工厂。
     */
    public static void releaseConnection(RedisConnection conn, RedisConnectionFactory factory) {
        if (conn == null) {
            return;
        }
        RedisConnectionHolder conHolder = (RedisConnectionHolder) TransactionSynchronizationManager.getResource(factory);
        if (conHolder != null) {
            if (conHolder.isTransactionActive()) {
                if (connectionEquals(conHolder, conn)) {
                    if (log.isDebugEnabled()) {
                        log.debug("RedisConnection will be closed when transaction finished.");
                    }
                    // 这是事务连接：不要关闭它。
                    conHolder.released();
                }
                return;
            }
            // 释放事务性只读和非事务性非绑定连接。
            // 只读事务的事务连接没有注册同步器
            unbindConnection(factory);
            return;
        }
        doCloseConnection(conn);
    }

    /**
     * 确定给定的两个 RedisConnections 是否相等，在代理的情况下询问目标 {@link RedisConnection}。
     * 用于检测相等性，即使用户传入原始目标连接而持有的连接是代理。
     *
     * @param conHolder   {@link RedisConnectionHolder} 用于保留的连接（可能是代理）
     * @param passedInCon 用户传入的 {@link RedisConnection} （可能是没有代理的目标连接）
     * @return 给定的连接是否相等
     * @see #getTargetConnection
     */
    private static boolean connectionEquals(RedisConnectionHolder conHolder, RedisConnection passedInCon) {
        if (!conHolder.hasConnection()) {
            return false;
        }
        RedisConnection heldCon = conHolder.getRequiredConnection();
        return heldCon.equals(passedInCon) || getTargetConnection(heldCon).equals(passedInCon);
    }

    /**
     * 返回给定 {@link RedisConnection} 的最内层目标 {@link RedisConnection}。
     * 如果给定的 {@link RedisConnection} 是一个代理，它将被解包直到找到一个非代理 {@link RedisConnection}。
     * 否则，传入的 {@link RedisConnection} 将按原样返回。
     *
     * @param con {@link RedisConnection} 代理解包
     * @return 最里面的目标连接，如果没有代理，则传入一个
     * @see RedisConnectionProxy#getTargetConnection()
     */
    private static RedisConnection getTargetConnection(RedisConnection con) {
        RedisConnection conToUse = con;
        while (conToUse instanceof RedisConnectionProxy) {
            conToUse = ((RedisConnectionProxy) conToUse).getTargetConnection();
        }
        return conToUse;
    }

    /**
     * 从闭包范围解除绑定并关闭与给定工厂关联的连接（如果有）。
     * 考虑正在进行的事务，因此事务绑定连接不会关闭，并且可重入闭包范围绑定连接。
     * 只有最外层的调用才会导致释放和关闭连接。
     *
     * @param factory Redis factory
     */
    public static void unbindConnection(RedisConnectionFactory factory) {
        RedisConnectionHolder conHolder = (RedisConnectionHolder) TransactionSynchronizationManager.getResource(factory);
        if (conHolder == null) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Unbinding Redis Connection.");
        }
        if (conHolder.isTransactionActive()) {
            if (log.isDebugEnabled()) {
                log.debug("Redis Connection will be closed when outer transaction finished.");
            }
        } else {
            RedisConnection connection = conHolder.getConnection();
            conHolder.released();
            if (!conHolder.isOpen()) {
                TransactionSynchronizationManager.unbindResourceIfPossible(factory);
                doCloseConnection(connection);
            }
        }
    }

    /**
     * 返回给定的Redis连接是否是事务性的，即通过事务设施绑定到当前线程。
     *
     * @param conn        Redis 连接检查
     * @param connFactory 创建连接的 Redis 连接工厂
     * @return 连接是否是事务性的
     */
    public static boolean isConnectionTransactional(RedisConnection conn, RedisConnectionFactory connFactory) {
        Assert.notNull(connFactory, "No RedisConnectionFactory specified");
        RedisConnectionHolder connHolder = (RedisConnectionHolder) TransactionSynchronizationManager.getResource(connFactory);
        return connHolder != null && connectionEquals(connHolder, conn);
    }

    private static void doCloseConnection(RedisConnection connection) {
        if (connection == null) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Closing Redis Connection.");
        }
        try {
            connection.close();
        } catch (DataAccessException ex) {
            log.debug("Could not close Redis Connection", ex);
        } catch (Throwable ex) {
            log.debug("Unexpected exception on closing Redis Connection", ex);
        }
    }

    /**
     * {@link TransactionSynchronization} 确保关联的 {@link RedisConnection} 在事务完成后被释放
     */
    private static class RedisTransactionSynchronizer implements TransactionSynchronization {
        private final RedisConnectionHolder connHolder;
        private final RedisConnection connection;
        private final RedisConnectionFactory factory;
        private final boolean readOnly;

        RedisTransactionSynchronizer(RedisConnectionHolder connHolder, RedisConnection connection, RedisConnectionFactory factory, boolean readOnly) {
            this.connHolder = connHolder;
            this.connection = connection;
            this.factory = factory;
            this.readOnly = readOnly;
        }

        @Override
        public void afterCompletion(int status) {
            try {
                if (!readOnly) {
                    switch (status) {
                        case TransactionSynchronization.STATUS_COMMITTED:
                            connection.exec();
                            break;
                        case TransactionSynchronization.STATUS_ROLLED_BACK:
                        case TransactionSynchronization.STATUS_UNKNOWN:
                        default:
                            connection.discard();
                    }
                }
            } finally {
                if (log.isDebugEnabled()) {
                    log.debug("Closing bound connection after transaction completed with " + status);
                }
                connHolder.setTransactionActive(false);
                doCloseConnection(connection);
                TransactionSynchronizationManager.unbindResource(factory);
                connHolder.reset();
            }
        }
    }

    /**
     * {@link InvocationHandler} 在新的 {@link RedisConnection} 上调用只读命令，而读写命令在绑定连接上排队。
     */
    static class ConnectionSplittingInterceptor implements InvocationHandler {
        private final RedisConnection target;
        private final RedisConnectionFactory factory;

        public ConnectionSplittingInterceptor(RedisConnection target, RedisConnectionFactory factory) {
            this.target = target;
            this.factory = factory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return intercept(target, method, args);
        }

        public Object intercept(Object obj, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("getTargetConnection")) {
                // Handle getTargetConnection 方法：返回底层 RedisConnection。
                return obj;
            }
            RedisCommand commandToExecute = RedisCommand.failsafeCommandLookup(method.getName());
            if (isPotentiallyThreadBoundCommand(commandToExecute)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Invoke '%s' on bound connection", method.getName()));
                }
                return invoke(method, obj, args);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Invoke '%s' on unbound connection", method.getName()));
            }
            RedisConnection connection = factory.getConnection();
            try {
                return invoke(method, connection, args);
            } finally {
                // 执行命令后正确关闭未绑定连接
                if (!connection.isClosed()) {
                    doCloseConnection(connection);
                }
            }
        }

        private Object invoke(Method method, Object target, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private boolean isPotentiallyThreadBoundCommand(RedisCommand command) {
            return RedisCommand.UNKNOWN.equals(command) || !command.isReadonly();
        }
    }

    /**
     * 包装 {@link RedisConnection} 的资源持有者。
     * {@link RedisConnectionUtils} 将此类的实例绑定到线程，用于特定的 {@link RedisConnectionFactory}
     */
    private static class RedisConnectionHolder extends ResourceHolderSupport {
        private RedisConnection connection;
        private boolean transactionActive = false;

        /**
         * 假设没有正在进行的事务，为给定的 Redis 连接创建一个新的 RedisConnectionHolder
         *
         * @param connection 要保持的 Redis 连接
         */
        public RedisConnectionHolder(RedisConnection connection) {
            this.connection = connection;
        }

        /**
         * 返回此持有者当前是否有 {@link RedisConnection}
         */
        protected boolean hasConnection() {
            return (this.connection != null);
        }

        public RedisConnection getConnection() {
            return connection;
        }

        public RedisConnection getRequiredConnection() {
            RedisConnection connection = getConnection();
            if (connection == null) {
                throw new IllegalStateException("No active RedisConnection");
            }
            return connection;
        }

        /**
         * 用给定的 {@link RedisConnection} 覆盖现有的 {@link RedisConnection} 句柄。如果给定 {@literal null} 则重置句柄
         * <p>
         * 用于在挂起时释放连接（使用 {@code null} 参数）并在恢复时设置新连接
         */
        protected void setConnection(RedisConnection connection) {
            this.connection = connection;
        }

        /**
         * 设置此持有者是否代表一个活跃的托管交易
         *
         * @see org.clever.transaction.PlatformTransactionManager
         */
        protected void setTransactionActive(boolean transactionActive) {
            this.transactionActive = transactionActive;
        }

        /**
         * 返回此持有者是否代表一个活跃的托管交易
         */
        protected boolean isTransactionActive() {
            return this.transactionActive;
        }

        /**
         * 释放此ConnectionHolder持有的当前连接。
         * <p>
         * 这对于期望“连接借用”的ConnectionHandles是必要的，其中每个返回的连接都只是临时租用的，并且在数据操作完成后需要返回，以使连接可用于同一事务中的其他操作
         */
        @Override
        public void released() {
            super.released();
            if (!isOpen()) {
                setConnection(null);
            }
        }

        @Override
        public void clear() {
            super.clear();
            this.transactionActive = false;
        }
    }

    /**
     * {@link RedisConnection} 的子接口将由 {@link RedisConnection} 代理实现。允许访问底层目标 {@link RedisConnection}
     *
     * @see RedisConnectionUtils#getTargetConnection(RedisConnection)
     */
    public interface RedisConnectionProxy extends RedisConnection {
        /**
         * 返回此代理的目标 {@link RedisConnection}
         * <p>
         * 这通常是本机驱动程序 {@link RedisConnection} 或来自连接池的包装器
         *
         * @return 底层的 {@link RedisConnection}（从不{@link null}）
         */
        RedisConnection getTargetConnection();
    }
}
