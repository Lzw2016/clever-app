package org.clever.jdbc.support.rowset;

import org.clever.jdbc.InvalidResultSetAccessException;
import org.clever.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

/**
 * {@link SqlRowSet}接口的默认实现，包装{@link ResultSet}，
 * 捕获任何{@link SQLException}并将其转换为相应的{@link InvalidResultSetAccessException}。
 *
 * <p>如果SqlRowSet应该以断开连接的方式可用，则传入的结果集应该已经断开连接。
 * 这意味着您通常会传入一个{@code javax.sql.rowset.CachedRowSet}，它实现了ResultSet接口。
 *
 * <p>注意：自JDBC 4.0以来，已经澄清了使用字符串标识列的任何方法都应该使用列标签。
 * 列标签是使用SQL查询字符串中的ALIAS关键字指定的。当查询不使用别名时，默认标签是列名。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:43 <br/>
 *
 * @see ResultSet
 * @see javax.sql.rowset.CachedRowSet
 * @see org.clever.jdbc.core.JdbcTemplate#queryForRowSet
 */
public class ResultSetWrappingSqlRowSet implements SqlRowSet {
    private final ResultSet resultSet;
    private final SqlRowSetMetaData rowSetMetaData;
    private final Map<String, Integer> columnLabelMap;

    /**
     * 为给定的ResultSet创建新的ResultSetWrappingSqlRowSet
     *
     * @param resultSet 要包装的断开连接的结果集(通常是{@code javax.sql.rowset.CachedRowSet})
     * @throws InvalidResultSetAccessException 如果提取ResultSetMetaData失败
     * @see javax.sql.rowset.CachedRowSet
     * @see ResultSet#getMetaData
     * @see ResultSetWrappingSqlRowSetMetaData
     */
    public ResultSetWrappingSqlRowSet(ResultSet resultSet) throws InvalidResultSetAccessException {
        this.resultSet = resultSet;
        try {
            this.rowSetMetaData = new ResultSetWrappingSqlRowSetMetaData(resultSet.getMetaData());
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
        try {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            if (rsmd != null) {
                int columnCount = rsmd.getColumnCount();
                this.columnLabelMap = CollectionUtils.newHashMap(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    String key = rsmd.getColumnLabel(i);
                    // Make sure to preserve first matching column for any given name,
                    // as defined in ResultSet's type-level javadoc (lines 81 to 83).
                    if (!this.columnLabelMap.containsKey(key)) {
                        this.columnLabelMap.put(key, i);
                    }
                }
            } else {
                this.columnLabelMap = Collections.emptyMap();
            }
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * 返回基础结果集 (通常是 {@code javax.sql.rowset.CachedRowSet}).
     *
     * @see javax.sql.rowset.CachedRowSet
     */
    public final ResultSet getResultSet() {
        return this.resultSet;
    }

    /**
     * @see ResultSetMetaData#getCatalogName(int)
     */
    @Override
    public final SqlRowSetMetaData getMetaData() {
        return this.rowSetMetaData;
    }

    /**
     * @see ResultSet#findColumn(String)
     */
    @Override
    public int findColumn(String columnLabel) throws InvalidResultSetAccessException {
        Integer columnIndex = this.columnLabelMap.get(columnLabel);
        if (columnIndex != null) {
            return columnIndex;
        } else {
            try {
                return this.resultSet.findColumn(columnLabel);
            } catch (SQLException se) {
                throw new InvalidResultSetAccessException(se);
            }
        }
    }

    // RowSet methods for extracting data values

    /**
     * @see ResultSet#getBigDecimal(int)
     */
    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getBigDecimal(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getBigDecimal(String)
     */
    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException {
        return getBigDecimal(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getBoolean(int)
     */
    @Override
    public boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getBoolean(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getBoolean(String)
     */
    @Override
    public boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException {
        return getBoolean(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getByte(int)
     */
    @Override
    public byte getByte(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getByte(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getByte(String)
     */
    @Override
    public byte getByte(String columnLabel) throws InvalidResultSetAccessException {
        return getByte(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getDate(int)
     */
    @Override
    public Date getDate(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getDate(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getDate(String)
     */
    @Override
    public Date getDate(String columnLabel) throws InvalidResultSetAccessException {
        return getDate(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getDate(int, Calendar)
     */
    @Override
    public Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getDate(columnIndex, cal);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getDate(String, Calendar)
     */
    @Override
    public Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return getDate(findColumn(columnLabel), cal);
    }

    /**
     * @see ResultSet#getDouble(int)
     */
    @Override
    public double getDouble(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getDouble(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getDouble(String)
     */
    @Override
    public double getDouble(String columnLabel) throws InvalidResultSetAccessException {
        return getDouble(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getFloat(int)
     */
    @Override
    public float getFloat(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getFloat(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getFloat(String)
     */
    @Override
    public float getFloat(String columnLabel) throws InvalidResultSetAccessException {
        return getFloat(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getInt(int)
     */
    @Override
    public int getInt(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getInt(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getInt(String)
     */
    @Override
    public int getInt(String columnLabel) throws InvalidResultSetAccessException {
        return getInt(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getLong(int)
     */
    @Override
    public long getLong(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getLong(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getLong(String)
     */
    @Override
    public long getLong(String columnLabel) throws InvalidResultSetAccessException {
        return getLong(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getNString(int)
     */
    @Override
    public String getNString(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getNString(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getNString(String)
     */
    @Override
    public String getNString(String columnLabel) throws InvalidResultSetAccessException {
        return getNString(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getObject(int)
     */
    @Override
    public Object getObject(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getObject(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getObject(String)
     */
    @Override
    public Object getObject(String columnLabel) throws InvalidResultSetAccessException {
        return getObject(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getObject(int, Map)
     */
    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getObject(columnIndex, map);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getObject(String, Map)
     */
    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
        return getObject(findColumn(columnLabel), map);
    }

    /**
     * @see ResultSet#getObject(int, Class)
     */
    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getObject(columnIndex, type);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getObject(String, Class)
     */
    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException {
        return getObject(findColumn(columnLabel), type);
    }

    /**
     * @see ResultSet#getShort(int)
     */
    @Override
    public short getShort(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getShort(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getShort(String)
     */
    @Override
    public short getShort(String columnLabel) throws InvalidResultSetAccessException {
        return getShort(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getString(int)
     */
    @Override
    public String getString(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getString(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getString(String)
     */
    @Override
    public String getString(String columnLabel) throws InvalidResultSetAccessException {
        return getString(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getTime(int)
     */
    @Override
    public Time getTime(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getTime(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getTime(String)
     */
    @Override
    public Time getTime(String columnLabel) throws InvalidResultSetAccessException {
        return getTime(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getTime(int, Calendar)
     */
    @Override
    public Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getTime(columnIndex, cal);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getTime(String, Calendar)
     */
    @Override
    public Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return getTime(findColumn(columnLabel), cal);
    }

    /**
     * @see ResultSet#getTimestamp(int)
     */
    @Override
    public Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getTimestamp(columnIndex);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getTimestamp(String)
     */
    @Override
    public Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException {
        return getTimestamp(findColumn(columnLabel));
    }

    /**
     * @see ResultSet#getTimestamp(int, Calendar)
     */
    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getTimestamp(columnIndex, cal);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getTimestamp(String, Calendar)
     */
    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return getTimestamp(findColumn(columnLabel), cal);
    }

    // RowSet navigation methods

    /**
     * @see ResultSet#absolute(int)
     */
    @Override
    public boolean absolute(int row) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.absolute(row);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#afterLast()
     */
    @Override
    public void afterLast() throws InvalidResultSetAccessException {
        try {
            this.resultSet.afterLast();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#beforeFirst()
     */
    @Override
    public void beforeFirst() throws InvalidResultSetAccessException {
        try {
            this.resultSet.beforeFirst();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#first()
     */
    @Override
    public boolean first() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.first();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#getRow()
     */
    @Override
    public int getRow() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.getRow();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#isAfterLast()
     */
    @Override
    public boolean isAfterLast() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.isAfterLast();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#isBeforeFirst()
     */
    @Override
    public boolean isBeforeFirst() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.isBeforeFirst();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#isFirst()
     */
    @Override
    public boolean isFirst() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.isFirst();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#isLast()
     */
    @Override
    public boolean isLast() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.isLast();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#last()
     */
    @Override
    public boolean last() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.last();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#next()
     */
    @Override
    public boolean next() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.next();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#previous()
     */
    @Override
    public boolean previous() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.previous();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#relative(int)
     */
    @Override
    public boolean relative(int rows) throws InvalidResultSetAccessException {
        try {
            return this.resultSet.relative(rows);
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }

    /**
     * @see ResultSet#wasNull()
     */
    @Override
    public boolean wasNull() throws InvalidResultSetAccessException {
        try {
            return this.resultSet.wasNull();
        } catch (SQLException se) {
            throw new InvalidResultSetAccessException(se);
        }
    }
}
