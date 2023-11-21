package org.clever.data.jdbc.meta.model;

import lombok.Data;

import java.util.Objects;

/**
 * 表数据同步信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/30 22:04 <br/>
 */
@Data
public class TableState {
    /**
     * 表名称
     */
    private String tableName;
    /**
     * 总数据量
     */
    private Integer dataCount;
    /**
     * 当前同步位置
     */
    private Integer currentIdx;
    /**
     * 同步成功数据量
     */
    private Integer syncCount;
    /**
     * 删除的数据量
     */
    private Integer deleteCount;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 当前同步进度
     */
    Double getSyncProgress() {
        if (dataCount == null || currentIdx == null) {
            return 0D;
        }
        if (Objects.equals(dataCount, currentIdx)) {
            return 100.0;
        }
        return currentIdx * 100.0D / dataCount;
    }
}
