package org.clever.task.core.listeners;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.core.tuples.TupleTwo;
import org.clever.task.core.GlobalConstant;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJobLog;
import org.clever.task.core.model.entity.TaskScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 定时任务执行日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 21:02 <br/>
 */
@Slf4j
public class JobLogListener implements JobListener {
    // Queue<TupleTwo<TaskJobLog, type>> | type: 1-onStartRun, 2-onEndRun, 3-onRetryRun
    private final ConcurrentLinkedQueue<TupleTwo<TaskJobLog, Integer>> jobLogCache = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduled;
    private volatile TaskStore taskStore;

    public JobLogListener() {
        scheduled = new ScheduledThreadPoolExecutor(
                1,
                new BasicThreadFactory.Builder().namingPattern("task-log-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        scheduled.scheduleAtFixedRate(this::batchSaveLog, 100, GlobalConstant.SAVE_LOG_INTERVAL, TimeUnit.MILLISECONDS);
        AppShutdownHook.addShutdownHook(this::stop, OrderIncrement.NORMAL + 100, "停止定时任务Job日志记录");
    }

    @Override
    public void onStartRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog) {
        setTaskStore(taskStore);
        jobLog.setStartTime(taskStore.currentDate());
        jobLogCache.add(TupleTwo.creat(jobLog, 1));
    }

    @Override
    public void onEndRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog) {
        setTaskStore(taskStore);
        jobLog.setEndTime(taskStore.currentDate());
        jobLogCache.add(TupleTwo.creat(jobLog, 2));
    }

    @Override
    public void onRetryRun(TaskScheduler scheduler, TaskStore taskStore, TaskJobLog jobLog, Exception error) {
        setTaskStore(taskStore);
        jobLogCache.add(TupleTwo.creat(jobLog, 3));
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
        int initSize = jobLogCache.size() / 3 + 4;
        List<TaskJobLog> addJobLogs = new ArrayList<>(initSize);
        List<TaskJobLog> updateJobLogByEnd = new ArrayList<>(initSize);
        List<TaskJobLog> updateJobLogByRetry = new ArrayList<>(initSize);
        TupleTwo<TaskJobLog, Integer> tuple;
        while ((tuple = jobLogCache.poll()) != null) {
            switch (tuple.getValue2()) {
                case 1:
                    addJobLogs.add(tuple.getValue1());
                    break;
                case 2:
                    updateJobLogByEnd.add(tuple.getValue1());
                    break;
                case 3:
                    updateJobLogByRetry.add(tuple.getValue1());
                    break;
            }
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
                    taskStore.addJobLogs(addJobLogs);
                    taskStore.updateJobLogsByEnd(updateJobLogByEnd);
                    taskStore.updateJobLogByRetry(updateJobLogByRetry);
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
            log.error("任务执行日志保存失败", throwable);
        }
    }

    private void stop() {
        batchSaveLog();
        scheduled.shutdownNow();
    }
}
