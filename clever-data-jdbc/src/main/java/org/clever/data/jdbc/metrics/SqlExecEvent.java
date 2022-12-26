package org.clever.data.jdbc.metrics;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/03/15 19:24 <br/>
 */
@Data
public class SqlExecEvent {
    /**
     * 数据源标识
     */
    private final String dataSourceName;
    /**
     * 将所有绑定变量替换为实际值的sql语句
     */
    private final String prepared;
    /**
     * 执行的sql语句
     */
    private final String sql;
    /**
     * 操作完成所需的时间(以毫秒为单位)
     */
    private final long cost;

    public SqlExecEvent(String dataSourceName, String prepared, String sql, long cost) {
        this.dataSourceName = dataSourceName;
        this.prepared = prepared;
        this.sql = sql;
        this.cost = cost;
    }
}
