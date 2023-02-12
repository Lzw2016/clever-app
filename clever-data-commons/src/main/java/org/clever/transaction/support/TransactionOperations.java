package org.clever.transaction.support;

import org.clever.transaction.TransactionException;
import org.clever.transaction.TransactionStatus;

import java.util.function.Consumer;

/**
 * 指定基本事务执行操作的接口。
 * 由 {@link TransactionTemplate} 实现。
 * 不经常直接使用，但它是增强可测试性的有用选项，因为它很容易
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 22:16 <br/>
 */
public interface TransactionOperations {
    /**
     * 在事务中执行给定回调对象指定的操作。
     * <p>允许返回在事务中创建的结果对象，即域对象或域对象集合。
     * 回调抛出的 RuntimeException 被视为强制回滚的致命异常。
     * 这样的异常会传播到模板的调用者。
     *
     * @param action 指定事务操作的回调对象
     * @return 回调返回的结果对象，如果没有则返回 {@code null}
     * @throws TransactionException 在初始化、回滚或系统错误的情况下
     * @throws RuntimeException     如果由 TransactionCallback 抛出
     * @see #executeWithoutResult(Consumer)
     */
    <T> T execute(TransactionCallback<T> action) throws TransactionException;

    /**
     * 在事务中执行给定 {@link Runnable} 指定的操作。
     * <p>如果您需要从回调中返回一个对象或从回调中访问 {@link org.clever.transaction.TransactionStatus}，
     * 请改用 {@link #execute(TransactionCallback)}。
     * <p>此变体类似于使用 {@link TransactionCallbackWithoutResult}
     * 但具有针对常见情况的简化签名 - 并且可以方便地与 Java 8 lambda 表达式一起使用。
     *
     * @param action 指定事务操作的 Runnable
     * @throws TransactionException 在初始化、回滚或系统错误的情况下
     * @throws RuntimeException     如果由 Runnable 抛出
     * @see #execute(TransactionCallback)
     * @see TransactionCallbackWithoutResult
     */
    default void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
        execute(status -> {
            action.accept(status);
            return null;
        });
    }

    /**
     * 返回 {@code TransactionOperations} 接口的实现，它在没有实际事务的情况下执行给定的 {@link TransactionCallback}。
     * <p>对测试有用：该行为等同于使用没有实际事务 (PROPAGATION_SUPPORTS) 且没有同步 (SYNCHRONIZATION_NEVER) 的事务管理器运行。
     * <p>对于具有实际事务处理的 {@link TransactionOperations} 实现，
     * 请使用 {@link TransactionTemplate} 和适当的 {@link org.clever.transaction.PlatformTransactionManager}。
     *
     * @see org.clever.transaction.TransactionDefinition#PROPAGATION_SUPPORTS
     * @see AbstractPlatformTransactionManager#SYNCHRONIZATION_NEVER
     * @see TransactionTemplate
     */
    static TransactionOperations withoutTransaction() {
        return WithoutTransactionOperations.INSTANCE;
    }
}
