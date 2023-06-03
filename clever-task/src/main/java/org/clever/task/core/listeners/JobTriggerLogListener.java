package org.clever.task.core.listeners;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJobTriggerLog;
import org.clever.task.core.model.entity.TaskScheduler;

/**
 * 定时任务触发日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:59 <br/>
 */
@Slf4j
public class JobTriggerLogListener implements JobTriggerListener {
    @Override
    public void onTriggered(TaskScheduler scheduler, TaskStore taskStore, TaskJobTriggerLog jobTriggerLog) {
        int count = taskStore.beginTX(status -> taskStore.addJobTriggerLog(jobTriggerLog));
        if (count <= 0) {
            log.error("触发器日志保存失败，jobTriggerLog={}", jobTriggerLog);
        }
    }
}
