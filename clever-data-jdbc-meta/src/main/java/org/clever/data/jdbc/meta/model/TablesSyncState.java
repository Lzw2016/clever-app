package org.clever.data.jdbc.meta.model;

import lombok.Data;
import org.clever.core.id.IDCreateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据同步信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/30 22:06 <br/>
 */
@Data
public class TablesSyncState {
    /**
     * 同步任务ID
     */
    private String jobId = IDCreateUtils.uuid();
    /**
     * 同步前清除数据
     */
    private boolean clearData = false;
    /**
     * 同步时跳过所有错误
     */
    private boolean skipError = true;
    /**
     * 数据表同步信息
     */
    private List<TableState> tableSyncInfos = new ArrayList<>();
    /**
     * 当前同步的表信息
     */
    private TableState currentSync;
    /**
     * 是否中断
     */
    private volatile boolean interrupt = false;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 是否同步成功
     */
    private Boolean success;
    /**
     * 同步失败的错误消息
     */
    private String errorMsg;
}
