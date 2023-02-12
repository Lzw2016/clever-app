package org.clever.transaction.support;

/**
 * 一个简单的 {@link org.clever.transaction.TransactionStatus} 实现。
 * 派生自 {@link AbstractTransactionStatus} 并添加显式 {@link #isNewTransaction() "newTransaction"} 标志。
 *
 * <p>任何预构建 {@link org.clever.transaction.PlatformTransactionManager} 实现均未使用此类。
 * 它主要作为自定义事务管理器实现的开始和作为测试事务代码的静态模拟
 * （作为模拟 {@code PlatformTransactionManager} 的一部分或作为参数传递到 {@link TransactionCallback} 进行测试）
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 22:19 <br/>
 *
 * @see TransactionCallback#doInTransaction
 */
public class SimpleTransactionStatus extends AbstractTransactionStatus {
    private final boolean newTransaction;

    /**
     * 创建一个新的 {@code SimpleTransactionStatus} 实例，表示一个新的事务。
     */
    public SimpleTransactionStatus() {
        this(true);
    }

    /**
     * 创建一个新的 {@code SimpleTransactionStatus} 实例
     *
     * @param newTransaction 是否指示新事务
     */
    public SimpleTransactionStatus(boolean newTransaction) {
        this.newTransaction = newTransaction;
    }

    @Override
    public boolean isNewTransaction() {
        return this.newTransaction;
    }
}
