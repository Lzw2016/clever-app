package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度器集群锁(task_scheduler_lock)
 */
@Data
public class TaskSchedulerLock implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 锁名称 */
    private String lockName;
    /** 锁次数 */
    private Long lockCount;
    /** 描述 */
    private String description;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
