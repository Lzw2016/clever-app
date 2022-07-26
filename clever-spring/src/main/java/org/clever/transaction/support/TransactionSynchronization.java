package org.clever.transaction.support;

import org.clever.core.Ordered;

import java.io.Flushable;

/**
 * 用于事务同步回调的接口。由AbstractPlatformTransactionManager支持。
 *
 * <p>TransactionSynchronization实现可以实现有序接口以影响其执行顺序。
 * 未实现有序接口的同步将附加到同步链的末尾。
 *
 * <p>框架本身执行的系统同步使用特定的顺序值，允许与它们的执行顺序进行细粒度的交互（如有必要）
 *
 * <p>实现{@link Ordered}接口，以支持以声明方式控制同步的执行顺序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:33 <br/>
 *
 * @see TransactionSynchronizationManager
 * @see AbstractPlatformTransactionManager
 * @see org.clever.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
 */
public interface TransactionSynchronization extends Ordered, Flushable {
    /**
     * 正确提交时的完成状态
     */
    int STATUS_COMMITTED = 0;
    /**
     * 正确回滚时的完成状态
     */
    int STATUS_ROLLED_BACK = 1;
    /**
     * 启发式混合完成或系统错误情况下的完成状态
     */
    int STATUS_UNKNOWN = 2;

    /**
     * 返回此事务同步的执行顺序。
     * <p>默认值为 {@link Ordered#LOWEST_PRECEDENCE}.
     */
    @Override
    default double getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 暂停此同步。
     * <p>如果管理资源，则应解除资源与TransactionSynchronizationManager的绑定。
     *
     * @see TransactionSynchronizationManager#unbindResource
     */
    default void suspend() {
    }

    /**
     * 继续此同步。
     * <p>如果管理资源，应该将资源重新绑定到TransactionSynchronizationManager。
     *
     * @see TransactionSynchronizationManager#bindResource
     */
    default void resume() {
    }

    /**
     * 如果适用，将底层会话刷新到数据存储：例如，Hibernate/JPA会话。
     *
     * @see org.clever.transaction.TransactionStatus#flush()
     */
    @Override
    default void flush() {
    }

    /**
     * 在事务提交之前（在“beforeCompletion”）调用。例如，可以将事务会话或O/R映射刷新到数据库。
     * <p>此回调并不意味着将实际提交事务。调用此方法后，仍可能发生回滚决策。
     * 此回调旨在执行仅在提交仍有机会发生时才相关的工作，例如将SQL语句刷新到数据库。
     * <p>请注意，异常将传播到提交调用方，并导致事务回滚。
     *
     * @param readOnly 事务是否定义为只读事务
     * @throws RuntimeException 如有错误；将传播到调用方（注意：不要在此处抛出TransactionException子类！）
     * @see #beforeCompletion
     */
    default void beforeCommit(boolean readOnly) {
    }

    /**
     * 在事务commit/rollback之前调用。可以在事务完成之前执行资源清理。
     * <p>此方法将在{@code beforeCommit}之后调用，即使{@code beforeCommit}引发异常也是如此。
     * 此回调允许在事务完成之前关闭资源，以获得任何结果。
     *
     * @throws RuntimeException 如有错误；将被记录但不会传播（注意：不要在此处抛出TransactionException子类！）
     * @see #beforeCommit
     * @see #afterCompletion
     */
    default void beforeCompletion() {
    }

    /**
     * 在事务提交后调用。可以在主事务成功提交后立即执行进一步的操作。
     * <p>例如，可以提交在成功提交主事务之后应该进行的进一步操作，如确认消息或电子邮件。
     * <p>注意：事务将已经提交，但事务资源可能仍然处于活动状态并且可以访问。
     * 因此，此时触发的任何数据访问代码仍将“participate”原始事务，允许执行一些清理（不再有提交跟踪！），
     * 除非它明确声明需要在单独的事务中运行。因此：对于从此处调用的任何事务操作，使用{@code PROPAGATION_REQUIRES_NEW}
     *
     * @throws RuntimeException 如有错误；将传播到调用方（注意：不要在此处抛出TransactionException子类！）
     */
    default void afterCommit() {
    }

    /**
     * 在事务commit/rollback之后调用。可以在事务完成后执行资源清理。
     * <p>注意：事务将已经提交或回滚，但事务资源可能仍然处于活动状态并且可以访问。
     * 因此，此时触发的任何数据访问代码仍将“participate”原始事务，允许执行一些清理（不再有提交跟踪！），
     * 除非它明确声明需要在单独的事务中运行。因此：对于从此处调用的任何事务操作，使用{@code PROPAGATION_REQUIRES_NEW}
     *
     * @param status 根据{@code STATUS_*}常量的完成状态
     * @throws RuntimeException 如有错误；将被记录但不会传播（注意：不要在此处抛出TransactionException子类！）
     * @see #STATUS_COMMITTED
     * @see #STATUS_ROLLED_BACK
     * @see #STATUS_UNKNOWN
     * @see #beforeCompletion
     */
    default void afterCompletion(int status) {
    }
}
