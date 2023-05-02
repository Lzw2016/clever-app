package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务触发器日志(task_job_trigger_log)
 */
@Data
public class TaskJobTriggerLog implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 调度器实例名称 */
    private String instanceName;
    /** 任务触发器ID */
    private Long jobTriggerId;
    /** 任务ID */
    private Long jobId;
    /** 触发器名称 */
    private String triggerName;
    /** 触发时间 */
    private Date fireTime;
    /** 是否是手动触发，0：系统自动触发，1：用户手动触发 */
    private Integer isManual;
    /** 触发耗时(单位：毫秒) */
    private Integer triggerTime;
    /** 上一次触发时间 */
    private Date lastFireTime;
    /** 下一次触发时间 */
    private Date nextFireTime;
    /** 触发次数 */
    private Long fireCount;
    /** 是否错过了触发，0：否，1：是 */
    private Integer misFired;
    /** 触发器消息 */
    private String triggerMsg;
    /** 创建时间 */
    private Date createAt;
}
