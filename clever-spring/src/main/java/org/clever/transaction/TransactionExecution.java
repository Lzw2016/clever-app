package org.clever.transaction;

/**
 * 事务当前状态的通用表示。
 * <p>为{@link TransactionStatus}和{@code ReactiveTransaction}提供基本接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:19 <br/>
 */
public interface TransactionExecution {
    /**
     * 返回当前事务是否为新事务；以其他方式参与现有事务，或可能首先不在实际事务中运行。
     */
    boolean isNewTransaction();

    /**
     * 仅设置事务回滚。这指示事务管理器，事务的唯一可能结果可能是回滚，作为引发异常的替代方法，异常反过来会触发回滚。
     */
    void setRollbackOnly();

    /**
     * 返回事务是否已标记为仅回滚（由应用程序或事务基础结构）
     */
    boolean isRollbackOnly();

    /**
     * 返回此事务是否已完成，即是否已提交或回滚。
     */
    boolean isCompleted();
}
