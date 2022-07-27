package org.clever.jdbc.core;

import org.clever.dao.DataAccessException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 操作PreparedStatement的代码的通用回调接口。
 * 允许在单个PreparedStatement上执行任意数量的操作，
 * 例如单个{@code executeUpdate}调用或具有不同参数的重复{@code executeUpdate}调用。
 *
 * <p>JdbcTemplate内部使用，但对应用程序代码也很有用。
 * 请注意，传入的PreparedStatement可以由框架或自定义PreparedStatementCreator创建。
 * 然而，后者几乎没有必要，因为大多数自定义回调操作都将执行更新，在这种情况下，标准PreparedStatement就可以了。
 * 自定义操作始终会自行设置参数值，因此也不需要PreparedStatementCreator功能。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:31 <br/>
 *
 * @param <T> the result type
 * @see JdbcTemplate#execute(String, PreparedStatementCallback)
 * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {
    /**
     * 由{@code JdbcTemplate.execute}使用活动JDBC PreparedStatement调用。
     * 不需要关心关闭语句或连接，也不需要关心处理事务：这一切都将由clever的JdbcTemplate处理。
     * <p>注意：任何打开的结果集都应该在回调实现中的finally块中关闭。
     * 在回调返回后关闭语句对象，但这并不一定意味着ResultSet资源将被关闭：
     * 语句对象可能会被连接池汇集，{@code close}调用仅将对象返回到池中，而不会实际关闭资源。
     * <p>如果在没有线程绑定的JDBC事务（由DataSourceTransactionManager启动）的情况下调用，
     * 代码将简单地在具有事务语义的JDBC连接上执行。如果JdbcTemplate配置为使用JTA感知数据源，
     * 那么如果JTA事务处于活动状态，则JDBC连接和回调代码将是事务性的。
     * <p>允许返回在回调中创建的结果对象，即域对象或域对象的集合。
     * 注意，对单步操作有特殊支持：请参阅JdbcTemplate。queryForObject等。
     * 抛出的RuntimeException被视为应用程序异常，它被传播到模板的调用方。
     *
     * @param ps 活动JDBC PreparedStatement
     * @return 结果对象，如果没有，则为null
     * @throws SQLException        如果由JDBC方法引发，则由SQLExceptionTranslator自动转换为DataAccessException
     * @throws DataAccessException 在自定义例外情况下
     * @see JdbcTemplate#queryForList(String, Object[])
     */
    T doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException;
}
