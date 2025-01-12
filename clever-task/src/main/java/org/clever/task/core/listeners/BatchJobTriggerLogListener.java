package org.clever.task.core.listeners;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.task.core.GlobalConstant;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJobTriggerLog;
import org.clever.task.core.model.entity.TaskScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 定时任务触发日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:59 <br/>
 */
@Slf4j
public class BatchJobTriggerLogListener implements JobTriggerListener {
    private final ConcurrentLinkedQueue<TaskJobTriggerLog> triggerLogCache = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduled;
    private volatile TaskStore taskStore;

    public BatchJobTriggerLogListener() {
        scheduled = new ScheduledThreadPoolExecutor(
            1,
            new BasicThreadFactory.Builder().namingPattern("task-log-pool-%d").daemon(true).build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        scheduled.scheduleAtFixedRate(this::batchSaveLog, 600, GlobalConstant.SAVE_LOG_INTERVAL, TimeUnit.MILLISECONDS);
        AppShutdownHook.addShutdownHook(this::stop, OrderIncrement.NORMAL + 100, "停止定时任务Trigger日志记录");
    }

    @Override
    public void onTriggered(TaskScheduler scheduler, TaskStore taskStore, TaskJobTriggerLog jobTriggerLog) {
        setTaskStore(taskStore);
        jobTriggerLog.setCreateAt(taskStore.currentDate());
        triggerLogCache.add(jobTriggerLog);
    }

    private void setTaskStore(TaskStore taskStore) {
        if (this.taskStore == null) {
            this.taskStore = taskStore;
        }
    }

    private void batchSaveLog() {
        if (taskStore == null) {
            return;
        }
        List<TaskJobTriggerLog> addJobTriggerLogs = new ArrayList<>(triggerLogCache.size());
        TaskJobTriggerLog triggerLog;
        while ((triggerLog = triggerLogCache.poll()) != null) {
            addJobTriggerLogs.add(triggerLog);
        }
        int count = 0;
        Throwable throwable = null;
        do {
            if (count > 8) {
                break;
            }
            count++;
            try {
                taskStore.beginTX(status -> {
                    taskStore.addJobTriggerLogs(addJobTriggerLogs);
                    return null;
                });
                throwable = null;
            } catch (Throwable e) {
                throwable = e;
                try {
                    Thread.sleep(10L * count);
                } catch (InterruptedException ignored) {
                    Thread.yield();
                }
            }
        } while (throwable != null);
        if (throwable != null) {
            log.error("触发器日志保存失败", throwable);
        }
    }

    private void stop() {
        batchSaveLog();
        scheduled.shutdownNow();
    }
}
