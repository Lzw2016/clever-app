package org.clever.data.jdbc.support;

import org.clever.core.RenameStrategy;
import org.clever.jdbc.core.RowCountCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/09 22:37 <br/>
 */
public class RowDataReaderCallback extends RowCountCallbackHandler {
    /**
     * 游标读取数据消费者
     */
    private final Consumer<RowData> consumer;
    /**
     * 数据行转Map实现
     */
    private final MapRowMapper mapRowMapper;

    /**
     * @param consumer       读取数据消费者
     * @param renameStrategy 返回数据字段名重命名策略
     */
    public RowDataReaderCallback(Consumer<RowData> consumer, RenameStrategy renameStrategy) {
        this.consumer = consumer;
        this.mapRowMapper = MapRowMapper.create(renameStrategy);
    }

    @Override
    protected void processRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> rowData = mapRowMapper.mapRow(rs, rowNum);
        consumer.accept(new RowData(getColumnNames(), getColumnTypes(), getColumnCount(), rowData, this.getRowCount()));
    }
}
