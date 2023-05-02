package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJobLog;
import org.clever.task.core.model.entity.TaskScheduler;

/**
 * 定时任务执行事件监听器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:49 <br/>
 */
public interface JobListener {
    /**
     * 开始执行
     */
    void onStartRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog);

    /**
     * 执行完成(成功或者失败)
     */
    void onEndRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog);

    /**
     * 重试执行
     */
    void onRetryRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog, Exception error);
}
