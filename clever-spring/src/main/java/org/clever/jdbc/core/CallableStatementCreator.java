package org.clever.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JdbcTemplate类使用的三个中央回调接口之一。
 * 该接口在给定连接的情况下创建一个CallableStatement，该连接由JdbcTemplate类提供。
 * 实现负责提供SQL和任何必要的参数。
 *
 * <p>实现不需要关心它们尝试的操作可能引发的SQLException。
 * JdbcTemplate类将捕获并适当处理SQLException。
 *
 * <p>如果PreparedStatementCreator能够提供用于创建PreparedStatement的SQL，
 * 那么它还应该实现SqlProvider接口。这允许在异常情况下提供更好的上下文信息。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:37 <br/>
 *
 * @see JdbcTemplate#execute(CallableStatementCreator, CallableStatementCallback)
 * @see JdbcTemplate#call
 * @see SqlProvider
 */
@FunctionalInterface
public interface CallableStatementCreator {
    /**
     * 在此连接中创建可调用语句。允许实现使用CallableStatements。
     *
     * @param con 用于创建statement的连接
     * @throws SQLException 不需要捕捉在该方法的实现中可能抛出的SQLException。JdbcTemplate类将处理它们。
     */
    CallableStatement createCallableStatement(Connection con) throws SQLException;
}
