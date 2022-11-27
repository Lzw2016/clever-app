package org.clever.data.jdbc.support;

import org.clever.core.RenameStrategy;
import org.clever.jdbc.core.RowCountCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 游标批量读取模式
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/02/19 17:53 <br/>
 */
public class BatchDataReaderCallback extends RowCountCallbackHandler {
    private static final int Default_Batch_Size = 200;

    /**
     * 一个批次的数据量
     */
    private final int batchSize;
    /**
     * 游标批次读取数据消费者
     */
    private final Consumer<BatchData> consumer;
    /**
     * 数据行转Map实现
     */
    private final MapRowMapper mapRowMapper;
    /**
     * 读取数据
     */
    private List<Map<String, Object>> rowDataList;

    /**
     * @param batchSize    一个批次的数据量
     * @param consumer     读取数据消费者
     * @param resultRename 返回数据字段名重命名策略
     */
    public BatchDataReaderCallback(int batchSize, Consumer<BatchData> consumer, RenameStrategy resultRename) {
        this.batchSize = batchSize <= 0 ? Default_Batch_Size : batchSize;
        this.consumer = consumer;
        this.mapRowMapper = MapRowMapper.create(resultRename);
        this.rowDataList = new ArrayList<>(this.batchSize);
    }

    @Override
    protected synchronized void processRow( ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> rowData = mapRowMapper.mapRow(rs, rowNum);
        rowDataList.add(rowData);
        if (rowDataList.size() >= batchSize) {
            consumer.accept(new BatchData(getColumnNames(), getColumnTypes(), getColumnCount(), rowDataList, this.getRowCount()));
            rowDataList = new ArrayList<>(this.batchSize);
        }
    }

    public synchronized void processEnd() {
        if (rowDataList.isEmpty()) {
            return;
        }
        consumer.accept(new BatchData(getColumnNames(), getColumnTypes(), getColumnCount(), rowDataList, this.getRowCount()));
        rowDataList = new ArrayList<>();
    }
}
