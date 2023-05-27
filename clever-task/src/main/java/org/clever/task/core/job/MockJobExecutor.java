package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.support.JobTriggerUtils;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/12 12:14 <br/>
 */
@Slf4j
public class MockJobExecutor implements JobExecutor {
    private final AtomicLong count = new AtomicLong(0);
    private final ConcurrentMap<Long, Long> lastTime = new ConcurrentHashMap<>(1000);

    @Override
    public boolean support(int jobType) {
        return true;
    }

    @Override
    public void exec(Date dbNow, TaskJob job, TaskScheduler scheduler, TaskStore taskStore) throws Exception {
        long newTime = taskStore.currentTimeMillis();
        long oldTime = lastTime.computeIfAbsent(job.getId(), id -> 0L);
        lastTime.put(job.getId(), newTime);
        long interval = newTime - oldTime;
        if (oldTime != 0L && interval >= 2000) {
            log.error("调度时间错误 | jobId={} | new={} | old={} | interval={}", job.getId(), newTime, oldTime, interval);
        }
        long second = JobTriggerUtils.getSecond(newTime);
//        log.info("#### ---> 模拟执行定时任务 | name={} | second={}", job.getName(), second);
//        Thread.sleep(5_000);
        if (count.incrementAndGet() % 100 == 0) {
            log.info("#### ---> 模拟执行定时任务 | name={} | second={}", job.getName(), second);
        }
    }
}
