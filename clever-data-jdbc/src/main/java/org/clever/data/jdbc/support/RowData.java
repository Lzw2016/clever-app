package org.clever.data.jdbc.support;

import lombok.Getter;
import lombok.ToString;
import org.clever.core.mapper.BeanCopyUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/09 22:38 <br/>
 */
@ToString
@Getter
public class RowData implements Serializable {
    /**
     * 列名称集合
     */
    private final List<String> columnNames;
    /**
     * 列类型集合
     */
    private final List<Integer> columnTypes;
    /**
     * 列宽(列数)
     */
    private final int columnCount;
    /**
     * 当前批次数
     */
    private final Map<String, Object> rowData;
    /**
     * 当前读取的行数
     */
    private final int rowCount;

    public RowData(String[] columnNames, int[] columnTypes, int columnCount, Map<String, Object> rowData, int rowCount) {
        this.columnNames = Arrays.asList(columnNames);
        Integer[] columnTypesTmp;
        if (columnTypes == null) {
            columnTypesTmp = new Integer[0];
        } else {
            columnTypesTmp = new Integer[columnTypes.length];
            for (int i = 0; i < columnTypes.length; i++) {
                columnTypesTmp[i] = columnTypes[i];
            }
        }
        this.columnTypes = Arrays.asList(columnTypesTmp);
        this.columnCount = columnCount;
        this.rowData = rowData;
        this.rowCount = rowCount;
    }

    public int[] getRawColumnTypes() {
        int[] result = new int[columnTypes.size()];
        for (int i = 0; i < columnTypes.size(); i++) {
            result[i] = columnTypes.get(i);
        }
        return result;
    }

    public <T> T getRowData(Class<T> clazz) {
        return BeanCopyUtils.toBean(rowData, clazz);
    }
}
