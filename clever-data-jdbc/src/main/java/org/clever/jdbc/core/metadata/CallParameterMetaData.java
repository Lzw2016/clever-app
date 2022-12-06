package org.clever.jdbc.core.metadata;

import java.sql.DatabaseMetaData;

/**
 * 用于呼叫处理的特定参数的元数据持有者。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:06 <br/>
 *
 * @see GenericCallMetaDataProvider
 */
public class CallParameterMetaData {
    private final boolean function;
    private final String parameterName;
    private final int parameterType;
    private final int sqlType;
    private final String typeName;
    private final boolean nullable;

    /**
     * 构造函数采用除函数标记之外的所有属性。
     */
    @Deprecated
    public CallParameterMetaData(String columnName, int columnType, int sqlType, String typeName, boolean nullable) {
        this(false, columnName, columnType, sqlType, typeName, nullable);
    }

    /**
     * 构造函数采用所有属性，包括函数标记。
     */
    public CallParameterMetaData(boolean function, String columnName, int columnType, int sqlType, String typeName, boolean nullable) {
        this.function = function;
        this.parameterName = columnName;
        this.parameterType = columnType;
        this.sqlType = sqlType;
        this.typeName = typeName;
        this.nullable = nullable;
    }

    /**
     * 返回此参数是否在函数中声明
     */
    public boolean isFunction() {
        return this.function;
    }

    /**
     * 返回参数名称
     */
    public String getParameterName() {
        return this.parameterName;
    }

    /**
     * 返回参数类型
     */
    public int getParameterType() {
        return this.parameterType;
    }

    /**
     * 确定声明的参数是否符合我们的目的的“返回”参数：
     * 键入 {@link DatabaseMetaData#procedureColumnReturn} 或 {@link DatabaseMetaData#procedureColumnResult}，
     * 或者在函数的情况下，{@link DatabaseMetaData#functionReturn}。
     */
    public boolean isReturnParameter() {
        return (this.function ?
                this.parameterType == DatabaseMetaData.functionReturn :
                (this.parameterType == DatabaseMetaData.procedureColumnReturn || this.parameterType == DatabaseMetaData.procedureColumnResult)
        );
    }

    /**
     * 返回参数 SQL 类型
     */
    public int getSqlType() {
        return this.sqlType;
    }

    /**
     * 返回参数类型名称
     */
    public String getTypeName() {
        return this.typeName;
    }

    /**
     * 返回参数是否可为空
     */
    public boolean isNullable() {
        return this.nullable;
    }
}
