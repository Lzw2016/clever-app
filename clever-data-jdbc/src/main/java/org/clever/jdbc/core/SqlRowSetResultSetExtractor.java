package org.clever.jdbc.core;

import org.clever.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.clever.jdbc.support.rowset.SqlRowSet;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link ResultSetExtractor}实现，为每个给定的{@link ResultSet}返回{@link SqlRowSet}表示。
 *
 * <p>默认实现在下面使用标准的JDBC CachedRowSet。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:42 <br/>
 *
 * @see #newCachedRowSet
 * @see SqlRowSet
 * @see JdbcTemplate#queryForRowSet(String)
 * @see CachedRowSet
 */
public class SqlRowSetResultSetExtractor implements ResultSetExtractor<SqlRowSet> {
    private static final RowSetFactory rowSetFactory;

    static {
        try {
            rowSetFactory = RowSetProvider.newFactory();
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot create RowSetFactory through RowSetProvider", ex);
        }
    }

    @Override
    public SqlRowSet extractData(ResultSet rs) throws SQLException {
        return createSqlRowSet(rs);
    }

    /**
     * 创建一个{@link ResultSet}，用于包装给定的{@link SqlRowSet}，以断开连接的方式表示其数据。
     * <p>这个实现创建了一个{@link ResultSetWrappingSqlRowSet}实例，
     * 它封装了一个标准的JDBC {@link CachedRowSet}实例。可以重写以使用其他实现。
     *
     * @param rs 原始结果集（已连接）
     * @return 断开连接的SqlRowSet
     * @throws SQLException 如果由JDBC方法引发
     * @see #newCachedRowSet()
     * @see ResultSetWrappingSqlRowSet
     */
    protected SqlRowSet createSqlRowSet(ResultSet rs) throws SQLException {
        CachedRowSet rowSet = newCachedRowSet();
        rowSet.populate(rs);
        return new ResultSetWrappingSqlRowSet(rowSet);
    }

    /**
     * 创建一个新的{@link CachedRowSet}实例，由{@code createSqlRowSet}实现填充。
     * <p>默认实现使用JDBC 4.1 {@link RowSetFactory}.
     *
     * @return 一个新的CachedRowSet实例
     * @throws SQLException 如果由JDBC方法引发
     * @see #createSqlRowSet
     * @see RowSetProvider#newFactory()
     * @see RowSetFactory#createCachedRowSet()
     */
    protected CachedRowSet newCachedRowSet() throws SQLException {
        return rowSetFactory.createCachedRowSet();
    }
}
