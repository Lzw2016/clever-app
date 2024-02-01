package org.clever.task.core.listeners;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.model.entity.TaskSchedulerLog;

/**
 * 调度器事件日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 21:01 <br/>
 */
@Slf4j
public class SchedulerLogListener implements SchedulerListener {
    @Override
    public void onStarted(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    @Override
    public void onPaused(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    @Override
    public void onResume(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    @Override
    public void onStop(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    @Override
    public void onErrorEvent(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog, Exception error) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    private void saveSchedulerLog(TaskStore taskStore, TaskSchedulerLog schedulerLog) {
        long count = taskStore.newBeginTX(status -> taskStore.addSchedulerLog(schedulerLog));
        if (count <= 0) {
            log.error("调度器日志保存失败，schedulerLog={}", schedulerLog);
        }
    }
}
