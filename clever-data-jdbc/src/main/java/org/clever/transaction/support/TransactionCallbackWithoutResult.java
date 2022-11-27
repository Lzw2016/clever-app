package org.clever.transaction.support;

import org.clever.transaction.TransactionStatus;

/**
 * TransactionCallback 实现的简单便利类。
 * 允许实现没有结果的 doInTransaction 版本，即不需要返回语句。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 22:21 <br/>
 *
 * @see TransactionTemplate
 */
public abstract class TransactionCallbackWithoutResult implements TransactionCallback<Object> {
    @Override
    public final Object doInTransaction(TransactionStatus status) {
        doInTransactionWithoutResult(status);
        return null;
    }

    /**
     * 在事务上下文中由 {@code TransactionTemplate.execute} 调用。
     * 不需要关心事务本身，尽管它可以通过给定的状态对象检索和影响当前事务的状态，例如设置回滚。
     * <p>回调抛出的 RuntimeException 被视为强制回滚的应用程序异常。异常会传播到模板的调用者。
     * <p>使用 JTA 时请注意：JTA 事务仅适用于事务性 JNDI 资源，因此如果实现需要事务支持，则需要使用此类资源。
     *
     * @param status 关联事务状态
     * @see TransactionTemplate#execute
     */
    protected abstract void doInTransactionWithoutResult(TransactionStatus status);
}
