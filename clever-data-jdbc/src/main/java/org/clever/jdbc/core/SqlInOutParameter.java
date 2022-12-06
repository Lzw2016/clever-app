package org.clever.jdbc.core;

import java.sql.ResultSet;

/**
 * {@link SqlOutParameter} 的子类表示 INOUT 参数。<br/>
 * 与标准 SqlOutParameter 相比，将为 SqlParameter 的 {@link #isInputValueProvided} 测试返回 {@code true}。
 *
 * <p>输出参数 - 像所有存储过程参数一样 - 必须有名称
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:09 <br/>
 */
public class SqlInOutParameter extends SqlOutParameter {
    /**
     * 创建一个新的 SqlInOutParameter
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
     */
    public SqlInOutParameter(String name, int sqlType) {
        super(name, sqlType);
    }

    /**
     * 创建一个新的 SqlInOutParameter
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
     * @param scale   小数点后的位数（对于 DECIMAL 和 NUMERIC 类型）
     */
    public SqlInOutParameter(String name, int sqlType, int scale) {
        super(name, sqlType, scale);
    }

    /**
     * 创建一个新的 SqlInOutParameter
     *
     * @param name     参数的名称，用于输入和输出映射
     * @param sqlType  根据 {@code java.sql.Types} 的参数 SQL 类型
     * @param typeName 参数的类型名称（可选）
     */
    public SqlInOutParameter(String name, int sqlType, String typeName) {
        super(name, sqlType, typeName);
    }

    /**
     * 创建一个新的 SqlInOutParameter
     *
     * @param name          参数的名称，用于输入和输出映射
     * @param sqlType       根据 {@code java.sql.Types} 的参数 SQL 类型
     * @param typeName      参数的类型名称（可选）
     * @param sqlReturnType 复杂类型的自定义值处理程序（可选）
     */
    public SqlInOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
        super(name, sqlType, typeName, sqlReturnType);
    }

    /**
     * 创建一个新的 SqlInOutParameter
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
     * @param rse     {@link ResultSetExtractor} 用于解析 {@link ResultSet}
     */
    public SqlInOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
        super(name, sqlType, rse);
    }

    /**
     * 创建一个新的 SqlInOutParameter
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
     * @param rch     {@link RowCallbackHandler} 用于解析 {@link ResultSet}
     */
    public SqlInOutParameter(String name, int sqlType, RowCallbackHandler rch) {
        super(name, sqlType, rch);
    }

    /**
     * 创建一个新的 SqlInOutParameter
     *
     * @param name    参数的名称，用于输入和输出映射
     * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
     * @param rm      {@link RowMapper} 用于解析 {@link ResultSet}
     */
    public SqlInOutParameter(String name, int sqlType, RowMapper<?> rm) {
        super(name, sqlType, rm);
    }

    /**
     * 此实现始终返回 {@code true}。
     */
    @Override
    public boolean isInputValueProvided() {
        return true;
    }
}
