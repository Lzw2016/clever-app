package org.clever.jdbc.core;

import org.clever.jdbc.support.SqlValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

/**
 * PreparedStatementSetterCreator和CallableStatementCreator实现的实用方法，提供复杂的参数管理（包括对LOB值的支持）。
 *
 * <p>由PreparedStatementCreatorFactory和CallableStatementCreatorFactory使用，但也可直接用于自定义setter/creator实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:46 <br/>
 *
 * @see PreparedStatementSetter
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see SqlParameter
 * @see SqlTypeValue
 */
public abstract class StatementCreatorUtils {
    /**
     * 指示完全忽略{@link java.sql.ParameterMetaData#getParameterType}的系统属性，
     * 即从不尝试为setNull调用检索{@link PreparedStatement#getParameterMetaData()}。
     * 指示完全忽略的系统属性，即从不尝试检索{@link StatementCreatorUtils#setNull}调用。
     * <p>默认值为 "false",首先尝试{@code getParameterType}调用，
     * 然后根据常见数据库的已知行为返回{@link PreparedStatement#setNull} / {@link PreparedStatement#setObject}调用。
     * <p>如果您在运行时遇到错误行为，例如{@code getParameterType}引发异常（JBoss as 7报告）或性能问题（PostgreSQL报告）时出现连接池问题，
     * 请考虑将此标志切换为“true”。
     */
    public static final String IGNORE_GETPARAMETERTYPE_PROPERTY_NAME = "clever.jdbc.getParameterType.ignore";
    static boolean shouldIgnoreGetParameterType = false; // SpringProperties.getFlag(IGNORE_GETPARAMETERTYPE_PROPERTY_NAME);
    private static final Logger logger = LoggerFactory.getLogger(StatementCreatorUtils.class);
    private static final Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<>(32);

    static {
        javaTypeToSqlTypeMap.put(boolean.class, Types.BOOLEAN);
        javaTypeToSqlTypeMap.put(Boolean.class, Types.BOOLEAN);
        javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
        javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
        javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
        javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
        javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
        javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
        javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
        javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
        javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
        javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
        javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
        javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
        javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
        javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
        javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
        javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
    }

    /**
     * 从给定的Java类型派生默认SQL类型。
     *
     * @param javaType 要翻译的Java类型
     * @return 对应的SQL类型，或 {@link SqlTypeValue#TYPE_UNKNOWN} 如果未找到
     */
    public static int javaTypeToSqlParameterType(Class<?> javaType) {
        if (javaType == null) {
            return SqlTypeValue.TYPE_UNKNOWN;
        }
        Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
        if (sqlType != null) {
            return sqlType;
        }
        if (Number.class.isAssignableFrom(javaType)) {
            return Types.NUMERIC;
        }
        if (isStringValue(javaType)) {
            return Types.VARCHAR;
        }
        if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
            return Types.TIMESTAMP;
        }
        return SqlTypeValue.TYPE_UNKNOWN;
    }

    /**
     * 设置参数的值。使用的方法基于参数的SQL类型，我们可以处理复杂类型，如数组和LOB。
     *
     * @param ps         准备好的语句或可赎回的语句
     * @param paramIndex 我们正在设置的参数的索引
     * @param param      声明的参数包括类型
     * @param inValue    要设置的值
     * @throws SQLException 如果由PreparedStatement方法引发
     */
    public static void setParameterValue(PreparedStatement ps,
                                         int paramIndex,
                                         SqlParameter param,
                                         Object inValue) throws SQLException {
        setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param.getScale(), inValue);
    }

    /**
     * 设置参数的值。使用的方法基于参数的SQL类型，我们可以处理复杂类型，如数组和LOB。
     *
     * @param ps         准备好的语句或可赎回的语句
     * @param paramIndex 我们正在设置的参数的索引
     * @param sqlType    参数的SQL类型
     * @param inValue    要设置的值（普通值或SqlTypeValue）
     * @throws SQLException 如果由PreparedStatement方法引发
     * @see SqlTypeValue
     */
    public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType, Object inValue) throws SQLException {
        setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
    }

    /**
     * 设置参数的值。使用的方法基于参数的SQL类型，我们可以处理复杂类型，如数组和LOB。
     *
     * @param ps         准备好的语句或可赎回的语句
     * @param paramIndex 我们正在设置的参数的索引
     * @param sqlType    参数的SQL类型
     * @param typeName   参数的类型（可选，仅用于SQL NULL和SqlTypeValue）
     * @param inValue    要设置的值（普通值或SqlTypeValue）
     * @throws SQLException 如果由PreparedStatement方法引发
     * @see SqlTypeValue
     */
    public static void setParameterValue(PreparedStatement ps,
                                         int paramIndex,
                                         int sqlType,
                                         String typeName,
                                         Object inValue) throws SQLException {
        setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
    }

    /**
     * 设置参数的值。使用的方法基于参数的SQL类型，我们可以处理复杂类型，如数组和LOB。
     *
     * @param ps         准备好的语句或可赎回的语句
     * @param paramIndex 我们正在设置的参数的索引
     * @param sqlType    参数的SQL类型
     * @param typeName   参数的类型（可选，仅用于SQL NULL和SqlTypeValue）
     * @param scale      小数点后的位数（对于DECIMAL 和 NUMERIC类型）
     * @param inValue    要设置的值（普通值或SqlTypeValue）
     * @throws SQLException 如果由PreparedStatement方法引发
     * @see SqlTypeValue
     */
    private static void setParameterValueInternal(PreparedStatement ps,
                                                  int paramIndex,
                                                  int sqlType,
                                                  String typeName,
                                                  Integer scale,
                                                  Object inValue) throws SQLException {
        String typeNameToUse = typeName;
        int sqlTypeToUse = sqlType;
        Object inValueToUse = inValue;
        // override type info?
        if (inValue instanceof SqlParameterValue) {
            SqlParameterValue parameterValue = (SqlParameterValue) inValue;
            if (logger.isDebugEnabled()) {
                logger.debug("Overriding type info with runtime info from SqlParameterValue: column index " + paramIndex
                        + ", SQL type " + parameterValue.getSqlType() + ", type name " + parameterValue.getTypeName()
                );
            }
            if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
                sqlTypeToUse = parameterValue.getSqlType();
            }
            if (parameterValue.getTypeName() != null) {
                typeNameToUse = parameterValue.getTypeName();
            }
            inValueToUse = parameterValue.getValue();
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Setting SQL statement parameter value: column index " + paramIndex
                    + ", parameter value [" + inValueToUse
                    + "], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null")
                    + "], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
        }
        if (inValueToUse == null) {
            setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
        } else {
            setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
        }
    }

    /**
     * 根据数据库特定的特性，将指定的PreparedStatement参数设置为null。
     */
    private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, String typeName) throws SQLException {
        if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER && typeName == null)) {
            boolean useSetObject = false;
            Integer sqlTypeToUse = null;
            if (!shouldIgnoreGetParameterType) {
                try {
                    sqlTypeToUse = ps.getParameterMetaData().getParameterType(paramIndex);
                } catch (SQLException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("JDBC getParameterType call failed - using fallback method instead: " + ex);
                    }
                }
            }
            if (sqlTypeToUse == null) {
                // Proceed with database-specific checks
                sqlTypeToUse = Types.NULL;
                DatabaseMetaData dbmd = ps.getConnection().getMetaData();
                String jdbcDriverName = dbmd.getDriverName();
                String databaseProductName = dbmd.getDatabaseProductName();
                if (databaseProductName.startsWith("Informix")
                        || (jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server"))) {
                    // "Microsoft SQL Server JDBC Driver 3.0" versus "Microsoft JDBC Driver 4.0 for SQL Server"
                    useSetObject = true;
                } else if (databaseProductName.startsWith("DB2")
                        || jdbcDriverName.startsWith("jConnect")
                        || jdbcDriverName.startsWith("SQLServer")
                        || jdbcDriverName.startsWith("Apache Derby")) {
                    sqlTypeToUse = Types.VARCHAR;
                }
            }
            if (useSetObject) {
                ps.setObject(paramIndex, null);
            } else {
                ps.setNull(paramIndex, sqlTypeToUse);
            }
        } else if (typeName != null) {
            ps.setNull(paramIndex, sqlType, typeName);
        } else {
            ps.setNull(paramIndex, sqlType);
        }
    }

    private static void setValue(PreparedStatement ps,
                                 int paramIndex,
                                 int sqlType,
                                 String typeName,
                                 Integer scale,
                                 Object inValue) throws SQLException {
        if (inValue instanceof SqlTypeValue) {
            ((SqlTypeValue) inValue).setTypeValue(ps, paramIndex, sqlType, typeName);
        } else if (inValue instanceof SqlValue) {
            ((SqlValue) inValue).setValue(ps, paramIndex);
        } else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR) {
            ps.setString(paramIndex, inValue.toString());
        } else if (sqlType == Types.NVARCHAR || sqlType == Types.LONGNVARCHAR) {
            ps.setNString(paramIndex, inValue.toString());
        } else if ((sqlType == Types.CLOB || sqlType == Types.NCLOB) && isStringValue(inValue.getClass())) {
            String strVal = inValue.toString();
            if (strVal.length() > 4000) {
                // Necessary for older Oracle drivers, in particular when running against an Oracle 10 database.
                // Should also work fine against other drivers/databases since it uses standard JDBC 4.0 API.
                if (sqlType == Types.NCLOB) {
                    ps.setNClob(paramIndex, new StringReader(strVal), strVal.length());
                } else {
                    ps.setClob(paramIndex, new StringReader(strVal), strVal.length());
                }
            } else {
                // Fallback: setString or setNString binding
                if (sqlType == Types.NCLOB) {
                    ps.setNString(paramIndex, strVal);
                } else {
                    ps.setString(paramIndex, strVal);
                }
            }
        } else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
            if (inValue instanceof BigDecimal) {
                ps.setBigDecimal(paramIndex, (BigDecimal) inValue);
            } else if (scale != null) {
                ps.setObject(paramIndex, inValue, sqlType, scale);
            } else {
                ps.setObject(paramIndex, inValue, sqlType);
            }
        } else if (sqlType == Types.BOOLEAN) {
            if (inValue instanceof Boolean) {
                ps.setBoolean(paramIndex, (Boolean) inValue);
            } else {
                ps.setObject(paramIndex, inValue, Types.BOOLEAN);
            }
        } else if (sqlType == Types.DATE) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Date) {
                    ps.setDate(paramIndex, (java.sql.Date) inValue);
                } else {
                    ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.DATE);
            }
        } else if (sqlType == Types.TIME) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Time) {
                    ps.setTime(paramIndex, (java.sql.Time) inValue);
                } else {
                    ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.TIME);
            }
        } else if (sqlType == Types.TIMESTAMP) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Timestamp) {
                    ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValue);
                } else {
                    ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
            }
        } else if (sqlType == SqlTypeValue.TYPE_UNKNOWN
                || (sqlType == Types.OTHER && "Oracle".equals(ps.getConnection().getMetaData().getDatabaseProductName()))) {
            if (isStringValue(inValue.getClass())) {
                ps.setString(paramIndex, inValue.toString());
            } else if (isDateValue(inValue.getClass())) {
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
            } else {
                // Fall back to generic setObject call without SQL type specified.
                ps.setObject(paramIndex, inValue);
            }
        } else {
            // Fall back to generic setObject call with SQL type specified.
            ps.setObject(paramIndex, inValue, sqlType);
        }
    }

    /**
     * 检查给定值是否可以视为字符串值。
     */
    private static boolean isStringValue(Class<?> inValueType) {
        // Consider any CharSequence (including StringBuffer and StringBuilder) as a String.
        return (CharSequence.class.isAssignableFrom(inValueType) || StringWriter.class.isAssignableFrom(inValueType));
    }

    /**
     * 检查给定值是否为 {@code java.util.Date} (但不是JDBC特定的子类之一).
     */
    private static boolean isDateValue(Class<?> inValueType) {
        return (java.util.Date.class.isAssignableFrom(inValueType)
                && !(java.sql.Date.class.isAssignableFrom(inValueType)
                || java.sql.Time.class.isAssignableFrom(inValueType)
                || java.sql.Timestamp.class.isAssignableFrom(inValueType))
        );
    }

    /**
     * 清理传递给execute方法的参数值所持有的所有资源。例如，这对于关闭LOB值很重要。
     *
     * @param paramValues 提供的参数值。可能是 {@code null}.
     * @see DisposableSqlTypeValue#cleanup()
     */
    public static void cleanupParameters(Object... paramValues) {
        if (paramValues != null) {
            cleanupParameters(Arrays.asList(paramValues));
        }
    }

    /**
     * 清理传递给execute方法的参数值所持有的所有资源。例如，这对于关闭LOB值很重要。
     *
     * @param paramValues 提供的参数值。可能是 {@code null}.
     * @see DisposableSqlTypeValue#cleanup()
     */
    public static void cleanupParameters(Collection<?> paramValues) {
        if (paramValues != null) {
            for (Object inValue : paramValues) {
                // Unwrap SqlParameterValue first...
                if (inValue instanceof SqlParameterValue) {
                    inValue = ((SqlParameterValue) inValue).getValue();
                }
                // Check for disposable value types
                if (inValue instanceof SqlValue) {
                    ((SqlValue) inValue).cleanup();
                } else if (inValue instanceof DisposableSqlTypeValue) {
                    ((DisposableSqlTypeValue) inValue).cleanup();
                }
            }
        }
    }
}
