package org.clever.jdbc.core;

import java.sql.ResultSet;

/**
 * {@link SqlParameter} 的子类，用于表示输出参数。
 * 没有其他属性：instanceof 将用于检查此类类型。
 * <p>
 * 输出参数 - 与所有存储过程参数一样 - 必须具有名称。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:03 <br/>
 *
 * @see SqlReturnResultSet
 */
public class SqlOutParameter extends ResultSetSupportingSqlParameter {
    private SqlReturnType sqlReturnType;

    /**
     * 创建一个新的 SqlOutParameter。
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 参数 SQL 类型根据 {@code java.sql.Types}
     */
    public SqlOutParameter(String name, int sqlType) {
        super(name, sqlType);
    }

    /**
     * 创建一个新的 SqlOutParameter。
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 参数 SQL 类型根据 {@code java.sql.Types}
     * @param scale   小数点后的位数（对于 DECIMAL 和 NUMERIC 类型）
     */
    public SqlOutParameter(String name, int sqlType, int scale) {
        super(name, sqlType, scale);
    }

    /**
     * 创建一个新的 SqlOutParameter。
     *
     * @param name     参数的名称，用于输入和输出映射
     * @param sqlType  参数 SQL 类型根据 {@code java.sql.Types}
     * @param typeName 参数的类型名称（可选）
     */
    public SqlOutParameter(String name, int sqlType, String typeName) {
        super(name, sqlType, typeName);
    }

    /**
     * 创建一个新的 SqlOutParameter。
     *
     * @param name          参数的名称，用于输入和输出映射
     * @param sqlType       参数 SQL 类型根据 {@code java.sql.Types}
     * @param typeName      参数的类型名称（可选）
     * @param sqlReturnType 复杂类型的自定义值处理程序（可选）
     */
    public SqlOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
        super(name, sqlType, typeName);
        this.sqlReturnType = sqlReturnType;
    }

    /**
     * 创建一个新的 SqlOutParameter。
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 参数 SQL 类型根据 {@code java.sql.Types}
     * @param rse     用于解析 {@link ResultSet} 的 {@link ResultSetExtractor}
     */
    public SqlOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
        super(name, sqlType, rse);
    }

    /**
     * 创建一个新的 SqlOutParameter。
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 参数 SQL 类型根据 {@code java.sql.Types}
     * @param rch     用于解析 {@link ResultSet} 的 {@link RowCallbackHandler}
     */
    public SqlOutParameter(String name, int sqlType, RowCallbackHandler rch) {
        super(name, sqlType, rch);
    }

    /**
     * 创建新的SqlOutParameter。
     *
     * @param name    输入和输出映射中使用的参数名称
     * @param sqlType 参数SQL类型根据 {@code java.sql.Types}
     * @param rm      用于解析 {@link ResultSet} 的 {@link RowMapper}
     */
    public SqlOutParameter(String name, int sqlType, RowMapper<?> rm) {
        super(name, sqlType, rm);
    }

    /**
     * 返回自定义返回类型（如果有）。
     */
    public SqlReturnType getSqlReturnType() {
        return this.sqlReturnType;
    }

    /**
     * Return此参数是否包含自定义返回类型。
     */
    public boolean isReturnTypeSupported() {
        return (this.sqlReturnType != null);
    }
}
