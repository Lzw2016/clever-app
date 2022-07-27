package org.clever.jdbc.core;

import java.sql.ResultSet;

/**
 * ResultSet的公共基类支持SqlParameters，如 {@link SqlOutParameter} 和 {@link SqlReturnResultSet}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:01 <br/>
 */
public class ResultSetSupportingSqlParameter extends SqlParameter {
    private ResultSetExtractor<?> resultSetExtractor;
    private RowCallbackHandler rowCallbackHandler;
    private RowMapper<?> rowMapper;

    /**
     * 创建新的ResultSetSupportingSqlParameter。
     *
     * @param name    输入和输出映射中使用的参数名称
     * @param sqlType 参数SQL类型根据 {@code java.sql.Types}
     */
    public ResultSetSupportingSqlParameter(String name, int sqlType) {
        super(name, sqlType);
    }

    /**
     * 创建新的ResultSetSupportingSqlParameter。
     *
     * @param name    输入和输出映射中使用的参数名称
     * @param sqlType 参数SQL类型根据 {@code java.sql.Types}
     * @param scale   小数点后的位数 (对于DECIMAL和NUMERIC类型)
     */
    public ResultSetSupportingSqlParameter(String name, int sqlType, int scale) {
        super(name, sqlType, scale);
    }

    /**
     * 创建新的ResultSetSupportingSqlParameter。
     *
     * @param name     输入和输出映射中使用的参数名称
     * @param sqlType  参数SQL类型根据 {@code java.sql.Types}
     * @param typeName 参数的类型名称（可选）
     */
    public ResultSetSupportingSqlParameter(String name, int sqlType, String typeName) {
        super(name, sqlType, typeName);
    }

    /**
     * 创建新的ResultSetSupportingSqlParameter。
     *
     * @param name    输入和输出映射中使用的参数名称
     * @param sqlType 参数SQL类型根据 {@code java.sql.Types}
     * @param rse     用于分析{@link ResultSet}的{@link ResultSetExtractor}
     */
    public ResultSetSupportingSqlParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
        super(name, sqlType);
        this.resultSetExtractor = rse;
    }

    /**
     * 创建新的ResultSetSupportingSqlParameter。
     *
     * @param name    输入和输出映射中使用的参数名称
     * @param sqlType 参数SQL类型根据 {@code java.sql.Types}
     * @param rch     用于分析{@link ResultSet}的{@link RowCallbackHandler}
     */
    public ResultSetSupportingSqlParameter(String name, int sqlType, RowCallbackHandler rch) {
        super(name, sqlType);
        this.rowCallbackHandler = rch;
    }

    /**
     * 创建新的ResultSetSupportingSqlParameter。
     *
     * @param name    输入和输出映射中使用的参数名称
     * @param sqlType 参数SQL类型根据 {@code java.sql.Types}
     * @param rm      用于分析{@link ResultSet}的{@link RowMapper}
     */
    public ResultSetSupportingSqlParameter(String name, int sqlType, RowMapper<?> rm) {
        super(name, sqlType);
        this.rowMapper = rm;
    }

    /**
     * 此参数是否支持ResultSet，即它是否包含ResultSetExtractor、RowCallbackHandler或RowMapper？
     */
    public boolean isResultSetSupported() {
        return (this.resultSetExtractor != null || this.rowCallbackHandler != null || this.rowMapper != null);
    }

    /**
     * 返回此参数持有的ResultSetExtractor（如果有）。
     */
    public ResultSetExtractor<?> getResultSetExtractor() {
        return this.resultSetExtractor;
    }

    /**
     * 返回此参数持有的RowCallbackHandler（如果有）。
     */
    public RowCallbackHandler getRowCallbackHandler() {
        return this.rowCallbackHandler;
    }

    /**
     * 返回此参数持有的行映射器（如果有）。
     */
    public RowMapper<?> getRowMapper() {
        return this.rowMapper;
    }

    /**
     * 此实现始终返回 {@code false}.
     */
    @Override
    public boolean isInputValueProvided() {
        return false;
    }
}
