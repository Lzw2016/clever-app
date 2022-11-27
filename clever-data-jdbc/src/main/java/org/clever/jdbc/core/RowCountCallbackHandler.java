package org.clever.jdbc.core;

import org.clever.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * RowCallbackHandler 的实现。回调处理程序的方便超类。
 * 一个实例只能使用一次。
 *
 * <p>我们可以单独使用它（例如，在测试用例中，以确保我们的结果集具有有效的维度），
 * 或者将它用作回调处理程序的超类，这些回调处理程序实际执行某些操作，并将受益于它的维度信息提供。
 *
 * <p>JdbcTemplate 的用法示例：
 * <pre>{@code
 * JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
 * jdbcTemplate.query("select * from user", countCallback);
 * int rowCount = countCallback.getRowCount();
 * }</pre>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 21:30 <br/>
 */
public class RowCountCallbackHandler implements RowCallbackHandler {
    /**
     * 行数量
     */
    private int rowCount;
    /**
     * 列数量
     */
    private int columnCount;
    /**
     * 从 0 开始索引。 ResultSetMetaData 对象返回的列的类型（如在 java.sql.Types 中）
     */
    private int[] columnTypes;
    /**
     * 从 0 开始索引。ResultSetMetaData 对象返回的列名
     */
    private String[] columnNames;

    /**
     * ResultSetCallbackHandler 的实现。
     * 如果这是第一行，计算列大小，否则只计算行数。
     * <p>子类可以通过覆盖 {@code processRow(ResultSet, int)} 方法来执行自定义提取或处理
     *
     * @see #processRow(java.sql.ResultSet, int)
     */
    @Override
    public final void processRow(ResultSet rs) throws SQLException {
        if (this.rowCount == 0) {
            ResultSetMetaData rsmd = rs.getMetaData();
            this.columnCount = rsmd.getColumnCount();
            this.columnTypes = new int[this.columnCount];
            this.columnNames = new String[this.columnCount];
            for (int i = 0; i < this.columnCount; i++) {
                this.columnTypes[i] = rsmd.getColumnType(i + 1);
                this.columnNames[i] = JdbcUtils.lookupColumnName(rsmd, i + 1);
            }
            // could also get column names
        }
        processRow(rs, this.rowCount++);
    }

    /**
     * 子类可以覆盖它以执行自定义提取或处理。这个类的实现什么也不做。
     *
     * @param rs     要从中提取数据的 ResultSet。为每一行调用此方法
     * @param rowNum 当前行号（从0开始）
     */
    protected void processRow(ResultSet rs, int rowNum) throws SQLException {
    }

    /**
     * 将列的类型作为 java.sql.Types 常量返回，在第一次调用 processRow 后有效
     *
     * @return 列的类型作为 java.sql.Types 常量。<b>索引从 0 到 n-1</b>
     */
    public final int[] getColumnTypes() {
        return this.columnTypes;
    }

    /**
     * 返回列的名称。在第一次调用 processRow 后有效
     *
     * @return 列的名称。
     * <b>索引从 0 到 n-1</b>
     */
    public final String[] getColumnNames() {
        return this.columnNames;
    }

    /**
     * 返回此 ResultSet 的行数。处理完成后才有效
     *
     * @return 此 ResultSet 中的行数
     */
    public final int getRowCount() {
        return this.rowCount;
    }

    /**
     * 返回此结果集中的列数。一旦我们看到第一行就有效，因此子类可以在处理期间使用它
     *
     * @return 此结果集中的列数
     */
    public final int getColumnCount() {
        return this.columnCount;
    }
}
