package org.clever.task.core.support;

import lombok.Getter;
import lombok.Setter;
import org.clever.task.core.GlobalConstant;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.util.Assert;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 定时任务调度器上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:55 <br/>
 */
public class TaskContext {
    /**
     * 当前调度器配置
     */
    @Getter
    private final SchedulerConfig schedulerConfig;
    /**
     * 当前调度器信息
     */
    @Getter
    @Setter
    private volatile TaskScheduler currentScheduler;
    /**
     * 当前集群可用的调度器列表
     */
    @Getter
    @Setter
    private volatile List<TaskScheduler> availableSchedulerList;
    /**
     * 当前节点任务运行的重入执行次数 {@code ConcurrentMap<jobId, jobReentryCount>}
     */
    private final ConcurrentMap<Long, AtomicInteger> jobReentryCountMap = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);
    /**
     * 当前节点触发器触发总次数 {@code ConcurrentMap<jobTriggerId, fireCount>}
     */
    private final ConcurrentMap<Long, AtomicLong> jobTriggerFireCountMap = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);
    /**
     * 当前节点任务运行的总次数 {@code ConcurrentMap<jobId, jobRunCount>}
     */
    private final ConcurrentMap<Long, AtomicLong> jobRunCountMap = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);

    public TaskContext(SchedulerConfig schedulerConfig) {
        Assert.notNull(schedulerConfig, "参数 schedulerConfig 不能为null");
        this.schedulerConfig = schedulerConfig;
    }

    public int getJobReentryCount(Long jobId) {
        return jobReentryCountMap.computeIfAbsent(jobId, id -> new AtomicInteger(0)).get();
    }

    public int getAndIncrementJobReentryCount(Long jobId) {
        return jobReentryCountMap.computeIfAbsent(jobId, id -> new AtomicInteger(0)).getAndIncrement();
    }

    public void decrementAndGetJobReentryCount(Long jobId) {
        AtomicInteger jobReentryCount = jobReentryCountMap.get(jobId);
        if (jobReentryCount != null) {
            jobReentryCount.decrementAndGet();
        }
    }

    public void removeJobReentryCount(Long jobId) {
        jobReentryCountMap.remove(jobId);
    }

    public long incrementAndGetJobFireCount(Long jobTriggerId) {
        return jobTriggerFireCountMap.computeIfAbsent(jobTriggerId, id -> new AtomicLong(0)).incrementAndGet();
    }

    public void decrementAndGetJobFireCount(Long jobTriggerId) {
        AtomicLong jobFireCount = jobTriggerFireCountMap.get(jobTriggerId);
        if (jobFireCount != null) {
            jobFireCount.decrementAndGet();
        }
    }

    public void removeJobFireCount(Long jobTriggerId) {
        jobTriggerFireCountMap.remove(jobTriggerId);
    }

    public long incrementAndGetJobRunCount(Long jobId) {
        return jobRunCountMap.computeIfAbsent(jobId, id -> new AtomicLong(0)).incrementAndGet();
    }

    public void removeJobRunCount(Long jobId) {
        jobRunCountMap.remove(jobId);
    }
}
