package org.clever.transaction.support;

import org.clever.transaction.TransactionStatus;

/**
 * 事务代码的回调接口。与 {@link TransactionTemplate} 的 {@code execute} 方法一起使用，通常作为方法实现中的匿名类。
 *
 * <p>通常用于将对不了解事务的数据访问服务的各种调用组装到具有事务划分的更高级别的服务方法中。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 22:07 <br/>
 *
 * @param <T> the result type
 * @see TransactionTemplate
 * @see CallbackPreferringPlatformTransactionManager
 */
@FunctionalInterface
public interface TransactionCallback<T> {
    /**
     * 在事务上下文中由 {@link TransactionTemplate#execute} 调用。
     * 不需要关心事务本身，尽管它可以通过给定的状态对象检索和影响当前事务的状态，例如设置回滚。
     * <p>允许返回在事务中创建的结果对象，即域对象或域对象的集合。
     * 回调抛出的 RuntimeException 被视为强制回滚的应用程序异常。
     * 任何此类异常都将传播到模板的调用者，除非回滚出现问题，在这种情况下将抛出 TransactionException。
     *
     * @param status 关联事务状态
     * @return 结果对象，或 {@code null}
     * @see TransactionTemplate#execute
     * @see CallbackPreferringPlatformTransactionManager#execute
     */
    T doInTransaction(TransactionStatus status);
}
