package org.clever.transaction;

/**
 * 指定API以通用方式以编程方式管理事务保存点。由TransactionStatus扩展以公开特定事务的保存点管理功能。
 * <p>请注意，保存点只能在活动事务中工作。只需使用此编程保存点处理即可满足高级需求；
 * 否则，最好使用带有PROPAGATION_NESTED的子事务。
 * <p>此接口的灵感来自JDBC 3.0的保存点机制，但它独立于任何特定的持久性技术。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:18 <br/>
 *
 * @see TransactionStatus
 * @see TransactionDefinition#PROPAGATION_NESTED
 * @see java.sql.Savepoint
 */
public interface SavepointManager {
    /**
     * 创建新的保存点。
     * 您可以通过{@code rollbackToSavepoint}回滚到特定的保存点，并通过{@code releaseSavepoint}显式释放不再需要的保存点。
     * 请注意，大多数事务管理器将在事务完成时自动释放保存点。
     *
     * @return 要传递到{@link #rollbackToSavepoint}或{@link #releaseSavepoint}的保存点对象
     * @throws NestedTransactionNotSupportedException 如果基础事务不支持保存点
     * @throws TransactionException                   如果无法创建保存点，例如因为事务未处于适当的状态
     * @see java.sql.Connection#setSavepoint
     */
    Object createSavepoint() throws TransactionException;

    /**
     * 回滚到给定的保存点。
     * <p>保存点随后不会自动释放。
     * 您可以显式调用{@link #releaseSavepoint(Object)}，或者依赖于事务完成时的自动释放
     *
     * @param savepoint 要回滚到的保存点
     * @throws NestedTransactionNotSupportedException 如果基础事务不支持保存点
     * @throws TransactionException                   如果回滚失败
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    void rollbackToSavepoint(Object savepoint) throws TransactionException;

    /**
     * 显式释放给定的保存点。
     * <p>请注意，大多数事务管理器将在事务完成时自动释放保存点。
     * <p>如果在事务完成时最终会进行适当的资源清理，那么实现应该尽可能安静地失败。
     *
     * @param savepoint 要释放的保存点
     * @throws NestedTransactionNotSupportedException 如果基础事务不支持保存点
     * @throws TransactionException                   如果发布失败
     * @see java.sql.Connection#releaseSavepoint
     */
    void releaseSavepoint(Object savepoint) throws TransactionException;
}
