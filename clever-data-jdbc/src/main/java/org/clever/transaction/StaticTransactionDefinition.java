package org.clever.transaction;

/**
 * 静态的不可修改的事务定义
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:14 <br/>
 *
 * @see TransactionDefinition#withDefaults()
 */
final class StaticTransactionDefinition implements TransactionDefinition {
    static final StaticTransactionDefinition INSTANCE = new StaticTransactionDefinition();

    private StaticTransactionDefinition() {
    }
}
