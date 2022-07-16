package org.clever.jdbc.core;

import org.clever.dao.DataAccessException;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}的查询方法使用的回调接口。
 * 该接口的实现执行从{@link java.sql.ResultSet}提取结果的实际工作，但不需要担心异常处理。
 * {@link java.sql.SQLException SQLExceptions}将被捕获并由调用JdbcTemplate处理。
 *
 * <p>该接口主要用于JDBC框架本身。{@link RowMapper}通常是更简单的结果集处理选择，
 * 它为每行映射一个结果对象，而不是为整个结果集映射一个结果对象。
 *
 * <p>注意：与{@link RowCallbackHandler}不同，
 * 只要ResultSetExtractor对象不访问有状态资源（如LOB内容流时的输出流）或在对象内保持结果状态，
 * 它通常是无状态的，因此可以重用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:29 <br/>
 *
 * @param <T> the result type
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see RowMapper
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {
    /**
     * 实现必须实现此方法才能处理整个结果集。
     *
     * @param rs 要从中提取数据的结果集。实现不应该关闭它：它将被调用的JdbcTemplate关闭。
     * @return 任意结果对象，如果没有，则为null（在后一种情况下，提取器通常是有状态的）。
     * @throws SQLException        如果在获取列值或导航时遇到SQLException（也就是说，不需要捕捉SQLException）
     * @throws DataAccessException 在自定义例外情况下
     */
    T extractData(ResultSet rs) throws SQLException, DataAccessException;
}
