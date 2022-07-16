package org.clever.jdbc.core;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.support.DefaultConversionService;
import org.clever.dao.TypeMismatchDataAccessException;
import org.clever.jdbc.IncorrectResultSetColumnCountException;
import org.clever.jdbc.support.JdbcUtils;
import org.clever.util.ClassUtils;
import org.clever.util.NumberUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * {@link RowMapper}实现，将每行的单个列转换为单个结果值。期望在仅包含单个列的{@code java.sql.ResultSet}上操作。
 *
 * <p>可以指定每行结果值的类型。将从{@code ResultSet}提取单个列的值，并将其转换为指定的目标类型。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:06 <br/>
 *
 * @param <T> the result type
 * @see JdbcTemplate#queryForList(String, Class)
 * @see JdbcTemplate#queryForObject(String, Class)
 */
public class SingleColumnRowMapper<T> implements RowMapper<T> {
    private Class<?> requiredType;
    private ConversionService conversionService = DefaultConversionService.getSharedInstance();

    /**
     * 创建新的{@code SingleColumnRowMapper}
     *
     * @see #setRequiredType
     */
    public SingleColumnRowMapper() {
    }

    /**
     * 创建新的 {@code SingleColumnRowMapper}.
     *
     * @param requiredType 每个结果对象预期匹配的类型
     */
    public SingleColumnRowMapper(Class<T> requiredType) {
        setRequiredType(requiredType);
    }

    /**
     * 设置每个结果对象预期匹配的类型。
     * <p>如果未指定，则列值将显示为JDBC驱动程序返回的值。
     */
    public void setRequiredType(Class<T> requiredType) {
        this.requiredType = ClassUtils.resolvePrimitiveIfNecessary(requiredType);
    }

    /**
     * 设置 {@link ConversionService} 用于转换提取的值。
     * <p>默认值为 {@link DefaultConversionService}.
     *
     * @see DefaultConversionService#getSharedInstance
     */
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * 提取当前行中单个列的值。
     * <p>验证是否只选择了一列，然后根据需要委托给{@code getColumnValue()}和{@code convertValueToRequiredType}。
     *
     * @see java.sql.ResultSetMetaData#getColumnCount()
     * @see #getColumnValue(java.sql.ResultSet, int, Class)
     * @see #convertValueToRequiredType(Object, Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        // Validate column count.
        ResultSetMetaData rsmd = rs.getMetaData();
        int nrOfColumns = rsmd.getColumnCount();
        if (nrOfColumns != 1) {
            throw new IncorrectResultSetColumnCountException(1, nrOfColumns);
        }
        // Extract column value from JDBC ResultSet.
        Object result = getColumnValue(rs, 1, this.requiredType);
        if (result != null && this.requiredType != null && !this.requiredType.isInstance(result)) {
            // Extracted value does not match already: try to convert it.
            try {
                return (T) convertValueToRequiredType(result, this.requiredType);
            } catch (IllegalArgumentException ex) {
                throw new TypeMismatchDataAccessException(
                        "Type mismatch affecting row number " + rowNum + " and column type '"
                                + rsmd.getColumnTypeName(1) + "': " + ex.getMessage());
            }
        }
        return (T) result;
    }

    /**
     * 检索指定列的JDBC对象值。
     * <p>默认实现调用{@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}.
     * 如果未指定所需的类型，则此方法将委托给 {@code getColumnValue(rs, index)},
     * 后者基本上调用 {@code ResultSet.getObject(index)} 但将一些额外的默认转换应用于适当的值类型。
     *
     * @param rs           是保存数据的结果集
     * @param index        列索引
     * @param requiredType 每个结果对象预期匹配的类型（如果未指定，则为null）
     * @return 对象值
     * @throws SQLException 如果提取失败
     * @see org.clever.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)
     * @see #getColumnValue(java.sql.ResultSet, int)
     */
    protected Object getColumnValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {
        if (requiredType != null) {
            return JdbcUtils.getResultSetValue(rs, index, requiredType);
        } else {
            // No required type specified -> perform default extraction.
            return getColumnValue(rs, index);
        }
    }

    /**
     * 使用最合适的值类型检索指定列的JDBC对象值。如果未指定所需类型，则调用。
     * <p>默认实现委托给 {@code JdbcUtils.getResultSetValue()}, 它使用 {@code ResultSet.getObject(index)} 方法。
     * 此外，它还包括一个“黑客”来绕过Oracle为其时间戳数据类型返回非标准对象。
     * 请参阅 {@code JdbcUtils#getResultSetValue()} javadoc获取详细信息。
     *
     * @param rs    是保存数据的结果集
     * @param index 列索引
     * @return 对象值
     * @throws SQLException 如果提取失败
     * @see org.clever.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int)
     */
    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index);
    }

    /**
     * 将给定的列值转换为指定的必需类型。仅当提取的列值不匹配时调用。
     * <p>如果所需类型为String，则值将通过 {@code toString()}。
     * 对于数字，该值将转换为数字，通过数字转换或字符串解析（取决于值类型）。
     * 否则，将使用 {@link ConversionService}.
     *
     * @param value        从中提取的列值 {@code getColumnValue()} (从不为 {@code null})
     * @param requiredType 每个结果对象预期匹配的类型(从不为 {@code null})
     * @return 转换后的值
     * @see #getColumnValue(java.sql.ResultSet, int, Class)
     */
    @SuppressWarnings("unchecked")
    protected Object convertValueToRequiredType(Object value, Class<?> requiredType) {
        if (String.class == requiredType) {
            return value.toString();
        } else if (Number.class.isAssignableFrom(requiredType)) {
            if (value instanceof Number) {
                // Convert original Number to target Number class.
                return NumberUtils.convertNumberToTargetClass(((Number) value), (Class<Number>) requiredType);
            } else {
                // Convert string field value to target Number class.
                return NumberUtils.parseNumber(value.toString(), (Class<Number>) requiredType);
            }
        } else if (this.conversionService != null && this.conversionService.canConvert(value.getClass(), requiredType)) {
            return this.conversionService.convert(value, requiredType);
        } else {
            throw new IllegalArgumentException(
                    "Value [" + value + "] is of type [" + value.getClass().getName()
                            + "] and cannot be converted to required type [" + requiredType.getName() + "]"
            );
        }
    }

    /**
     * 创建新工厂的静态工厂方法 {@code SingleColumnRowMapper}.
     *
     * @param requiredType 每个结果对象预期匹配的类型
     * @see #newInstance(Class, ConversionService)
     */
    public static <T> SingleColumnRowMapper<T> newInstance(Class<T> requiredType) {
        return new SingleColumnRowMapper<>(requiredType);
    }

    /**
     * 创建新工厂的静态工厂方法 {@code SingleColumnRowMapper}.
     *
     * @param requiredType      每个结果对象预期匹配的类型
     * @param conversionService {@link ConversionService}用于转换获取的值，或null表示无
     * @see #newInstance(Class)
     * @see #setConversionService
     */
    public static <T> SingleColumnRowMapper<T> newInstance(Class<T> requiredType, ConversionService conversionService) {
        SingleColumnRowMapper<T> rowMapper = newInstance(requiredType);
        rowMapper.setConversionService(conversionService);
        return rowMapper;
    }
}
