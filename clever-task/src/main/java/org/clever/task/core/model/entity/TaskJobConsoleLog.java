package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务控制台日志(task_job_console_log)
 */
@Data
public class TaskJobConsoleLog implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 调度器实例名称 */
    private String instanceName;
    /** 任务ID */
    private Long jobId;
    /** 任务执行日志ID */
    private Long jobLogId;
    /** 日志行号 */
    private Integer lineNum;
    /** 日志内容 */
    private String log;
    /** 创建时间 */
    private Date createAt;
}
