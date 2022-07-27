package org.clever.jdbc.core;

import org.clever.jdbc.support.JdbcUtils;
import org.clever.util.LinkedCaseInsensitiveMap;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * 创建行{@code java.util.Map}实现{@link RowMapper}，将所有列表示为键值对：每列一个条目，列名为键。
 *
 * <p>可以通过分别重写{@link #createColumnMap}和{@link #getColumnKey}来定制列映射中每个列要使用的映射实现和要使用的键。
 *
 * <p>注意：默认情况下，{@code ColumnMapRowMapper}将尝试使用不区分大小写的键构建链接映射，以保留列顺序，并允许对列名使用任何大小写。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:04 <br/>
 *
 * @see JdbcTemplate#queryForList(String)
 * @see JdbcTemplate#queryForMap(String)
 */
public class ColumnMapRowMapper implements RowMapper<Map<String, Object>> {
    @Override
    public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        Map<String, Object> mapOfColumnValues = createColumnMap(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            String column = JdbcUtils.lookupColumnName(rsmd, i);
            mapOfColumnValues.putIfAbsent(getColumnKey(column), getColumnValue(rs, i));
        }
        return mapOfColumnValues;
    }

    /**
     * 创建要用作列映射的映射实例。
     * <p>默认情况下，将创建链接的不区分大小写的映射。
     *
     * @param columnCount 列计数，用作映射的初始容量
     * @return 新Map实例
     * @see org.clever.util.LinkedCaseInsensitiveMap
     */
    protected Map<String, Object> createColumnMap(int columnCount) {
        return new LinkedCaseInsensitiveMap<>(columnCount);
    }

    /**
     * 确定用于列映射中给定列的键。
     * <p>默认情况下，提供的列名将未经修改地返回。
     *
     * @param columnName 结果集返回的列名
     * @return 要使用的列键
     * @see ResultSetMetaData#getColumnName
     */
    protected String getColumnKey(String columnName) {
        return columnName;
    }

    /**
     * 检索指定列的JDBC对象值。
     * <p>默认实现使用{@code getObject}方法。
     * 此外，此实现还包括一个“hack”来绕过Oracle返回非标准对象作为其时间戳数据类型的问题。
     *
     * @param rs    保存数据的结果集
     * @param index 列索引
     * @return 返回Object
     * @see JdbcUtils#getResultSetValue
     */
    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index);
    }
}
