package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.task.core.model.entity.TaskJobTrigger;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:09 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FixedIntervalTrigger extends AbstractTrigger {
    /**
     * 固定速率触发，间隔时间(单位：秒)
     */
    private Long fixedInterval;

    public FixedIntervalTrigger(String name, Long fixedInterval) {
        Assert.hasText(name, "参数name不能为空");
        Assert.notNull(fixedInterval, "参数fixedInterval不能为空");
        Assert.isTrue(fixedInterval > 0, "参数fixedInterval值必须大于0");
        this.name = name;
        this.fixedInterval = fixedInterval;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TRIGGER_TYPE_2;
    }

    @Override
    public TaskJobTrigger toJobTrigger() {
        TaskJobTrigger jobTrigger = super.toJobTrigger();
        jobTrigger.setFixedInterval(getFixedInterval());
        return jobTrigger;
    }
}
