package org.clever.transaction.annotation;

import org.clever.transaction.TransactionDefinition;

/**
 * 事务传递性行为的枚举，对应于 {@link TransactionDefinition} 接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/21 15:02 <br/>
 */
public enum Propagation {
    /**
     * 支持当前事务，如果不存在则创建一个新事务。
     * <p>这是事务注释的默认设置。
     */
    REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),

    /**
     * 支持当前事务，如果不存在则以非事务方式执行。
     *
     * @see org.clever.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
     */
    SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),

    /**
     * 支持当前事务，如果不存在则抛出异常。
     */
    MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),

    /**
     * 创建一个新事务，如果存在则暂停当前事务。
     */
    REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),

    /**
     * 以非事务方式执行，如果存在则挂起当前事务。
     */
    NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

    /**
     * 以非事务方式执行，如果存在事务则抛出异常。
     */
    NEVER(TransactionDefinition.PROPAGATION_NEVER),

    /**
     * 如果当前事务存在，则在嵌套事务中执行，否则表现得像{@code REQUIRED}。
     *
     * @see org.clever.jdbc.datasource.DataSourceTransactionManager
     */
    NESTED(TransactionDefinition.PROPAGATION_NESTED);

    private final int value;

    Propagation(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
