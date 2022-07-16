package org.clever.jdbc.core;

import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 对象来表示SQL参数定义。
 *
 * <p>参数可以是匿名的，在这种情况下，“name”为null。但是，所有参数都必须根据 {@link java.sql.Types}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:38 <br/>
 *
 * @see java.sql.Types
 */
public class SqlParameter {
    // 参数的名称（如果有）
    private String name;
    // 来自的SQL类型常量 {@code java.sql.Types}
    private final int sqlType;
    // 用于用户命名的类型，例如：STRUCT、DISTINCT、JAVA_OBJECT、命名数组类型
    private String typeName;
    // NUMERIC 或 DECIMAL类型（如有）的scale
    private Integer scale;

    /**
     * 创建一个新的匿名SqlParameter，提供SQL类型。
     *
     * @param sqlType 参数的SQL类型根据 {@code java.sql.Types}
     */
    public SqlParameter(int sqlType) {
        this.sqlType = sqlType;
    }

    /**
     * 创建一个新的匿名SqlParameter，提供SQL类型。
     *
     * @param sqlType  参数的SQL类型根据 {@code java.sql.Types}
     * @param typeName 参数的类型名称（可选）
     */
    public SqlParameter(int sqlType, String typeName) {
        this.sqlType = sqlType;
        this.typeName = typeName;
    }

    /**
     * 创建一个新的匿名SqlParameter，提供SQL类型。
     *
     * @param sqlType 参数的SQL类型根据 {@code java.sql.Types}
     * @param scale   小数点后的位数(对于DECIMAL和NUMERIC类型)
     */
    public SqlParameter(int sqlType, int scale) {
        this.sqlType = sqlType;
        this.scale = scale;
    }

    /**
     * 创建一个新的SqlParameter，提供名称和SQL类型。
     *
     * @param name    输入和输出Map中使用的参数名称
     * @param sqlType 参数的SQL类型根据 {@code java.sql.Types}
     */
    public SqlParameter(String name, int sqlType) {
        this.name = name;
        this.sqlType = sqlType;
    }

    /**
     * 创建一个新的SqlParameter，提供名称和SQL类型。
     *
     * @param name     输入和输出Map中使用的参数名称
     * @param sqlType  参数的SQL类型根据 {@code java.sql.Types}
     * @param typeName 参数的类型名称（可选）
     */
    public SqlParameter(String name, int sqlType, String typeName) {
        this.name = name;
        this.sqlType = sqlType;
        this.typeName = typeName;
    }

    /**
     * 创建一个新的SqlParameter，提供名称和SQL类型。
     *
     * @param name    输入和输出Map中使用的参数名称
     * @param sqlType 参数的SQL类型根据 {@code java.sql.Types}
     * @param scale   小数点后的位数 (对于DECIMAL和NUMERIC类型)
     */
    public SqlParameter(String name, int sqlType, int scale) {
        this.name = name;
        this.sqlType = sqlType;
        this.scale = scale;
    }

    /**
     * 复制构造函数。
     *
     * @param otherParam 要从中复制的SqlParameter对象
     */
    public SqlParameter(SqlParameter otherParam) {
        Assert.notNull(otherParam, "SqlParameter object must not be null");
        this.name = otherParam.name;
        this.sqlType = otherParam.sqlType;
        this.typeName = otherParam.typeName;
        this.scale = otherParam.scale;
    }

    /**
     * 返回参数的名称，如果匿名，则返回null。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 返回参数的SQL类型。
     */
    public int getSqlType() {
        return this.sqlType;
    }

    /**
     * 返回参数的类型名（如果有）。
     */
    public String getTypeName() {
        return this.typeName;
    }

    /**
     * 返回参数的比例（如果有）。
     */
    public Integer getScale() {
        return this.scale;
    }

    /**
     * 返回此参数是否包含应在执行前设置的输入值，即使它们是 {@code null}.
     * <p>此实现始终返回 {@code true}.
     */
    public boolean isInputValueProvided() {
        return true;
    }

    /**
     * Return此参数是否为在的结果处理期间使用的隐式返回参数 {@code CallableStatement.getMoreResults/getUpdateCount}.
     * <p>此实现始终返回 {@code false}.
     */
    public boolean isResultsParameter() {
        return false;
    }

    /**
     * 转换JDBC类型列表，如中所定义 {@code java.sql.Types},此包中使用的SqlParameter对象列表。
     */
    public static List<SqlParameter> sqlTypesToAnonymousParameterList(int... types) {
        if (types == null) {
            return new ArrayList<>();
        }
        List<SqlParameter> result = new ArrayList<>(types.length);
        for (int type : types) {
            result.add(new SqlParameter(type));
        }
        return result;
    }
}
