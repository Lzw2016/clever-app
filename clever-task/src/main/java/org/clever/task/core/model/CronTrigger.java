package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.model.entity.TaskJobTrigger;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:08 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CronTrigger extends AbstractTrigger {
    /**
     * cron表达式
     */
    private String cron;

    public CronTrigger(String name, String cron) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(cron, "参数cron不能为空");
        Assert.isTrue(CronExpressionUtil.isValidExpression(cron), "参数cron格式错误");
        this.name = name;
        this.cron = cron;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TRIGGER_TYPE_1;
    }

    @Override
    public TaskJobTrigger toJobTrigger() {
        TaskJobTrigger jobTrigger = super.toJobTrigger();
        jobTrigger.setCron(getCron());
        return jobTrigger;
    }
}
