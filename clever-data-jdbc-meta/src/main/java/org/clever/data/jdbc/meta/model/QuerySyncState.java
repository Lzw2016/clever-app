package org.clever.data.jdbc.meta.model;

import lombok.Data;
import org.clever.core.id.IDCreateUtils;

/**
 * 查询同状态
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/11/21 17:51 <br/>
 */
@Data
public class QuerySyncState {
    /**
     * 同步任务ID
     */
    private String jobId = IDCreateUtils.uuid();
    /**
     * 同步时跳过所有错误
     */
    private boolean skipError = true;
    /**
     * 查询sql
     */
    private String querySql;
    /**
     * 目标表
     */
    private String targetTableName;
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
