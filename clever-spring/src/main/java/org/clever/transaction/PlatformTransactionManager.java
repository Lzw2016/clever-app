package org.clever.transaction;

/**
 * 事务基础架构中的中心接口。
 * <p>此策略接口的默认实现为{@link org.clever.jdbc.datasource.DataSourceTransactionManager},它可以作为其他事务策略的实现指南。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 19:59 <br/>
 */
public interface PlatformTransactionManager extends TransactionManager {
    /**
     * 根据指定的传播行为，返回当前活动的事务或创建新事务。
     * <p>请注意，隔离级别或超时等参数将仅应用于新事务，因此在参与活动事务时将被忽略。
     * <p>此外，并非每个事务管理器都支持所有事务定义设置：当遇到不支持的设置时，适当的事务管理器实现应该引发异常。
     * <p>上述规则的一个例外是只读标志，如果不支持显式只读模式，则应忽略该标志。本质上，只读标志只是潜在优化的提示。
     *
     * @param definition TransactionDefinition实例（默认情况下可以为null），描述传播行为、隔离级别、超时等
     * @return 表示新事务或当前事务的事务状态对象
     * @throws TransactionException             查找、创建或系统错误时
     * @throws IllegalTransactionStateException 如果给定的事务定义无法执行（例如，如果当前活动的事务与指定的传播行为冲突）
     * @see TransactionDefinition#getPropagationBehavior
     * @see TransactionDefinition#getIsolationLevel
     * @see TransactionDefinition#getTimeout
     * @see TransactionDefinition#isReadOnly
     */
    TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException;

    /**
     * 提交给定事务的状态。如果事务已以编程方式标记为仅回滚，请执行回滚。
     * <p>如果事务不是新事务，请省略提交以正确参与周围的事务。如果前一个事务已暂停以创建新事务，请在提交新事务后恢复前一个事务。
     * <p>请注意，当提交调用完成时，无论是正常还是引发异常，都必须完全完成并清理事务。在这种情况下，不应期望回滚调用。
     * <p>如果此方法引发TransactionException以外的异常，则某些提交前错误会导致提交尝试失败。
     * 例如，O/R映射工具可能在提交之前尝试刷新对数据库的更改，导致DataAccessException导致事务失败。
     * 在这种情况下，原始异常将传播到此提交方法的调用方
     *
     * @param status {@code getTransaction}方法返回的对象
     * @throws UnexpectedRollbackException      如果事务协调器启动了意外回滚
     * @throws TransactionSystemException       如果发生提交或系统错误（通常由基本资源故障引起）
     * @throws IllegalTransactionStateException 如果给定事务已完成（即提交或回滚）
     * @see TransactionStatus#setRollbackOnly
     */
    void commit(TransactionStatus status) throws TransactionException;

    /**
     * 执行给定事务的回滚。
     * <p>如果事务不是新事务，只需将其设置为回滚，以便正确参与周围的事务。
     * 如果前一个事务已暂停以创建新事务，请在回滚新事务后恢复前一个事务。
     * <p><b>如果提交引发异常，则不要对事务调用回滚。</b>
     * 即使在出现提交异常的情况下，当提交返回时，事务也已经完成并清理完毕。
     * 因此，提交失败后的回滚调用将导致非法TransactionStateException
     *
     * @param status {@code getTransaction}方法返回的对象
     * @throws TransactionSystemException       如果发生回滚或系统错误（通常由基本资源故障引起）
     * @throws IllegalTransactionStateException 如果给定事务已完成（即提交或回滚）
     */
    void rollback(TransactionStatus status) throws TransactionException;
}
