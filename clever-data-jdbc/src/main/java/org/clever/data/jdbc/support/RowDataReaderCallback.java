package org.clever.data.jdbc.support;

import org.clever.core.RenameStrategy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/09 22:37 <br/>
 */
public class RowDataReaderCallback extends InterruptRowCallbackHandler {
    /**
     * 游标读取数据回调(返回true则中断数据读取)
     */
    private final Function<RowData, Boolean> callback;
    /**
     * 数据行转Map实现
     */
    private final MapRowMapper mapRowMapper;

    /**
     * @param callback       游标读取数据回调(返回true则中断数据读取)
     * @param renameStrategy 返回数据字段名重命名策略
     */
    public RowDataReaderCallback(Function<RowData, Boolean> callback, RenameStrategy renameStrategy) {
        this.callback = callback;
        this.mapRowMapper = MapRowMapper.create(renameStrategy);
    }

    @Override
    protected void processRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> rowMap = mapRowMapper.mapRow(rs, rowNum);
        RowData rowData = new RowData(getColumnNames(), getColumnTypes(), getColumnCount(), rowMap, this.getRowCount());
        Boolean interrupted = callback.apply(rowData);
        this.interrupted = Objects.equals(interrupted, true);
    }
}
