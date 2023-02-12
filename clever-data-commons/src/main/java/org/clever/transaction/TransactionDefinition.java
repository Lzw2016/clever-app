package org.clever.transaction;

/**
 * 定义兼容事务属性的接口。
 * <p>请注意，除非启动实际的新事务，否则不会应用隔离级别和超时设置。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:13 <br/>
 *
 * @see PlatformTransactionManager#getTransaction(TransactionDefinition)
 * @see org.clever.transaction.support.DefaultTransactionDefinition
 */
public interface TransactionDefinition {
    /**
     * 当前如果有事务，就会使用该事务；否则会开始一个新事务
     */
    int PROPAGATION_REQUIRED = 0;
    /**
     * 当前如果有事务，就会使用该事务；否则不会开始一个新事务
     *
     * @see org.clever.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
     * @see org.clever.transaction.support.AbstractPlatformTransactionManager#SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
     */
    int PROPAGATION_SUPPORTS = 1;
    /**
     * 当前如果有事务，就会使用该事务；否则会抛出异常
     */
    int PROPAGATION_MANDATORY = 2;
    /**
     * 总是开始一个新事务。如果当前有事务，则该事务挂起
     */
    int PROPAGATION_REQUIRES_NEW = 3;
    /**
     * 不会执行事务中的代码。代码总是在非事务环境下执行，如果当前有事务，则该事务挂起
     */
    int PROPAGATION_NOT_SUPPORTED = 4;
    /**
     * 即使当前有事务，也会在非事务环境下执行。如果当前有事务，则抛出异常
     */
    int PROPAGATION_NEVER = 5;
    /**
     * 如果当前有事务，则在嵌套事务中执行。如果没有，那么执行情况与{@code PROPAGATION_REQUIRED}一样
     *
     * @see org.clever.jdbc.datasource.DataSourceTransactionManager
     */
    int PROPAGATION_NESTED = 6;
    /**
     * PlatformTransactionManager默认隔离级别
     * <p>对大多数数据库来说就是 ISOLATION_READ_COMMITTED
     *
     * @see java.sql.Connection
     */
    int ISOLATION_DEFAULT = -1;
    /**
     * 最低的隔离级别。读未提交
     *
     * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
     */
    int ISOLATION_READ_UNCOMMITTED = 1;  // 等同于 java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
    /**
     * 大多数数据库的默认级别。读已提交
     *
     * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
     */
    int ISOLATION_READ_COMMITTED = 2;  // 等同于 java.sql.Connection.TRANSACTION_READ_COMMITTED;
    /**
     * 可重复读
     *
     * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
     */
    int ISOLATION_REPEATABLE_READ = 4;  // 等同于 java.sql.Connection.TRANSACTION_REPEATABLE_READ;
    /**
     * 序列化
     *
     * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
     */
    int ISOLATION_SERIALIZABLE = 8;  // 等同于 java.sql.Connection.TRANSACTION_SERIALIZABLE;
    /**
     * 使用基础事务系统的默认超时，如果不支持超时，则使用无
     */
    int TIMEOUT_DEFAULT = -1;

    /**
     * 返回传播行为。
     * <p>默认值为 {@link #PROPAGATION_REQUIRED}
     *
     * @see #PROPAGATION_REQUIRED
     * @see org.clever.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
     */
    default int getPropagationBehavior() {
        return PROPAGATION_REQUIRED;
    }

    /**
     * 返回隔离级别。
     * <p>默认值为 {@link #ISOLATION_DEFAULT}
     *
     * @see #ISOLATION_DEFAULT
     * @see org.clever.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
     */
    default int getIsolationLevel() {
        return ISOLATION_DEFAULT;
    }

    /**
     * 返回事务超时。
     * <p>默认值为 {@link #TIMEOUT_DEFAULT}
     */
    default int getTimeout() {
        return TIMEOUT_DEFAULT;
    }

    /**
     * 返回是否作为只读事务进行优化。
     * <p>默认为 false
     *
     * @see org.clever.transaction.support.TransactionSynchronization#beforeCommit(boolean)
     * @see org.clever.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
     */
    default boolean isReadOnly() {
        return false;
    }

    /**
     * 返回此事务的名称。可以是 {@code null}
     * <p>默认为 null
     *
     * @see org.clever.transaction.support.TransactionSynchronizationManager#getCurrentTransactionName()
     */
    default String getName() {
        return null;
    }

    // Static builder methods

    /**
     * 返回具有默认值的不可修改的{@code TransactionDefinition}。
     * <p>出于自定义目的，请使用可修改的{@link org.clever.transaction.support.DefaultTransactionDefinition}
     */
    static TransactionDefinition withDefaults() {
        return StaticTransactionDefinition.INSTANCE;
    }
}
