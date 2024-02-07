package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.support.JobTriggerUtils;

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
    public void exec(final JobContext context) throws Exception {
        final TaskJob job = context.getJob();
        final TaskStore taskStore = context.getTaskStore();
        long newTime = taskStore.currentTimeMillis();
        long oldTime = lastTime.computeIfAbsent(job.getId(), id -> 0L);
        lastTime.put(job.getId(), newTime);
        long interval = newTime - oldTime;
        if (oldTime != 0L && interval >= 2000) {
            log.error("调度时间错误 | jobId={} | new={} | old={} | interval={}", job.getId(), newTime, oldTime, interval);
        }
        long second = JobTriggerUtils.getSecond(newTime);
        log.info("#### ---> 模拟执行定时任务 | name={} | second={}", job.getName(), second);
        // Thread.sleep(5_000);
        // if (count.incrementAndGet() % 100 == 0) {
        //     log.info("#### ---> 模拟执行定时任务 | name={} | second={}", job.getName(), second);
        // }
    }
}
