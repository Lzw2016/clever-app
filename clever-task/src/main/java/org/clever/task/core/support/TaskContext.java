package org.clever.task.core.support;

import lombok.Getter;
import lombok.Setter;
import org.clever.task.core.GlobalConstant;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.model.entity.TaskJobTrigger;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
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
    private TaskScheduler currentScheduler;
    /**
     * 当前集群可用的调度器列表
     */
    @Getter
    @Setter
    private volatile List<TaskScheduler> availableSchedulerList;
    /**
     * 接下来N秒内需要触发的触发器列表(N = heartbeatInterval * NEXT_TRIGGER_INTERVAL) {@code ConcurrentMap<jobTriggerId, JobTrigger>}
     */
    private final LinkedHashMap<Long, TaskJobTrigger> nextJobTriggerMap = new LinkedHashMap<>(GlobalConstant.INITIAL_CAPACITY);
    /**
     * 正在触发的触发器ID {@code Set<jobTriggerId + nextFireTime>}
     */
    private final Set<String> triggeringSet = ConcurrentHashMap.newKeySet();
    /**
     * 当前节点任务运行的重入执行次数 {@code ConcurrentMap<jobId, jobReentryCount>}
     */
    private final ConcurrentMap<Long, AtomicInteger> jobReentryCountMap = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);
    /**
     * 触发器最后一次触发的时间 {@code ConcurrentMap<jobTriggerId, 时间戳>}
     */
    private final ConcurrentMap<Long, Long> jobLastTriggerFireTimeMap = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);
    /**
     * 当前节点触发器触发次数计数 {@code ConcurrentMap<jobTriggerId, fireCount>}
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

    public void setNextJobTriggerMap(List<TaskJobTrigger> nextJobTriggerList) {
        synchronized (nextJobTriggerMap) {
            nextJobTriggerMap.clear();
            nextJobTriggerList.forEach(jobTrigger -> nextJobTriggerMap.put(jobTrigger.getId(), jobTrigger));
        }
    }

    public List<TaskJobTrigger> getNextJobTriggerList() {
        synchronized (nextJobTriggerMap) {
            return new ArrayList<>(nextJobTriggerMap.values());
        }
    }

    public void removeNextJobTrigger(Long jobTriggerId) {
        synchronized (nextJobTriggerMap) {
            nextJobTriggerMap.remove(jobTriggerId);
        }
    }

    public void putNextJobTrigger(TaskJobTrigger jobTrigger) {
        synchronized (nextJobTriggerMap) {
            nextJobTriggerMap.put(jobTrigger.getId(), jobTrigger);
        }
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

    public boolean addTriggering(TaskJobTrigger jobTrigger) {
        return triggeringSet.add(String.format("%s_%s", jobTrigger.getId(), jobTrigger.getNextFireTime().getTime()));
    }

    public void removeTriggering(TaskJobTrigger jobTrigger) {
        triggeringSet.remove(String.format("%s_%s", jobTrigger.getId(), jobTrigger.getNextFireTime().getTime()));
    }

    public void setLastTriggerFireTime(Long jobTriggerId, Long lastFireTime) {
        jobLastTriggerFireTimeMap.put(jobTriggerId, lastFireTime);
    }

    public long getLastTriggerFireTime(Long jobTriggerId) {
        Long time = jobLastTriggerFireTimeMap.get(jobTriggerId);
        return time == null ? 0 : time;
    }

    public void removeLastTriggerFireTime(Long jobTriggerId) {
        jobLastTriggerFireTimeMap.remove(jobTriggerId);
    }
}
