package org.clever.jdbc.support.rowset;

import org.clever.jdbc.InvalidResultSetAccessException;

/**
 * {@link SqlRowSet}的元数据接口，类似于JDBC的{@link java.sql.ResultSetMetaData}
 *
 * <p>与标准JDBC ResultSetMetaData的主要区别在于这里从不抛出{@link java.sql.SQLException}。
 * 这允许使用SqlRowSetMetaData，而不必处理已检查的异常。
 * SqlRowSetMetaData将抛出{@link InvalidResultSetAccessException（如果合适）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:34 <br/>
 *
 * @see SqlRowSet#getMetaData()
 * @see java.sql.ResultSetMetaData
 * @see org.clever.jdbc.InvalidResultSetAccessException
 */
public interface SqlRowSetMetaData {
    /**
     * 检索用作指定列源的表的目录名称。
     *
     * @param columnIndex 列的索引
     * @return 目录名称
     * @see java.sql.ResultSetMetaData#getCatalogName(int)
     */
    String getCatalogName(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索指定列将映射到的完全限定类
     *
     * @param columnIndex 列的索引
     * @return 以字符串形式显示的类名
     * @see java.sql.ResultSetMetaData#getColumnClassName(int)
     */
    String getColumnClassName(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索行集中的列数
     *
     * @return 列数
     * @see java.sql.ResultSetMetaData#getColumnCount()
     */
    int getColumnCount() throws InvalidResultSetAccessException;

    /**
     * 返回结果集表示的表的列名
     *
     * @return 列名
     */
    String[] getColumnNames() throws InvalidResultSetAccessException;

    /**
     * 检索指定列的最大宽度
     *
     * @param columnIndex 列的索引
     * @return 列的宽度
     * @see java.sql.ResultSetMetaData#getColumnDisplaySize(int)
     */
    int getColumnDisplaySize(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索指定列的建议列标题
     *
     * @param columnIndex 列的索引
     * @return 列标题
     * @see java.sql.ResultSetMetaData#getColumnLabel(int)
     */
    String getColumnLabel(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索指示列的列名
     *
     * @param columnIndex 列的索引
     * @return 列名
     * @see java.sql.ResultSetMetaData#getColumnName(int)
     */
    String getColumnName(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索指示列的SQL类型代码
     *
     * @param columnIndex 列的索引
     * @return SQL类型代码
     * @see java.sql.ResultSetMetaData#getColumnType(int)
     * @see java.sql.Types
     */
    int getColumnType(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索指定列的DBMS特定类型名称
     *
     * @param columnIndex 列的索引
     * @return 类型名称
     * @see java.sql.ResultSetMetaData#getColumnTypeName(int)
     */
    String getColumnTypeName(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索指示列的精度
     *
     * @param columnIndex 列的索引
     * @return 精度
     * @see java.sql.ResultSetMetaData#getPrecision(int)
     */
    int getPrecision(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索指示列的scale
     *
     * @param columnIndex 列的索引
     * @return scale
     * @see java.sql.ResultSetMetaData#getScale(int)
     */
    int getScale(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索用作指定列源的表的schema name
     *
     * @param columnIndex 列的索引
     * @return schema name
     * @see java.sql.ResultSetMetaData#getSchemaName(int)
     */
    String getSchemaName(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 检索用作指定列源的表的名称
     *
     * @param columnIndex 列的索引
     * @return 表的名称
     * @see java.sql.ResultSetMetaData#getTableName(int)
     */
    String getTableName(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 指出指定列的大小写是否重要
     *
     * @param columnIndex 列的索引
     * @return 如果区分大小写，则为true，否则为false
     * @see java.sql.ResultSetMetaData#isCaseSensitive(int)
     */
    boolean isCaseSensitive(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 指示指定列是否包含货币值
     *
     * @param columnIndex 列的索引
     * @return 如果值是货币值，则为true，否则为false
     * @see java.sql.ResultSetMetaData#isCurrency(int)
     */
    boolean isCurrency(int columnIndex) throws InvalidResultSetAccessException;

    /**
     * 指示指定列是否包含有符号的数字
     *
     * @param columnIndex 列的索引
     * @return 如果列包含有符号的数字，则为true，否则为false
     * @see java.sql.ResultSetMetaData#isSigned(int)
     */
    boolean isSigned(int columnIndex) throws InvalidResultSetAccessException;
}
