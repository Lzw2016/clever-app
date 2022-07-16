package org.clever.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JdbcTemplate类使用的两个中央回调接口之一。
 * 该接口在给定连接的情况下创建一个PreparedStatement，该连接由JdbcTemplate类提供。
 * 实现负责提供SQL和任何必要的参数。
 *
 * <p>准备好的报表该连接由使用类提供。实现负责提供SQL和任何必要的参数。
 * 实现不需要担心它们尝试的操作可能引发的SQLException。
 * JdbcTemplate类将捕获并适当处理SQLException。
 *
 * <p>如果PreparedStatementCreator能够提供用于创建PreparedStatement的SQL，
 * 那么它还应该实现SqlProvider接口。
 * 这允许在异常情况下提供更好的上下文信息。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:31 <br/>
 *
 * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
 * @see JdbcTemplate#query(PreparedStatementCreator, RowCallbackHandler)
 * @see JdbcTemplate#update(PreparedStatementCreator)
 * @see SqlProvider
 */
@FunctionalInterface
public interface PreparedStatementCreator {
    /**
     * 在此连接中创建一条语句。允许实现使用PreparedStatements。JdbcTemplate将关闭创建的语句。
     *
     * @param con 用于创建语句的连接
     * @return 准备好的声明
     * @throws SQLException 不需要捕捉在该方法的实现中可能抛出的SQLException。JdbcTemplate类将处理它们。
     */
    PreparedStatement createPreparedStatement(Connection con) throws SQLException;
}
