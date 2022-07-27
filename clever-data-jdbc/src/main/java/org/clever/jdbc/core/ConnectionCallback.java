package org.clever.jdbc.core;

import org.clever.dao.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 用于在JDBC连接上操作的代码的通用回调接口。允许使用任何类型和数量的语句在单个连接上执行任意数量的操作。
 *
 * <p>这对于委托给现有的数据访问代码特别有用，这些代码需要连接并引发SQLException。
 * 对于新编写的代码，强烈建议使用JdbcTemplate的更具体的操作，例如{@code query}或{@code update}变体。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:28 <br/>
 *
 * @param <T> the result type
 * @see JdbcTemplate#execute(ConnectionCallback)
 * @see JdbcTemplate#query
 * @see JdbcTemplate#update
 */
@FunctionalInterface
public interface ConnectionCallback<T> {
    /**
     * 由具有活动JDBC连接的{@code JdbcTemplate.execute}调用。不需要关心激活或关闭连接或处理事务。
     * <p>如果在没有线程绑定的JDBC事务（由DataSourceTransactionManager启动）的情况下调用，
     * 代码将简单地在具有事务语义的JDBC连接上执行。如果JdbcTemplate配置为使用JTA感知数据源，
     * 那么如果JTA事务处于活动状态，则JDBC连接和回调代码将是事务性的。
     * <p>允许返回在回调中创建的结果对象，即域对象或域对象的集合。
     * 注意，对单步操作有特殊支持：请参阅{@code JdbcTemplate.queryForObject}等。
     * 抛出的RuntimeException被视为应用程序异常：它被传播到模板的调用方。
     *
     * @param con 活动JDBC连接
     * @return 结果对象，如果没有，则为null
     * @throws SQLException        如果由JDBC方法引发，则由SQLExceptionTranslator自动转换为DataAccessException
     * @throws DataAccessException 在自定义例外情况下
     * @see JdbcTemplate#queryForObject(String, Class)
     * @see JdbcTemplate#queryForRowSet(String)
     */
    T doInConnection(Connection con) throws SQLException, DataAccessException;
}
