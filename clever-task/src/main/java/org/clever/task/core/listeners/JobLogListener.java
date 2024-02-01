package org.clever.task.core.listeners;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJobLog;
import org.clever.task.core.model.entity.TaskScheduler;

/**
 * 定时任务执行日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 21:02 <br/>
 */
@Slf4j
public class JobLogListener implements JobListener {
    @Override
    public void onStartRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog) {
        long count = taskStore.newBeginTX(status -> taskStore.addJobLog(jobLog));
        if (count <= 0) {
            log.error("任务执行日志保存失败[onStartRun]，jobLog={}", jobLog);
        }
    }

    @Override
    public void onEndRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog) {
        long count = taskStore.newBeginTX(status -> taskStore.updateJobLogByEnd(jobLog));
        if (count <= 0) {
            log.error("任务执行日志保存失败[onEndRun]，jobLog={}", jobLog);
        }
    }

    @Override
    public void onRetryRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog, Exception error) {
        long count = taskStore.newBeginTX(status -> taskStore.updateJobLogByRetry(jobLog));
        if (count <= 0) {
            log.error("任务执行日志保存失败[onRetryRun]，jobLog={}", jobLog);
        }
    }
}
