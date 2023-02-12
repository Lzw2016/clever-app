package org.clever.transaction.annotation;

import org.clever.transaction.TransactionDefinition;

/**
 * 事务隔离级别的枚举，对应于 {@link TransactionDefinition} 接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/21 15:08 <br/>
 */
public enum Isolation {
    /**
     * 使用底层数据存储的默认隔离级别。所有其他级别对应于 JDBC 隔离级别。
     *
     * @see java.sql.Connection
     */
    DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),

    /**
     * 一个常量，指示可能发生脏读、不可重复读和幻读。
     * 此级别允许由一个事务更改的行在提交该行的任何更改之前由另一个事务读取（“脏读”）。
     * 如果任何更改被回滚，则第二个事务将检索到无效行。
     *
     * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
     */
    READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),

    /**
     * 表示防止脏读的常量；可能会出现不可重复读取和幻读。
     * 此级别仅禁止事务读取其中包含未提交更改的行。
     *
     * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
     */
    READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),

    /**
     * 一个常量，表示防止脏读和不可重复读；可能会出现幻读。
     * 这个级别禁止一个事务读取其中有未提交更改的行，它也禁止一个事务读取一行，第二个事务修改该行，
     * 第一个事务重新读取该行，第二次得到不同值的情况（ “不可重复读取”）。
     *
     * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
     */
    REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),

    /**
     * 一个常量，表示防止脏读、不可重复读和幻读。
     * 此级别包括 {@code ISOLATION_REPEATABLE_READ} 中的禁令，并进一步禁止以下情况：一个事务读取满足 {@code WHERE} 条件的所有行，
     * 第二个事务插入满足该 {@code WHERE} 条件的行，并且第一个事务针对相同的条件重新读取，在第二次读取中检索额外的“幻影”行。
     *
     * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
     */
    SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE);

    private final int value;

    Isolation(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
