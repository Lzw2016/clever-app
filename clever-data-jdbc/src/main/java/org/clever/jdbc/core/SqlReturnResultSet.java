package org.clever.jdbc.core;

/**
 * 表示从存储过程调用返回的{@link java.sql.ResultSet}。
 *
 * <p>必须提供{@link ResultSetExtractor}, {@link RowCallbackHandler}或{@link RowMapper}来处理任何返回的行。
 *
 * <p>与所有存储过程参数一样，返回的{@link java.sql.ResultSet ResultSets}必须有名称。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:01 <br/>
 */
public class SqlReturnResultSet extends ResultSetSupportingSqlParameter {
    /**
     * 创建的新{@link SqlReturnResultSet}实例
     *
     * @param name      输入和输出Map中使用的参数名称
     * @param extractor 用于解析{@link java.sql.ResultSet}的{@link ResultSetExtractor}
     */
    public SqlReturnResultSet(String name, ResultSetExtractor<?> extractor) {
        super(name, 0, extractor);
    }

    /**
     * 创建的新{@link SqlReturnResultSet}实例
     *
     * @param name    输入和输出Map中使用的参数名称
     * @param handler 用于解析{@link java.sql.ResultSet}的{@link RowCallbackHandler}
     */
    public SqlReturnResultSet(String name, RowCallbackHandler handler) {
        super(name, 0, handler);
    }

    /**
     * 创建的新{@link SqlReturnResultSet}实例
     *
     * @param name   输入和输出Map中使用的参数名称
     * @param mapper 用于解析{@link java.sql.ResultSet}的{@link RowMapper}
     */
    public SqlReturnResultSet(String name, RowMapper<?> mapper) {
        super(name, 0, mapper);
    }

    /**
     * 此实现始终返回 {@code true}.
     */
    @Override
    public boolean isResultsParameter() {
        return true;
    }
}
