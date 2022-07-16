package org.clever.jdbc.core;

import org.clever.dao.DataAccessException;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * 用于操作JDBC语句的代码的通用回调接口。
 * 允许在单个语句上执行任意数量的操作，例如单个{@code executeUpdate}调用或具有不同SQL的重复{@code executeUpdate}调用。
 *
 * <p>JdbcTemplate内部使用，但对应用程序代码也很有用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:28 <br/>
 *
 * @param <T> the result type
 * @see JdbcTemplate#execute(StatementCallback)
 */
@FunctionalInterface
public interface StatementCallback<T> {
    /**
     * 由{@code JdbcTemplate.execute}使用活动JDBC语句调用。
     * 不需要关心关闭语句或连接，也不需要关心处理事务：这一切都将由JdbcTemplate处理。
     * <p>注意：任何打开的结果集都应该在回调实现中的finally块中关闭。
     * 在回调返回后关闭语句对象，但这并不一定意味着ResultSet资源将被关闭：
     * 语句对象可能会被连接池汇集，{@code close}调用仅将对象返回到池中，而不会实际关闭资源。
     * <p>如果在没有线程绑定的JDBC事务（由DataSourceTransactionManager启动）的情况下调用，
     * 代码将简单地在具有事务语义的JDBC连接上执行。
     * 如果JdbcTemplate配置为使用JTA感知数据源，那么如果JTA事务处于活动状态，则JDBC连接和回调代码将是事务性的。
     * <p>允许返回在回调中创建的结果对象，即域对象或域对象的集合。
     * 注意，对单步操作有特殊支持：请参阅JdbcTemplate.queryForObject等。
     * 抛出的RuntimeException被视为应用程序异常，它被传播到模板的调用方
     *
     * @param stmt 活动JDBC语句
     * @return 结果对象，如果没有，则为null
     * @throws SQLException        如果由JDBC方法引发，则由SQLExceptionTranslator自动转换为DataAccessException
     * @throws DataAccessException 在自定义例外情况下
     * @see JdbcTemplate#queryForObject(String, Class)
     * @see JdbcTemplate#queryForRowSet(String)
     */
    T doInStatement(Statement stmt) throws SQLException, DataAccessException;
}
