package org.clever.transaction.support;

import org.clever.transaction.PlatformTransactionManager;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.TransactionException;

/**
 * {@link org.clever.transaction.PlatformTransactionManager} 接口的扩展，公开了在事务中执行给定回调的方法
 *
 * <p>此接口的实现者会自动表达对回调的偏好，
 * 而不是编程式 {@code getTransaction}、{@code commit} 和 {@code rollback} 调用。
 * 调用代码可以检查给定的事务管理器是否实现此接口以选择准备回调而不是显式事务分界控制。
 *
 * <p>{@link TransactionTemplate}自动检测并使用这个 PlatformTransactionManager 变体。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 22:09 <br/>
 *
 * @see TransactionTemplate
 */
public interface CallbackPreferringPlatformTransactionManager extends PlatformTransactionManager {
    /**
     * 在事务中执行给定回调对象指定的操作。
     * <p>允许返回在事务中创建的结果对象，即域对象或域对象集合。
     * 回调抛出的 RuntimeException 被视为强制回滚的致命异常。这样的异常会传播到模板的调用者。
     *
     * @param definition 用于包装回调的事务的定义
     * @param callback   指定事务操作的回调对象
     * @return 回调返回的结果对象，如果没有则返回 {@code null}
     * @throws TransactionException 在初始化、回滚或系统错误的情况下
     * @throws RuntimeException     如果由 TransactionCallback 抛出
     */
    <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) throws TransactionException;
}
