package org.clever.task.manage.model.request;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/28 14:19 <br/>
 */
@Data
public class JobConsoleLogReq {
    /**
     * 任务执行日志ID
     */
    private Long jobLogId;
    /**
     * 开始行号
     */
    private Integer startLineNum;
}
