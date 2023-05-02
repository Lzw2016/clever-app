package org.clever.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度器事件日志(task_scheduler_log)
 */
@Data
public class TaskSchedulerLog implements Serializable {
    /** 编号 */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 调度器实例名称 */
    private String instanceName;
    /** 事件名称 */
    private String eventName;
    /** 事件日志数据 */
    private String logData;
    /** 创建时间 */
    private Date createAt;
}
