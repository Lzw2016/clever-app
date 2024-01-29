package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.task.core.model.entity.TaskScheduler;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/11 20:09 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SchedulerInfo extends TaskScheduler {
    /**
     * 调度器状态
     */
    private Boolean available;
}
