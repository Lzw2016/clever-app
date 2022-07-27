package org.clever.jdbc.support.rowset;

import org.clever.jdbc.InvalidResultSetAccessException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * {@link javax.sql.RowSet}的镜像接口，表示断开连接的{@link java.sql.ResultSet}
 *
 * <p>与标准JDBC行集的主要区别在于这里从不抛出{@link java.sql.SQLException}。
 * 这允许使用SqlRowSet，而不必处理已检查的异常。
 * SqlRowSet将抛出{@link InvalidResultSetAccessException}（如果合适）。
 *
 * <p>注意：此接口扩展了{@code java.io.Serializable}标记接口。
 * 通常保存断开连接的数据的实现被鼓励实际可序列化（尽可能）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:33 <br/>
 *
 * @see javax.sql.RowSet
 * @see java.sql.ResultSet
 * @see InvalidResultSetAccessException
 * @see org.clever.jdbc.core.JdbcTemplate#queryForRowSet
 */
public interface SqlRowSet extends Serializable {
    /**
     * 检索元数据，即此行集列的编号、类型和属性。
     *
     * @return 对应的SqlRowSetMetaData实例
     * @see java.sql.ResultSet#getMetaData()
     */
    SqlRowSetMetaData getMetaData();

    /**
     * 将给定的列标签Map到其列索引
     *
     * @param columnLabel 列的名称
     * @return 给定列标签的列索引
     * @see java.sql.ResultSet#findColumn(String)
     */
    int findColumn(String columnLabel) throws InvalidResultSetAccessException;

    // RowSet methods for extracting data values

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getBigDecimal(int)
     */
    BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getBigDecimal(String)
     */
    BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getBoolean(int)
     */
    boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getBoolean(String)
     */
    boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getByte(int)
     */
    byte getByte(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getByte(String)
     */
    byte getByte(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getDate(int)
     */
    Date getDate(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getDate(String)
     */
    Date getDate(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @param cal         用于构建日期的日历
     * @see java.sql.ResultSet#getDate(int, Calendar)
     */
    Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @param cal         用于构建日期的日历
     * @see java.sql.ResultSet#getDate(String, Calendar)
     */
    Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getDouble(int)
     */
    double getDouble(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getDouble(String)
     */
    double getDouble(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getFloat(int)
     */
    float getFloat(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getFloat(String)
     */
    float getFloat(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getInt(int)
     */
    int getInt(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getInt(String)
     */
    int getInt(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getLong(int)
     */
    long getLong(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getLong(String)
     */
    long getLong(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getNString(int)
     */
    String getNString(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getNString(String)
     */
    String getNString(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getObject(int)
     */
    Object getObject(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getObject(String)
     */
    Object getObject(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @param map         一个映射对象，包含从SQL类型到Java类型的映射
     * @return 表示列值的对象
     * @see java.sql.ResultSet#getObject(int, Map)
     */
    Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @param map         一个映射对象，包含从SQL类型到Java类型的映射
     * @return 表示列值的对象
     * @see java.sql.ResultSet#getObject(String, Map)
     */
    Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @param type        要将指定列转换为的Java类型
     * @return 表示列值的对象
     * @see java.sql.ResultSet#getObject(int, Class)
     */
    <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException;

    /**
     * 检索当前行中指示列的值作为对象
     *
     * @param columnLabel 列标签
     * @param type        要将指定列转换为的Java类型
     * @return 表示列值的对象
     * @see java.sql.ResultSet#getObject(String, Class)
     */
    <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getShort(int)
     */
    short getShort(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getShort(String)
     */
    short getShort(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getString(int)
     */
    String getString(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getString(String)
     */
    String getString(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getTime(int)
     */
    Time getTime(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getTime(String)
     */
    Time getTime(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @param cal         用于构建日期的日历
     * @see java.sql.ResultSet#getTime(int, Calendar)
     */
    Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @param cal         用于构建日期的日历
     * @see java.sql.ResultSet#getTime(String, Calendar)
     */
    Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @see java.sql.ResultSet#getTimestamp(int)
     */
    Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @see java.sql.ResultSet#getTimestamp(String)
     */
    Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException;

    /**
     * @param columnIndex 列索引
     * @param cal         用于构建日期的日历
     * @see java.sql.ResultSet#getTimestamp(int, Calendar)
     */
    Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

    /**
     * @param columnLabel 列标签
     * @param cal         用于构建日期的日历
     * @see java.sql.ResultSet#getTimestamp(String, Calendar)
     */
    Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

    // RowSet navigation methods

    /**
     * 将光标移动到行集中的给定行号，就在最后一行之后
     *
     * @param row 光标应移动的行数
     * @return 如果光标位于行集合上，则为true，否则为false
     * @see java.sql.ResultSet#absolute(int)
     */
    boolean absolute(int row) throws InvalidResultSetAccessException;

    /**
     * 将光标移动到此行集的末尾
     *
     * @see java.sql.ResultSet#afterLast()
     */
    void afterLast() throws InvalidResultSetAccessException;

    /**
     * 将光标移到此行集的前面，即第一行之前
     *
     * @see java.sql.ResultSet#beforeFirst()
     */
    void beforeFirst() throws InvalidResultSetAccessException;

    /**
     * 将光标移动到此行集的第一行
     *
     * @return 如果光标位于有效行上，则为true，否则为false
     * @see java.sql.ResultSet#first()
     */
    boolean first() throws InvalidResultSetAccessException;

    /**
     * 检索当前行号
     *
     * @return 当前行号
     * @see java.sql.ResultSet#getRow()
     */
    int getRow() throws InvalidResultSetAccessException;

    /**
     * 检索光标是否位于该行集合的最后一行之后
     *
     * @return 如果光标位于最后一行之后，则为true，否则为false
     * @see java.sql.ResultSet#isAfterLast()
     */
    boolean isAfterLast() throws InvalidResultSetAccessException;

    /**
     * 检索光标是否位于该行集合的第一行之前
     *
     * @return 如果光标位于第一行之前，则为true，否则为false
     * @see java.sql.ResultSet#isBeforeFirst()
     */
    boolean isBeforeFirst() throws InvalidResultSetAccessException;

    /**
     * 检索光标是否位于此行集的第一行
     *
     * @return 如果光标位于第一行之后，则为true，否则为false
     * @see java.sql.ResultSet#isFirst()
     */
    boolean isFirst() throws InvalidResultSetAccessException;

    /**
     * 检索光标是否位于此行集的最后一行
     *
     * @return 如果光标位于最后一行之后，则为true，否则为false
     * @see java.sql.ResultSet#isLast()
     */
    boolean isLast() throws InvalidResultSetAccessException;

    /**
     * 将光标移动到此行集的最后一行
     *
     * @return 如果光标位于有效行上，则为true，否则为false
     * @see java.sql.ResultSet#last()
     */
    boolean last() throws InvalidResultSetAccessException;

    /**
     * 将光标移动到下一行
     *
     * @return 如果新行有效，则为true；如果没有更多行，则为false
     * @see java.sql.ResultSet#next()
     */
    boolean next() throws InvalidResultSetAccessException;

    /**
     * 将光标移到上一行
     *
     * @return 如果新行有效，则为true；如果新行不在行集合中，则为false
     * @see java.sql.ResultSet#previous()
     */
    boolean previous() throws InvalidResultSetAccessException;

    /**
     * 将光标移动相对行数（正或负）
     *
     * @return 如果光标位于一行上，则为true，否则为false
     * @see java.sql.ResultSet#relative(int)
     */
    boolean relative(int rows) throws InvalidResultSetAccessException;

    /**
     * 报告上次读取的列的值是否为SQL NULL
     * <p>请注意，您必须首先调用一个getter方法，然后调用{@code wasNull()}方法。
     *
     * @return 如果检索到的最新列为SQL NULL，则为true，否则为false
     * @see java.sql.ResultSet#wasNull()
     */
    boolean wasNull() throws InvalidResultSetAccessException;
}
