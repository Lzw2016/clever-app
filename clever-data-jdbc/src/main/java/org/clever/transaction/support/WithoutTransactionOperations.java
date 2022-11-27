package org.clever.transaction.support;

import org.clever.transaction.TransactionException;
import org.clever.transaction.TransactionStatus;

import java.util.function.Consumer;

/**
 * {@link TransactionOperations} 实现，它在没有实际事务的情况下执行给定的 {@link TransactionCallback}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 22:18 <br/>
 *
 * @see TransactionOperations#withoutTransaction()
 */
final class WithoutTransactionOperations implements TransactionOperations {
    static final WithoutTransactionOperations INSTANCE = new WithoutTransactionOperations();

    private WithoutTransactionOperations() {
    }

    @Override
    public <T> T execute(TransactionCallback<T> action) throws TransactionException {
        return action.doInTransaction(new SimpleTransactionStatus(false));
    }

    @Override
    public void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
        action.accept(new SimpleTransactionStatus(false));
    }
}
