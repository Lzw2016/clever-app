package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务执行报表(task_report)
 */
@Data
public class TaskReport implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 报表时间 */
    private String reportTime;
    /** job 运行总次数 */
    private Long jobCount;
    /** job 运行错误次数 */
    private Long jobErrCount;
    /** 触发总次数 */
    private Long triggerCount;
    /** 错过触发次数 */
    private Long misfireCount;
}
