package org.clever.jdbc.core;

import org.clever.dao.DataAccessException;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * 对CallableStatement进行操作的代码的通用回调接口。
 * 允许在单个CallableStatement上执行任意数量的操作，例如单个execute调用或具有不同参数的重复execute调用。
 *
 * <p>JdbcTemplate内部使用，但对应用程序代码也很有用。
 * 注意，传入的CallableStatement可以由框架或自定义CallableStatementCreator创建。
 * 然而，后者几乎没有必要，因为大多数自定义回调操作将执行更新，
 * 在这种情况下，标准CallableStatement就可以了。
 * 自定义操作总是自己设置参数值，因此也不需要CallableStatementCreator功能。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:37 <br/>
 *
 * @param <T> the result type
 * @see JdbcTemplate#execute(String, CallableStatementCallback)
 * @see JdbcTemplate#execute(CallableStatementCreator, CallableStatementCallback)
 */
@FunctionalInterface
public interface CallableStatementCallback<T> {
    /**
     * 被{@code JdbcTemplate.execute}调用。使用活动的JDBC CallableStatement执行。
     * 不需要关心关闭语句或连接，也不需要关心处理事务：这一切都将由JdbcTemplate处理。
     *
     * <p>注意：任何打开的结果集都应该在回调实现中的finally块中关闭。
     * 将在回调返回后关闭语句对象，但这并不一定意味着ResultSet资源将被关闭：
     * 语句对象可能会被连接池汇集，{@code close}调用仅将对象返回到池中，而不会实际关闭资源。
     *
     * <p>如果在没有线程绑定的JDBC事务（由DataSourceTransactionManager启动）的情况下调用，
     * 代码将简单地在具有事务语义的JDBC连接上执行。如果JdbcTemplate配置为使用JTA感知数据源，
     * 那么如果JTA事务处于活动状态，则JDBC连接和回调代码将是事务性的。
     *
     * <p>允许返回在回调中创建的结果对象，即域对象或域对象的集合。
     * 抛出的RuntimeException被视为应用程序异常：它被传播到模板的调用方。
     *
     * @param cs 活动JDBC CallableStatement
     * @return 结果对象，如果没有，则为null
     * @throws SQLException        如果由JDBC方法引发，则由SQLExceptionTranslator自动转换为DataAccessException
     * @throws DataAccessException 在自定义例外情况下
     */
    T doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException;
}
