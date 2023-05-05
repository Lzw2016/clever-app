package org.clever.task.core.model;

import lombok.Getter;
import lombok.Setter;
import org.clever.task.core.model.entity.TaskJobTrigger;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 11:48 <br/>
 */
@Getter
@Setter
public abstract class AbstractTrigger {
    /**
     * 触发器名称
     */
    protected String name;
    /**
     * 触发开始时间
     */
    protected Date startTime;
    /**
     * 触发结束时间
     */
    protected Date endTime;
    /**
     * 错过触发策略，1：忽略，2：立即补偿触发一次
     */
    protected Integer misfireStrategy;
    /**
     * 是否允许多节点并行触发，使用分布式锁实现，0：禁止，1：允许
     */
    protected Integer allowConcurrent;
    /**
     * 是否禁用：0-启用，1-禁用
     */
    protected Integer disable;
    /**
     * 描述
     */
    protected String description;

    /**
     * 任务类型，1：cron触发，2：固定速率触发
     */
    public abstract Integer getType();

    public TaskJobTrigger toJobTrigger() {
        TaskJobTrigger jobTrigger = new TaskJobTrigger();
        jobTrigger.setName(getName());
        jobTrigger.setStartTime(getStartTime());
        jobTrigger.setEndTime(getEndTime());
        jobTrigger.setMisfireStrategy(getMisfireStrategy());
        jobTrigger.setAllowConcurrent(getAllowConcurrent());
        jobTrigger.setDisable(getDisable());
        jobTrigger.setDescription(getDescription());
        jobTrigger.setType(getType());
        return jobTrigger;
    }
}
