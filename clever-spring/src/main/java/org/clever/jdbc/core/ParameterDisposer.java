package org.clever.jdbc.core;

/**
 * 接口将由对象实现，这些对象可以关闭由参数（如{@code SqlLobValue}对象）分配的资源。
 *
 * <p>通常由{@code PreparedStatementCreators}和{@code PreparedStatementSetters}实现，
 * 它们支持可处置的{@link DisposableSqlTypeValue}对象（例如{@code SqlLobValue}）作为参数。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:44 <br/>
 *
 * @see PreparedStatementCreator
 * @see PreparedStatementSetter
 * @see DisposableSqlTypeValue
 */
public interface ParameterDisposer {
    /**
     * 关闭由实现对象持有的参数分配的资源，例如在DisposableSqlTypeValue（如SqlLobValue）的情况下。
     *
     * @see DisposableSqlTypeValue#cleanup()
     */
    void cleanupParameters();
}
