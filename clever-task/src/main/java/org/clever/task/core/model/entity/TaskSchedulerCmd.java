package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度器指令(task_scheduler_cmd)
 */
@Data
public class TaskSchedulerCmd implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 指定的调度器实例名称，为空表示不指定 */
    private String instanceName;
    /** 指令信息 */
    private String cmdInfo;
    /** 指令执行状态，0：未执行，1：执行中，2：执行完成 */
    private Integer state;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
