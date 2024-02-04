package org.clever.task.core.support;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.ResourcePathUtils;
import org.clever.task.core.GlobalConstant;
import org.clever.task.core.TaskInstance;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.model.SchedulerRuntimeInfo;
import org.clever.task.core.model.entity.TaskJobTrigger;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 定时任务调度器上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:55 <br/>
 */
@Slf4j
public class TaskContext {
    /**
     * 全局基础路径
     */
    @Getter
    private final String rootPath;
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
     * 接下来(N+M)秒内需要触发的触发器列表 {@code ConcurrentMap<触发时间戳(秒), Linked<TaskJobTrigger>>}
     */
    private final ConcurrentMap<Long, ConcurrentLinkedQueue<TaskJobTrigger>> nextTriggers = new ConcurrentHashMap<>(
        GlobalConstant.NEXT_TRIGGER_N + GlobalConstant.NEXT_TRIGGER_M
    );
    /**
     * 当前节点任务运行的重入执行次数 {@code ConcurrentMap<jobId, jobReentryCount>}
     */
    private final ConcurrentMap<Long, AtomicInteger> jobReentryCount = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);
    /**
     * 当前节点触发器触发总次数 {@code ConcurrentMap<jobTriggerId, fireCount>}
     */
    private final ConcurrentMap<Long, AtomicLong> jobTriggerFireCount = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);
    /**
     * 当前节点任务运行的总次数 {@code ConcurrentMap<jobId, jobRunCount>}
     */
    private final ConcurrentMap<Long, AtomicLong> jobRunCount = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);

    public TaskContext(String rootPath, SchedulerConfig schedulerConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(schedulerConfig, "参数 schedulerConfig 不能为null");
        this.rootPath = rootPath;
        this.schedulerConfig = schedulerConfig;
    }

    public void addNextTrigger(TaskJobTrigger trigger) {
        if (trigger.getNextFireTime() != null) {
            long second = JobTriggerUtils.getSecond(trigger.getNextFireTime());
            ConcurrentLinkedQueue<TaskJobTrigger> triggers = nextTriggers.computeIfAbsent(second, time -> new ConcurrentLinkedQueue<>());
            triggers.add(trigger);
        }
    }

    public void removeNextTrigger(TaskJobTrigger trigger) {
        if (trigger == null) return;
        removeNextTrigger(trigger.getId());
    }

    public void removeNextTrigger(Long triggerId) {
        if (triggerId == null) return;
        for (ConcurrentLinkedQueue<TaskJobTrigger> triggers : nextTriggers.values()) {
            triggers.removeIf(jobTrigger -> Objects.equals(jobTrigger.getId(), triggerId));
        }
    }

    public Map<Long, ConcurrentLinkedQueue<TaskJobTrigger>> getNextTriggers(long second) {
        Map<Long, ConcurrentLinkedQueue<TaskJobTrigger>> triggerMap = new HashMap<>(4);
        Iterator<Map.Entry<Long, ConcurrentLinkedQueue<TaskJobTrigger>>> iterator = nextTriggers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ConcurrentLinkedQueue<TaskJobTrigger>> entry = iterator.next();
            long timeOfSecond = entry.getKey();
            ConcurrentLinkedQueue<TaskJobTrigger> triggers = entry.getValue();
            if (second >= timeOfSecond) {
                triggerMap.put(timeOfSecond, triggers);
            }
            iterator.remove();
        }
        return triggerMap;
    }

    public int getJobReentryCount(Long jobId) {
        return jobReentryCount.computeIfAbsent(jobId, id -> new AtomicInteger(0)).get();
    }

    public int getAndIncrementJobReentryCount(Long jobId) {
        return jobReentryCount.computeIfAbsent(jobId, id -> new AtomicInteger(0)).getAndIncrement();
    }

    public void decrementAndGetJobReentryCount(Long jobId) {
        AtomicInteger jobReentryCount = this.jobReentryCount.get(jobId);
        if (jobReentryCount != null) {
            jobReentryCount.decrementAndGet();
        }
    }

    public void removeJobReentryCount(Long jobId) {
        jobReentryCount.remove(jobId);
    }

    public long incrementAndGetJobFireCount(Long triggerId) {
        return jobTriggerFireCount.computeIfAbsent(triggerId, id -> new AtomicLong(0)).incrementAndGet();
    }

    public void decrementAndGetJobFireCount(Long triggerId) {
        AtomicLong jobFireCount = jobTriggerFireCount.get(triggerId);
        if (jobFireCount != null) {
            jobFireCount.decrementAndGet();
        }
    }

    public void removeJobFireCount(Long triggerId) {
        jobTriggerFireCount.remove(triggerId);
    }

    public long incrementAndGetJobRunCount(Long jobId) {
        return jobRunCount.computeIfAbsent(jobId, id -> new AtomicLong(0)).incrementAndGet();
    }

    public void removeJobRunCount(Long jobId) {
        jobRunCount.remove(jobId);
    }

    public List<TaskScheduler> getRunningSchedulerList() {
        if (availableSchedulerList == null) {
            return Collections.emptyList();
        }
        return availableSchedulerList.stream()
            .filter(scheduler -> {
                SchedulerRuntimeInfo runtimeInfo = scheduler.getSchedulerRuntimeInfo();
                return runtimeInfo != null && Objects.equals(runtimeInfo.getState(), TaskInstance.State.RUNNING);
            }).collect(Collectors.toList());
    }

    /**
     * shell任务的work目录
     */
    public String getShellJobWorkingDir() {
        return ResourcePathUtils.getAbsolutePath(rootPath, schedulerConfig.getShellJobWorkingDir());
    }
}
