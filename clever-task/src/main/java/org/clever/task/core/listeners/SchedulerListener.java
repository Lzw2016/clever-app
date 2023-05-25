package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.model.entity.TaskSchedulerLog;

/**
 * 调度器事件监听器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:45 <br/>
 */
public interface SchedulerListener {
    /**
     * 调度器启动完成
     */
    void onStarted(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog);

    /**
     * 调度器已暂停
     */
    void onPaused(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog);

    /**
     * 调度器已取消暂停(继续运行)
     */
    void onResume(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog);

    /**
     * 调度器已停止
     */
    void onStop(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog);

    /**
     * 调度器出现错误
     */
    void onErrorEvent(TaskScheduler scheduler, TaskStore taskStore, TaskSchedulerLog schedulerLog, Exception error);
}
