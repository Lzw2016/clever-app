package org.clever.task.core;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.data.redis.Redis;
import org.clever.data.redis.config.RedisProperties;
import org.clever.task.core.job.JobExecutor;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.support.JobTriggerUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/27 13:24 <br/>
 */
@Slf4j
public class RedisMockJobExecutor implements JobExecutor {
    private final AtomicLong count = new AtomicLong(0);
    public final Redis redis;

    public RedisMockJobExecutor() {
        RedisProperties properties = new RedisProperties();
        properties.setMode(RedisProperties.Mode.Standalone);
        properties.setClientName("test");
        properties.getStandalone().setHost("192.168.1.201");
        properties.getStandalone().setPort(30007);
        properties.getStandalone().setDatabase(0);
        properties.getStandalone().setPassword("admin123456");
        properties.getPool().setMaxActive(30000);
        this.redis = new Redis("test", properties);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            this.redis.close();
        }));
        this.redis.kDelete(redis.keys("task-test:job_*"));
    }

    @Override
    public boolean support(int jobType) {
        return true;
    }

    @Override
    public void exec(Date dbNow, TaskJob job, TaskScheduler scheduler, TaskStore taskStore) {
        long newTime = taskStore.currentTimeMillis();
        String key = String.format("task-test:job_%s", job.getId());
        Long oldTime = Conv.asLong(redis.vGet(key, String.class), 0L);
        redis.vSet(key, newTime);
        if (oldTime != null) {
            long interval = newTime - oldTime;
            if(interval >= 2000) {
                log.error("调度时间错误 | jobId={} | new={} | old={} | interval={}", job.getId(), newTime, oldTime, interval);
            }
        }
        long second = JobTriggerUtils.getSecond(newTime);
//        log.info("#### ---> 模拟执行定时任务 | name={} | second={}", job.getName(), second);
        if (count.incrementAndGet() % 100 == 0) {
            log.info("#### ---> 模拟执行定时任务 | name={} | second={}", job.getName(), second);
        }
    }
}
