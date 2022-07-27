package org.clever.jdbc.core;

/**
 * 表示 SQL 参数值的对象，包括参数元数据，例如 SQL 类型和数值的比例。
 *
 * <p>
 * 设计用于采用参数值数组的 {@link JdbcTemplate} 操作：
 * 每个此类参数值都可以是 {@code SqlParameterValue}，指示 SQL 类型（以及可选的比例），而不是让模板猜测默认类型。
 * 请注意，这仅适用于具有“普通”参数数组的操作，不适用于具有显式类型数组的重载变体。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:46 <br/>
 *
 * @see java.sql.Types
 * @see JdbcTemplate#query(String, ResultSetExtractor, Object[])
 * @see JdbcTemplate#query(String, RowCallbackHandler, Object[])
 * @see JdbcTemplate#query(String, RowMapper, Object[])
 * @see JdbcTemplate#update(String, Object[])
 */
public class SqlParameterValue extends SqlParameter {
    private final Object value;

    /**
     * 创建一个新的 SqlParameterValue，提供 SQL 类型。
     *
     * @param sqlType 参数的 SQL 类型根据 {@code java.sql.Types}
     * @param value   价值对象
     */
    public SqlParameterValue(int sqlType, Object value) {
        super(sqlType);
        this.value = value;
    }

    /**
     * 创建一个新的 SqlParameterValue，提供 SQL 类型。
     *
     * @param sqlType  参数的 SQL 类型根据 {@code java.sql.Types}
     * @param typeName 参数的类型名称（可选）
     * @param value    价值对象
     */
    public SqlParameterValue(int sqlType, String typeName, Object value) {
        super(sqlType, typeName);
        this.value = value;
    }

    /**
     * 创建一个新的 SqlParameterValue，提供 SQL 类型。
     *
     * @param sqlType 参数的 SQL 类型根据 {@code java.sql.Types}
     * @param scale   小数点后的位数（对于 DECIMAL 和 NUMERIC 类型）
     * @param value   价值对象
     */
    public SqlParameterValue(int sqlType, int scale, Object value) {
        super(sqlType, scale);
        this.value = value;
    }

    /**
     * 根据给定的 SqlParameter 声明创建一个新的 SqlParameterValue。
     *
     * @param declaredParam 声明的 SqlParameter 来定义一个值
     * @param value         价值对象
     */
    public SqlParameterValue(SqlParameter declaredParam, Object value) {
        super(declaredParam);
        this.value = value;
    }

    /**
     * 返回此参数值所持有的值对象。
     */
    public Object getValue() {
        return this.value;
    }
}
