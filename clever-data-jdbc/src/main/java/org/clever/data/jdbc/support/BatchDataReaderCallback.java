package org.clever.data.jdbc.support;

import org.clever.core.RenameStrategy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 游标批量读取模式
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/02/19 17:53 <br/>
 */
public class BatchDataReaderCallback extends InterruptRowCallbackHandler {
    private static final int DEFAULT_BATCH_SIZE = 200;

    /**
     * 一个批次的数据量
     */
    private final int batchSize;
    /**
     * 游标批次读取数据回调(返回true则中断数据读取)
     */
    private final Function<BatchData, Boolean> callback;
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
     * @param callback     游标批次读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public BatchDataReaderCallback(int batchSize, Function<BatchData, Boolean> callback, RenameStrategy resultRename) {
        this.batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        this.callback = callback;
        this.mapRowMapper = MapRowMapper.create(resultRename);
        this.rowDataList = new ArrayList<>(this.batchSize);
    }

    @Override
    protected void processRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> rowData = mapRowMapper.mapRow(rs, rowNum);
        rowDataList.add(rowData);
        if (rowDataList.size() >= batchSize) {
            BatchData batchData = new BatchData(getColumnNames(), getColumnTypes(), getColumnCount(), rowDataList, this.getRowCount());
            Boolean interrupted = callback.apply(batchData);
            this.interrupted = Objects.equals(interrupted, true);
            rowDataList = new ArrayList<>(this.batchSize);
        }
    }

    @Override
    public void processEnd() {
        if (rowDataList.isEmpty()) {
            return;
        }
        BatchData batchData = new BatchData(getColumnNames(), getColumnTypes(), getColumnCount(), rowDataList, this.getRowCount());
        callback.apply(batchData);
        rowDataList = new ArrayList<>();
    }
}
