package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJobTriggerLog;
import org.clever.task.core.model.entity.TaskScheduler;

/**
 * 触发器事件监听器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:45 <br/>
 */
public interface JobTriggerListener {
    /**
     * 触发成功
     */
    void onTriggered(TaskScheduler scheduler, TaskStore taskStore, TaskJobTriggerLog jobTriggerLog);
}
