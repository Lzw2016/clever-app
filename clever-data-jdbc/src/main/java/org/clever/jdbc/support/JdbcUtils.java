package org.clever.jdbc.support;

import org.clever.jdbc.CannotGetJdbcConnectionException;
import org.clever.jdbc.datasource.DataSourceUtils;
import org.clever.util.NumberUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用JDBC的通用实用程序方法。主要用于框架内的内部使用，也可用于自定义JDBC访问代码。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:18 <br/>
 */
public abstract class JdbcUtils {
    /**
     * 指示未知（或未指定）SQL类型的常量。
     *
     * @see Types
     */
    public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;
    private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);
    private static final Map<Integer, String> typeNames = new HashMap<>();

    static {
        try {
            for (Field field : Types.class.getFields()) {
                typeNames.put((Integer) field.get(null), field.getName());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to resolve JDBC Types constants", ex);
        }
    }

    /**
     * 关闭给定的JDBC连接并忽略任何抛出的异常。
     * 这对于手动JDBC代码中的典型finally块很有用。
     *
     * @param con 要关闭的JDBC连接 (可能是 {@code null})
     */
    public static void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                logger.debug("Could not close JDBC Connection", ex);
            } catch (Throwable ex) {
                // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                logger.debug("Unexpected exception on closing JDBC Connection", ex);
            }
        }
    }

    /**
     * 关闭给定的JDBC语句并忽略任何抛出的异常。
     * 这对于手动JDBC代码中的典型finally块很有用。
     *
     * @param stmt 要关闭的JDBC语句 (可能是 {@code null})
     */
    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                logger.trace("Could not close JDBC Statement", ex);
            } catch (Throwable ex) {
                // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                logger.trace("Unexpected exception on closing JDBC Statement", ex);
            }
        }
    }

    /**
     * 关闭给定的JDBC结果集并忽略任何抛出的异常。
     * 这对于手动JDBC代码中的典型finally块很有用。
     *
     * @param rs 要关闭的JDBC结果集 (可能是 {@code null})
     */
    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                logger.trace("Could not close JDBC ResultSet", ex);
            } catch (Throwable ex) {
                // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                logger.trace("Unexpected exception on closing JDBC ResultSet", ex);
            }
        }
    }

    /**
     * 使用指定的值类型从ResultSet检索JDBC列值。
     * <p>使用特定类型的ResultSet访问器方法，对于未知类型，返回到{@link #getResultSetValue(ResultSet, int)}。
     * 请注意，如果类型未知，则返回值可能无法分配给指定的必需类型。调用代码需要适当处理这种情况，例如引发相应的异常。
     *
     * @param rs           是保存数据的结果集
     * @param index        是列索引
     * @param requiredType 所需的值类型 (可能是 {@code null})
     * @return value对象（可能不是指定的必需类型，需要进一步的转换步骤）
     * @throws SQLException 如果由JDBC API引发
     * @see #getResultSetValue(ResultSet, int)
     */
    public static Object getResultSetValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {
        if (requiredType == null) {
            return getResultSetValue(rs, index);
        }
        Object value;
        // Explicitly extract typed value, as far as possible.
        if (String.class == requiredType) {
            return rs.getString(index);
        } else if (boolean.class == requiredType || Boolean.class == requiredType) {
            value = rs.getBoolean(index);
        } else if (byte.class == requiredType || Byte.class == requiredType) {
            value = rs.getByte(index);
        } else if (short.class == requiredType || Short.class == requiredType) {
            value = rs.getShort(index);
        } else if (int.class == requiredType || Integer.class == requiredType) {
            value = rs.getInt(index);
        } else if (long.class == requiredType || Long.class == requiredType) {
            value = rs.getLong(index);
        } else if (float.class == requiredType || Float.class == requiredType) {
            value = rs.getFloat(index);
        } else if (double.class == requiredType
                || Double.class == requiredType
                || Number.class == requiredType) {
            value = rs.getDouble(index);
        } else if (BigDecimal.class == requiredType) {
            return rs.getBigDecimal(index);
        } else if (Date.class == requiredType) {
            return rs.getDate(index);
        } else if (Time.class == requiredType) {
            return rs.getTime(index);
        } else if (Timestamp.class == requiredType || java.util.Date.class == requiredType) {
            return rs.getTimestamp(index);
        } else if (byte[].class == requiredType) {
            return rs.getBytes(index);
        } else if (Blob.class == requiredType) {
            return rs.getBlob(index);
        } else if (Clob.class == requiredType) {
            return rs.getClob(index);
        } else if (requiredType.isEnum()) {
            // Enums can either be represented through a String or an enum index value:
            // leave enum type conversion up to the caller (e.g. a ConversionService)
            // but make sure that we return nothing other than a String or an Integer.
            Object obj = rs.getObject(index);
            if (obj instanceof String) {
                return obj;
            } else if (obj instanceof Number) {
                // Defensively convert any Number to an Integer (as needed by our
                // ConversionService's IntegerToEnumConverterFactory) for use as index
                return NumberUtils.convertNumberToTargetClass((Number) obj, Integer.class);
            } else {
                // e.g. on Postgres: getObject returns a PGObject but we need a String
                return rs.getString(index);
            }
        } else {
            // Some unknown type desired -> rely on getObject.
            try {
                return rs.getObject(index, requiredType);
            } catch (AbstractMethodError err) {
                logger.debug("JDBC driver does not implement JDBC 4.1 'getObject(int, Class)' method", err);
            } catch (SQLFeatureNotSupportedException ex) {
                logger.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
            } catch (SQLException ex) {
                logger.debug("JDBC driver has limited support for JDBC 4.1 'getObject(int, Class)' method", ex);
            }
            // Corresponding SQL types for JSR-310 / Joda-Time types, left up
            // to the caller to convert them (e.g. through a ConversionService).
            String typeName = requiredType.getSimpleName();
            // noinspection IfCanBeSwitch
            if ("LocalDate".equals(typeName)) {
                return rs.getDate(index);
            } else if ("LocalTime".equals(typeName)) {
                return rs.getTime(index);
            } else if ("LocalDateTime".equals(typeName)) {
                return rs.getTimestamp(index);
            }
            // Fall back to getObject without type specification, again
            // left up to the caller to convert the value if necessary.
            return getResultSetValue(rs, index);
        }
        // Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
        return (rs.wasNull() ? null : value);
    }

    /**
     * 使用最合适的值类型从ResultSet检索JDBC列值。
     * 返回的值应该是分离的值对象，与活动的ResultSet没有任何联系：
     * 特别是，它不应该是Blob或Clob对象，而应该分别是字节数组或字符串表示。
     * <p>使用{@code getObject(index)}方法，
     * 但还包括其他“hacks”来绕过Oracle 10g返回非标准对象作为其时间戳数据类型和{@code java.sql.Date}列省略时间部分：
     * 这些列将显式提取为标准{@code java.sql.Timestamp}对象。
     *
     * @param rs    是保存数据的结果集
     * @param index 是列索引
     * @return value对象
     * @throws SQLException 如果由JDBC API引发
     * @see Blob
     * @see Clob
     * @see Timestamp
     */
    public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
        Object obj = rs.getObject(index);
        String className = null;
        if (obj != null) {
            className = obj.getClass().getName();
        }
        if (obj instanceof Blob) {
            Blob blob = (Blob) obj;
            obj = blob.getBytes(1, (int) blob.length());
        } else if (obj instanceof Clob) {
            Clob clob = (Clob) obj;
            obj = clob.getSubString(1, (int) clob.length());
        } else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
            obj = rs.getTimestamp(index);
        } else if (className != null && className.startsWith("oracle.sql.DATE")) {
            String metaDataClassName = rs.getMetaData().getColumnClassName(index);
            if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
                obj = rs.getTimestamp(index);
            } else {
                obj = rs.getDate(index);
            }
        } else if (obj instanceof Date) {
            if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
                obj = rs.getTimestamp(index);
            }
        }
        return obj;
    }

    /**
     * 通过给定的DatabaseMetaDataCallback提取数据库元数据。
     * <p>此方法将打开与数据库的连接并检索其元数据。
     * 由于此方法是在为数据源配置异常转换功能之前调用的，因此此方法不能依赖于SQLException转换本身。
     * <p>任何异常都将包装在MetaDataAccessException中。这是一个已检查的异常，任何调用代码都应该捕获并处理该异常。
     * 您可以只记录错误并希望一切顺利，但当您再次尝试访问数据库时，可能会再次出现更严重的错误。
     *
     * @param dataSource 要提取元数据的数据源
     * @param action     将执行实际工作的回调
     * @return 对象，该对象包含由DatabaseMetaDataCallback的{@code processMetaData}方法返回的提取信息
     * @throws MetaDataAccessException 如果元数据访问失败
     * @see DatabaseMetaData
     */
    public static <T> T extractDatabaseMetaData(DataSource dataSource, DatabaseMetaDataCallback<T> action) throws MetaDataAccessException {
        Connection con = null;
        try {
            con = DataSourceUtils.getConnection(dataSource);
            DatabaseMetaData metaData;
            try {
                metaData = con.getMetaData();
            } catch (SQLException ex) {
                if (DataSourceUtils.isConnectionTransactional(con, dataSource)) {
                    // Probably a closed thread-bound Connection - retry against fresh Connection
                    DataSourceUtils.releaseConnection(con, dataSource);
                    con = null;
                    logger.debug(
                            "Failed to obtain DatabaseMetaData from transactional Connection - " +
                                    "retrying against fresh Connection",
                            ex
                    );
                    con = dataSource.getConnection();
                    metaData = con.getMetaData();
                } else {
                    throw ex;
                }
            }
            if (metaData == null) {
                // should only happen in test environments
                throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + con + "] was null");
            }
            return action.processMetaData(metaData);
        } catch (CannotGetJdbcConnectionException ex) {
            throw new MetaDataAccessException("Could not get Connection for extracting meta-data", ex);
        } catch (SQLException ex) {
            throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
        } catch (AbstractMethodError err) {
            throw new MetaDataAccessException(
                    "JDBC DatabaseMetaData method not implemented by JDBC driver - upgrade your driver", err
            );
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /**
     * 返回给定的JDBC驱动程序是否支持JDBC 2.0批量更新。
     * <p>通常在执行给定的一组语句之前调用：决定该组SQL语句是应该通过JDBC 2.0批处理机制执行，还是简单地以传统的逐个方式执行。
     * <p>如果“supportsBatchUpdates”方法引发异常并在这种情况下仅返回false，则记录警告。
     *
     * @param con 要检查的连接
     * @return 是否支持JDBC 2.0批量更新
     * @see DatabaseMetaData#supportsBatchUpdates()
     */
    public static boolean supportsBatchUpdates(Connection con) {
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            if (dbmd != null) {
                if (dbmd.supportsBatchUpdates()) {
                    logger.debug("JDBC driver supports batch updates");
                    return true;
                } else {
                    logger.debug("JDBC driver does not support batch updates");
                }
            }
        } catch (SQLException ex) {
            logger.debug("JDBC driver 'supportsBatchUpdates' method threw exception", ex);
        }
        return false;
    }

    /**
     * 为正在使用的目标数据库提取一个通用名称，即使不同的drivers/platforms在运行时提供不同的名称。
     *
     * @param source 数据库元数据中提供的名称
     * @return 要使用的通用名称 (例如 "DB2" or "Sybase")
     */
    public static String commonDatabaseName(String source) {
        String name = source;
        if (source != null && source.startsWith("DB2")) {
            name = "DB2";
        } else if ("MariaDB".equals(source)) {
            name = "MySQL";
        } else if ("Sybase SQL Server".equals(source)
                || "Adaptive Server Enterprise".equals(source)
                || "ASE".equals(source)
                || "sql server".equalsIgnoreCase(source)) {
            name = "Sybase";
        }
        return name;
    }

    /**
     * 检查给定的SQL类型是否为数字。
     *
     * @param sqlType 要检查的SQL类型
     * @return 类型是否为数字
     */
    public static boolean isNumeric(int sqlType) {
        return (Types.BIT == sqlType
                || Types.BIGINT == sqlType
                || Types.DECIMAL == sqlType
                || Types.DOUBLE == sqlType
                || Types.FLOAT == sqlType
                || Types.INTEGER == sqlType
                || Types.NUMERIC == sqlType
                || Types.REAL == sqlType
                || Types.SMALLINT == sqlType
                || Types.TINYINT == sqlType);
    }

    /**
     * 如果可能，请解析给定SQL类型的标准类型名称。
     *
     * @param sqlType 要解析的SQL类型
     * @return 中对应的常量名称 {@link Types} (例如 "VARCHAR"/"NUMERIC"), 或 {@code null} 如果无法解决
     */
    public static String resolveTypeName(int sqlType) {
        return typeNames.get(sqlType);
    }

    /**
     * 确定要使用的列名。列名是基于使用ResultSetMetaData的查找确定的。
     * <p>此方法实现考虑了JDBC 4.0规范中最近的澄清：
     * <p><i>columnLabel—使用SQL AS子句指定的列的标签。如果未指定SQL AS子句，则标签是列的名称</i>.
     *
     * @param resultSetMetaData 要使用的当前元数据
     * @param columnIndex       用于查找的列的索引
     * @return 要使用的列名
     * @throws SQLException 查找失败时
     */
    public static String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
        String name = resultSetMetaData.getColumnLabel(columnIndex);
        if (!StringUtils.hasLength(name)) {
            name = resultSetMetaData.getColumnName(columnIndex);
        }
        return name;
    }

    /**
     * 使用“camel case”将带下划线的列名转换为相应的属性名。
     * “customer_number”这样的名称将与“customerNumber”属性名称匹配。
     *
     * @param name 要转换的列名
     * @return 使用“camel case”的名称
     */
    public static String convertUnderscoreNameToPropertyName(String name) {
        StringBuilder result = new StringBuilder();
        boolean nextIsUpper = false;
        if (name != null && name.length() > 0) {
            if (name.length() > 1 && name.charAt(1) == '_') {
                result.append(Character.toUpperCase(name.charAt(0)));
            } else {
                result.append(Character.toLowerCase(name.charAt(0)));
            }
            for (int i = 1; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == '_') {
                    nextIsUpper = true;
                } else {
                    if (nextIsUpper) {
                        result.append(Character.toUpperCase(c));
                        nextIsUpper = false;
                    } else {
                        result.append(Character.toLowerCase(c));
                    }
                }
            }
        }
        return result.toString();
    }
}
