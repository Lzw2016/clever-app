package org.clever.jdbc.core.namedparam;

import org.clever.jdbc.support.JdbcUtils;

/**
 * 为对象定义通用功能的接口，这些对象可以为命名的SQL参数提供参数值，用作{@link NamedParameterJdbcTemplate}操作的参数。
 *
 * <p>除了参数值外，该接口还允许指定SQL类型。通过指定参数的名称来标识所有参数值和类型。
 *
 * <p>旨在用一致的接口包装各种实现，如映射或JavaBean。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:49 <br/>
 *
 * @see NamedParameterJdbcOperations
 * @see NamedParameterJdbcTemplate
 * @see MapSqlParameterSource
 * @see BeanPropertySqlParameterSource
 */
public interface SqlParameterSource {
    /**
     * 指示未知（或未指定）SQL类型的常量。当未知特定SQL类型时，从{@code getType}返回。
     *
     * @see #getSqlType
     * @see java.sql.Types
     */
    int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;

    /**
     * 确定指定的命名参数是否有值。
     *
     * @param paramName 参数的名称
     * @return 是否定义了值
     */
    boolean hasValue(String paramName);

    /**
     * 返回请求的命名参数的参数值。
     *
     * @param paramName 参数的名称
     * @return 指定参数的值
     * @throws IllegalArgumentException 如果请求的参数没有值
     */
    Object getValue(String paramName) throws IllegalArgumentException;

    /**
     * 确定指定命名参数的SQL类型。
     *
     * @param paramName 参数的名称
     * @return 指定参数的SQL类型，或者{@code TYPE_UNKNOWN}（如果未知）
     * @see #TYPE_UNKNOWN
     */
    default int getSqlType(String paramName) {
        return TYPE_UNKNOWN;
    }

    /**
     * 确定指定命名参数的类型名称。
     *
     * @param paramName 参数的名称
     * @return 指定参数的类型名，如果未知，则为null
     */
    default String getTypeName(String paramName) {
        return null;
    }

    /**
     * 如果可能，枚举所有可用的参数名称。
     * <p>这是一个可选操作，主要用于{@code SimpleJdbcInsert} 和 {@code SimpleJdbcCall}。
     *
     * @return 参数名数组，如果不可确定，则为null
     * @see SqlParameterSourceUtils#extractCaseInsensitiveParameterNames
     */
    default String[] getParameterNames() {
        return null;
    }
}
