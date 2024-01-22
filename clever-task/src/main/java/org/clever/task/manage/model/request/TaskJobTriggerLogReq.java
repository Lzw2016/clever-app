package org.clever.task.manage.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.model.request.QueryByPage;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/04 22:41 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskJobTriggerLogReq extends QueryByPage {
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 调度器实例名称
     */
    private String instanceName;
    /**
     * 任务触发器ID
     */
    private Long jobTriggerId;
    /**
     * 是否是手动触发
     */
    private Integer isManual;
    /**
     * 是否错过了触发
     */
    private Integer misFired;
    /**
     * 触发时间 - 开始
     */
    private Date fireTimeStart;
    /**
     * 触发时间 - 结束
     */
    private Date fireTimeEnd;
}
