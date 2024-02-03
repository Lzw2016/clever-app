package org.clever.task.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.core.DateUtils;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.function.ThreeConsumer;
import org.clever.core.id.SnowFlake;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.thread.ThreadUtils;
import org.clever.core.tuples.TupleOne;
import org.clever.data.jdbc.QueryDSL;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.job.JobContext;
import org.clever.task.core.job.JobExecutor;
import org.clever.task.core.listeners.JobListener;
import org.clever.task.core.listeners.JobTriggerListener;
import org.clever.task.core.listeners.SchedulerListener;
import org.clever.task.core.model.*;
import org.clever.task.core.model.entity.*;
import org.clever.task.core.support.JobTriggerUtils;
import org.clever.task.core.support.TaskContext;
import org.clever.util.Assert;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 定时任务调度器实例 <br />
 * 1.三种实体: 调度器、任务、触发器 <br />
 * 2.调度器: 通过 namespace 隔离，同一个 namespace 下可以有多个调度器组成一个集群 <br />
 * 3.任务&触发器: 任务与触发器是一对一的关系(表结构体现的是一个任务可以有多个触发器，目前程序设计的是一对一的关系) <br />
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:38 <br/>
 */
@Slf4j
public class TaskInstance {
    public interface State {
        int INIT = 0;
        int RUNNING = 1;
        int PAUSED = 2;
        int STOPPED = 3;
    }

    private static final AtomicIntegerFieldUpdater<TaskInstance> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(TaskInstance.class, "state");

    /**
     * 定时任务执行器实现列表
     */
    private final List<JobExecutor> jobExecutors;
    /**
     * 调度器事件监听器列表
     */
    private final List<SchedulerListener> schedulerListeners;
    /**
     * 触发器事件监听器列表
     */
    private final List<JobTriggerListener> jobTriggerListeners;
    /**
     * 定时任务执行事件监听器列表
     */
    private final List<JobListener> jobListeners;
    /**
     * 调度器数据存储对象
     * -- GETTER --
     * 获取调度器 TaskStore
     */
    @Getter
    private final TaskStore taskStore;
    /**
     * 调度器上下文
     */
    private final TaskContext taskContext;
    /**
     * 内部的调度器(守护线程池)
     */
    private final ScheduledExecutorService scheduledExecutor;
    /**
     * 执行定时任务线程池
     */
    private final ExecutorService jobExecutor;
    /**
     * 调度器节点注册&更新调度器运行信息
     */
    private ScheduledFuture<?> registerSchedulerFuture;
    /**
     * 集群节点心跳保持
     */
    private ScheduledFuture<?> heartbeatFuture;
    /**
     * 数据完整性校验&一致性校验
     */
    private ScheduledFuture<?> dataCheckFuture;
    /**
     * 维护当前集群可用的调度器列表
     */
    private ScheduledFuture<?> reloadSchedulerFuture;
    /**
     * 触发接下来(N+M)秒内需要触发的触发器
     */
    private Future<?> fireTriggerFuture;
    /**
     * 执行调度器指令
     */
    private ScheduledFuture<?> execSchedulerCmdFuture;
    /**
     * 清理日志数据
     */
    private ScheduledFuture<?> clearLogFuture;
    /**
     * 收集任务执行报表
     */
    private ScheduledFuture<?> collectReportFuture;
    /**
     * 调度器状态
     */
    private volatile int state = State.INIT;

    /**
     * 创建定时任务调度器实例
     *
     * @param queryDSL            QueryDSL数据源
     * @param schedulerConfig     调度器配置
     * @param jobExecutors        定时任务执行器实现列表
     * @param schedulerListeners  调度器事件监听器列表
     * @param jobTriggerListeners 触发器事件监听器列表
     * @param jobListeners        定时任务执行事件监听器列表
     */
    public TaskInstance(
        QueryDSL queryDSL,
        SchedulerConfig schedulerConfig,
        List<JobExecutor> jobExecutors,
        List<SchedulerListener> schedulerListeners,
        List<JobTriggerListener> jobTriggerListeners,
        List<JobListener> jobListeners) {
        Assert.notNull(queryDSL, "参数 queryDSL 不能为空");
        Assert.notNull(schedulerConfig, "参数 schedulerConfig 不能为空");
        Assert.notEmpty(jobExecutors, "参数 jobExecutors 不能为空");
        Assert.notEmpty(schedulerListeners, "参数 schedulerListeners 不能为空");
        Assert.notEmpty(jobTriggerListeners, "参数 jobTriggerListeners 不能为空");
        Assert.notEmpty(jobListeners, "参数 jobListeners 不能为空");
        jobExecutors.sort(Comparator.comparingInt(JobExecutor::order));
        this.jobExecutors = jobExecutors;
        if (this.jobExecutors.isEmpty()) {
            log.error("[TaskInstance] 定时任务执行器实现列表为空 | instanceName={}", schedulerConfig.getInstanceName());
        }
        log.info(
            "[TaskInstance] 定时任务执行器={} | instanceName={}",
            StringUtils.join(this.jobExecutors.stream().map(exec -> exec.getClass().getSimpleName()).collect(Collectors.toList()), ", "),
            schedulerConfig.getInstanceName()
        );
        this.schedulerListeners = schedulerListeners;
        this.jobTriggerListeners = jobTriggerListeners;
        this.jobListeners = jobListeners;
        final SnowFlake snowFlake = new SnowFlake(
            Math.abs(schedulerConfig.getInstanceName().hashCode() % SnowFlake.MAX_WORKER_ID),
            Math.abs(schedulerConfig.getNamespace().hashCode() % SnowFlake.MAX_DATACENTER_ID)
        );
        this.taskStore = new TaskStore(snowFlake, queryDSL);
        this.taskContext = new TaskContext(schedulerConfig);
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(
            schedulerConfig.getSchedulerExecutorPoolSize(),
            new BasicThreadFactory.Builder().namingPattern("task-scheduler-pool-%d").daemon(true).build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.jobExecutor = new ThreadPoolExecutor(
            schedulerConfig.getJobExecutorPoolSize(),
            schedulerConfig.getJobExecutorPoolSize(),
            GlobalConstant.THREAD_POOL_KEEP_ALIVE_SECONDS,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(schedulerConfig.getJobExecutorQueueSize()),
            new BasicThreadFactory.Builder().namingPattern("task-worker-pool-%d").daemon(true).build(),
            new ThreadPoolExecutor.AbortPolicy()
        );
        // 初始化 TaskContext
        this.taskContext.setCurrentScheduler(toScheduler(schedulerConfig));
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- api

    /**
     * 直接进入 paused 状态(无须先 start 再 paused)
     */
    public void standby() {
        STATE_UPDATER.getAndUpdate(this, state -> {
            switch (state) {
                case State.INIT:
                    doStandby();
                    break;
                case State.RUNNING:
                    doPause();
                    break;
                case State.PAUSED:
                    break;
                default:
                    throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", getStateText()));
            }
            return State.PAUSED;
        });
    }

    /**
     * 启动调度器，只有当前为 {@link State#INIT} 状态才能调用
     */
    public void start() {
        STATE_UPDATER.getAndUpdate(this, state -> {
            if (state == State.INIT) {
                doStart();
            } else {
                throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", getStateText()));
            }
            return State.RUNNING;
        });
    }

    /**
     * 异步延时启动调度器
     *
     * @param seconds 延时时间(单位：秒)
     */
    public void startDelayed(int seconds) {
        Assert.isTrue(seconds >= 0, "参数 seconds 必须 >=0");
        scheduledExecutor.schedule(this::start, seconds, TimeUnit.SECONDS);
    }

    /**
     * 暂停调度器，只有当前为 {@link State#RUNNING} 状态才能调用
     */
    public void paused() {
        STATE_UPDATER.getAndUpdate(this, state -> {
            if (state == State.RUNNING) {
                doPause();
            } else {
                throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", getStateText()));
            }
            return State.PAUSED;
        });
    }

    /**
     * 继续运行调度器，只有当前为 {@link State#PAUSED} 状态才能调用
     */
    public void resume() {
        STATE_UPDATER.getAndUpdate(this, state -> {
            if (state == State.PAUSED) {
                doResume();
            } else {
                throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", getStateText()));
            }
            return State.RUNNING;
        });
    }

    /**
     * 停止调度器，只有当前为 {@link State#RUNNING}、{@link State#PAUSED} 状态才能调用
     */
    public void stop() {
        STATE_UPDATER.getAndUpdate(this, state -> {
            if (state == State.RUNNING || state == State.PAUSED) {
                doStop();
            } else {
                throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", getStateText()));
            }
            return State.STOPPED;
        });
    }

    /**
     * 当前集群 namespace
     */
    public String getNamespace() {
        String namespace = null;
        if (taskContext.getCurrentScheduler() != null) {
            namespace = taskContext.getCurrentScheduler().getNamespace();
        }
        if (namespace == null) {
            namespace = taskContext.getSchedulerConfig().getNamespace();
        }
        return namespace;
    }

    /**
     * 当前调度器实例名
     */
    public String getInstanceName() {
        String instanceName = null;
        if (taskContext.getCurrentScheduler() != null) {
            instanceName = taskContext.getCurrentScheduler().getInstanceName();
        }
        if (instanceName == null) {
            instanceName = taskContext.getSchedulerConfig().getInstanceName();
        }
        return instanceName;
    }

    /**
     * 调度器上下文
     */
    public TaskContext getContext() {
        return taskContext;
    }

    /**
     * 当前调度器状态 {@link State}
     */
    public int getState() {
        return STATE_UPDATER.get(this);
    }

    /**
     * 当前调度器状态 {@link State}
     */
    public String getStateText() {
        int state = getState();
        switch (state) {
            case State.INIT:
                return "init";
            case State.RUNNING:
                return "running";
            case State.PAUSED:
                return "paused";
            case State.STOPPED:
                return "stopped";
            default:
                throw new SchedulerException(String.format("无效的调度器状态：%s", state));
        }
    }

    /**
     * 新增或更新触发器，基于 triggerId 判断是更新还是新增
     *
     * @param triggerId 触发器ID(可为null)
     * @param trigger   触发器
     * @param jobId     任务ID
     * @param namespace 命名空间
     */
    protected TaskJobTrigger addOrUpdateTrigger(Long triggerId, AbstractTrigger trigger, long jobId, String namespace) {
        Assert.notNull(trigger, "参数 trigger 不能为 null");
        Assert.isNotBlank(namespace, "参数 namespace 不能为空");
        final TaskJobTrigger jobTrigger = trigger.toJobTrigger();
        jobTrigger.setNamespace(namespace);
        jobTrigger.setJobId(jobId);
        return taskStore.beginTX(status -> {
            final TaskJobTrigger existsTrigger = triggerId != null ? taskStore.getTrigger(triggerId) : null;
            final boolean exists = existsTrigger != null;
            long count;
            final Date dbNow = taskStore.currentDate();
            if (jobTrigger.getStartTime() == null || jobTrigger.getStartTime().compareTo(dbNow) < 0) {
                jobTrigger.setStartTime(JobTriggerUtils.removeMillisecond(dbNow));
            }
            final Date nextFireTime = JobTriggerUtils.getNextFireTime(jobTrigger);
            jobTrigger.setNextFireTime(nextFireTime);
            if (exists) {
                count = taskStore.updateTrigger(jobTrigger) ? 1 : 0;
            } else {
                count = taskStore.addTrigger(jobTrigger);
            }
            Assert.isTrue(count > 0, "更新 Trigger 失败");
            return jobTrigger;
        });
    }

    /**
     * 新增或更新定时任务，基于 jobId 判断是更新还是新增
     *
     * @param jobId     任务ID(可为null)
     * @param jobModel  任务配置
     * @param trigger   触发器配置(可为null,表示不新增或更新触发器)
     * @param namespace 命名空间
     * @param callback  自定义更新Job回调
     */
    protected JobInfo doAddOrUpdateJob(Long jobId, AbstractJob jobModel, AbstractTrigger trigger, String namespace, ThreeConsumer<JobInfo, TaskJob, Boolean> callback) {
        return taskStore.beginTX(status -> {
            final JobInfo info = new JobInfo();
            final TaskJob existsJob = jobId != null ? taskStore.getJob(jobId) : null;
            final boolean exists = existsJob != null;
            long count;
            // 更新job
            final TaskJob job = jobModel.toJob();
            job.setId(jobId);
            job.setNamespace(namespace);
            if (exists) {
                count = taskStore.updateJob(job) ? 1 : 0;
            } else {
                count = taskStore.addJob(job);
            }
            Assert.isTrue(count > 0, "更新 Job 失败");
            info.setJob(job);
            // 更新子job
            callback.call(info, job, exists);
            // 更新trigger
            if (trigger != null) {
                Long triggerId = jobId != null ? taskStore.getTriggerId(jobId) : null;
                TaskJobTrigger jobTrigger = addOrUpdateTrigger(triggerId, trigger, job.getId(), namespace);
                info.setJobTrigger(jobTrigger);
            }
            return info;
        });
    }

    /**
     * 新增或更新 http 定时任务，基于 jobId 判断是更新还是新增
     *
     * @param jobId     任务ID(可为null)
     * @param httpJob   http任务配置
     * @param trigger   触发器配置(可为null,表示不新增或更新触发器)
     * @param namespace 命名空间
     */
    protected JobInfo addOrUpdateHttpJob(Long jobId, HttpJobModel httpJob, AbstractTrigger trigger, String namespace) {
        Assert.notNull(httpJob, "参数 httpJob 不能为 null");
        Assert.isNotBlank(namespace, "参数 namespace 不能为空");
        return doAddOrUpdateJob(jobId, httpJob, trigger, namespace, (info, job, exists) -> {
            final TaskHttpJob http = httpJob.toJobEntity();
            http.setNamespace(namespace);
            http.setJobId(job.getId());
            long count;
            if (exists) {
                count = taskStore.updateHttpJob(http) ? 1 : 0;
            } else {
                count = taskStore.addHttpJob(http);
            }
            Assert.isTrue(count > 0, "更新 HttpJob 失败");
            info.setHttpJob(http);
        });
    }

    /**
     * 新增或更新 java 定时任务，基于 jobId 判断是更新还是新增
     *
     * @param jobId     任务ID(可为null)
     * @param javaJob   java任务配置
     * @param trigger   触发器配置(可为null,表示不新增或更新触发器)
     * @param namespace 命名空间
     */
    protected JobInfo addOrUpdateJavaJob(Long jobId, JavaJobModel javaJob, AbstractTrigger trigger, String namespace) {
        Assert.notNull(javaJob, "参数 javaJob 不能为 null");
        Assert.isNotBlank(namespace, "参数 namespace 不能为空");
        return doAddOrUpdateJob(jobId, javaJob, trigger, namespace, (info, job, exists) -> {
            final TaskJavaJob java = javaJob.toJobEntity();
            java.setNamespace(namespace);
            java.setJobId(job.getId());
            long count;
            if (exists) {
                count = taskStore.updateJavaJob(java) ? 1 : 0;
            } else {
                count = taskStore.addJavaJob(java);
            }
            Assert.isTrue(count > 0, "更新 JavaJob 失败");
            info.setJavaJob(java);
        });
    }

    /**
     * 新增或更新 js 定时任务，基于 jobId 判断是更新还是新增
     *
     * @param jobId     任务ID(可为null)
     * @param jsJob     js任务配置
     * @param trigger   触发器配置(可为null,表示不新增或更新触发器)
     * @param namespace 命名空间
     */
    protected JobInfo addOrUpdateJsJob(Long jobId, JsJobModel jsJob, AbstractTrigger trigger, String namespace) {
        Assert.notNull(jsJob, "参数 jsJob 不能为 null");
        Assert.isNotBlank(namespace, "参数 namespace 不能为空");
        return doAddOrUpdateJob(jobId, jsJob, trigger, namespace, (info, job, exists) -> {
            final TaskJsJob js = jsJob.toJobEntity();
            js.setNamespace(namespace);
            js.setJobId(job.getId());
            long count;
            if (exists) {
                count = taskStore.updateJsJob(js) ? 1 : 0;
            } else {
                count = taskStore.addJsJob(js);
            }
            Assert.isTrue(count > 0, "更新 JsJob 失败");
            info.setJsJob(js);
        });
    }

    /**
     * 新增或更新 shell 定时任务，基于 jobId 判断是更新还是新增
     */
    protected JobInfo addOrUpdateShellJob(Long jobId, ShellJobModel shellJob, AbstractTrigger trigger, String namespace) {
        Assert.notNull(shellJob, "参数 shellJob 不能为 null");
        Assert.isNotBlank(namespace, "参数 namespace 不能为空");
        return doAddOrUpdateJob(jobId, shellJob, trigger, namespace, (info, job, exists) -> {
            final TaskShellJob shell = shellJob.toJobEntity();
            shell.setNamespace(namespace);
            shell.setJobId(job.getId());
            long count;
            if (exists) {
                count = taskStore.updateShellJob(shell) ? 1 : 0;
            } else {
                count = taskStore.addShellJob(shell);
            }
            Assert.isTrue(count > 0, "更新 JsJob 失败");
            info.setShellJob(shell);
        });
    }

    /**
     * 新增或更新定时任务，基于 jobId 判断是更新还是新增
     *
     * @param jobId     任务ID(可为null)
     * @param jobModel  任务配置
     * @param trigger   触发器配置
     * @param namespace 命名空间
     */
    public JobInfo addOrUpdateJob(Long jobId, AbstractJob jobModel, AbstractTrigger trigger, String namespace) {
        Assert.notNull(trigger, "参数 trigger 不能为 null");
        if (jobModel instanceof HttpJobModel) {
            return addOrUpdateHttpJob(jobId, (HttpJobModel) jobModel, trigger, namespace);
        } else if (jobModel instanceof JavaJobModel) {
            return addOrUpdateJavaJob(jobId, (JavaJobModel) jobModel, trigger, namespace);
        } else if (jobModel instanceof JsJobModel) {
            return addOrUpdateJsJob(jobId, (JsJobModel) jobModel, trigger, namespace);
        } else if (jobModel instanceof ShellJobModel) {
            return addOrUpdateShellJob(jobId, (ShellJobModel) jobModel, trigger, namespace);
        } else {
            throw new IllegalArgumentException("不支持的任务类型:" + jobModel.getClass().getName());
        }
    }

    /**
     * 新增或更新定时任务，基于 jobId 判断是更新还是新增
     *
     * @param jobId    任务ID(可为null)
     * @param jobModel 任务配置
     * @param trigger  触发器配置
     */
    public JobInfo addOrUpdateJob(Long jobId, AbstractJob jobModel, AbstractTrigger trigger) {
        return addOrUpdateJob(jobId, jobModel, trigger, getNamespace());
    }

    /**
     * 新增定时任务
     *
     * @param jobModel  任务配置
     * @param trigger   触发器配置
     * @param namespace 命名空间
     */
    public JobInfo addJob(AbstractJob jobModel, AbstractTrigger trigger, String namespace) {
        return addOrUpdateJob(null, jobModel, trigger, namespace);
    }

    /**
     * 新增定时任务
     *
     * @param jobModel 任务配置
     * @param trigger  触发器配置
     */
    public JobInfo addJob(AbstractJob jobModel, AbstractTrigger trigger) {
        return addOrUpdateJob(null, jobModel, trigger, getNamespace());
    }

    /**
     * 禁用定时任务
     */
    public long disableJob(Long jobId) {
        Assert.notNull(jobId, "参数jobId不能为空");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(EnumConstant.JOB_DISABLE_1, jobId));
    }

    /**
     * 批量禁用定时任务
     */
    public long disableJobs(Collection<Long> jobIds) {
        Assert.notEmpty(jobIds, "参数jobIds不能为空");
        Assert.noNullElements(jobIds, "参数jobIds含有空jobId");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(EnumConstant.JOB_DISABLE_1, jobIds.toArray(new Long[0])));
    }

    /**
     * 启用定时任务
     */
    public long enableJob(Long jobId) {
        Assert.notNull(jobId, "参数jobId不能为空");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(EnumConstant.JOB_DISABLE_0, jobId));
    }

    /**
     * 批量启用定时任务
     */
    public long enableJobs(Collection<Long> jobIds) {
        Assert.notEmpty(jobIds, "参数jobIds不能为空");
        Assert.noNullElements(jobIds, "参数jobIds含有空jobId");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(EnumConstant.JOB_DISABLE_0, jobIds.toArray(new Long[0])));
    }

    /**
     * 禁用触发器
     */
    public long disableTrigger(Long triggerId) {
        long count = taskStore.beginTX(status -> taskStore.updateDisableTrigger(EnumConstant.JOB_TRIGGER_DISABLE_1, triggerId));
        if (count > 0) {
            taskContext.removeNextTrigger(triggerId);
        }
        return count;
    }

    /**
     * 批量禁用触发器
     */
    public long disableTriggers(Collection<Long> triggerIds) {
        Assert.notEmpty(triggerIds, "参数triggerIds不能为空");
        Assert.noNullElements(triggerIds, "参数triggerIds含有空triggerId");
        long count = taskStore.beginTX(status -> taskStore.updateDisableTrigger(EnumConstant.JOB_TRIGGER_DISABLE_1, triggerIds.toArray(new Long[0])));
        if (count > 0) {
            triggerIds.forEach(taskContext::removeNextTrigger);
        }
        return count;
    }

    /**
     * 启用触发器
     */
    public long enableTrigger(Long triggerId) {
        return taskStore.beginTX(status -> taskStore.updateDisableTrigger(EnumConstant.JOB_TRIGGER_DISABLE_0, triggerId));
    }

    /**
     * 批量启用触发器
     */
    public long enableTriggers(Collection<Long> triggerIds) {
        Assert.notEmpty(triggerIds, "参数jobIds不能为空");
        Assert.noNullElements(triggerIds, "参数triggerIds含有空triggerId");
        return taskStore.beginTX(status -> taskStore.updateDisableTrigger(EnumConstant.JOB_TRIGGER_DISABLE_0, triggerIds.toArray(new Long[0])));
    }

    /**
     * 删除定时任务(同时删除触发器)
     */
    public JobInfo deleteJob(Long jobId) {
        Assert.notNull(jobId, "参数jobId不能为空");
        JobInfo res = taskStore.beginReadOnlyTX(status -> taskStore.getJobInfo(jobId));
        Assert.notNull(res, "定时任务不存在");
        TaskJob job = res.getJob();
        Assert.notNull(job, "定时任务不存在");
        TupleOne<Long> triggerId = TupleOne.creat(null);
        taskStore.beginTX(status -> {
            taskStore.delJobByJobId(jobId);
            triggerId.setValue1(taskStore.getTriggerId(jobId));
            taskStore.delTriggerByJobId(jobId);
            switch (job.getType()) {
                case EnumConstant.JOB_TYPE_1:
                    taskStore.delHttpJobByJobId(jobId);
                    break;
                case EnumConstant.JOB_TYPE_2:
                    taskStore.delJavaJobByJobId(jobId);
                    break;
                case EnumConstant.JOB_TYPE_3:
                    taskStore.delJsJobByJobId(jobId);
                    break;
                case EnumConstant.JOB_TYPE_4:
                    taskStore.delShellJobByJobId(jobId);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的任务类型: Type=" + job.getType());
            }
            return null;
        });
        taskContext.removeJobReentryCount(jobId);
        taskContext.removeJobRunCount(jobId);
        taskContext.removeJobFireCount(triggerId.getValue1());
        return res;
    }

    /**
     * 批量删除定时任务(同时删除触发器)
     */
    public void deleteJobs(Collection<Long> jobIds) {
        Assert.notEmpty(jobIds, "参数jobIds不能为空");
        Assert.noNullElements(jobIds, "参数jobIds含有空jobId");
        taskStore.beginTX(status -> {
            jobIds.forEach(this::deleteJob);
            return null;
        });
    }

    /**
     * 立即执行定时任务
     */
    public void execJob(Long jobId) {
        Assert.notNull(jobId, "参数jobId不能为空");
        final long startTime = taskStore.currentTimeMillis();
        final TaskJob job = taskStore.beginReadOnlyTX(status -> taskStore.getJob(getNamespace(), jobId));
        Assert.notNull(job, "job不存在");
        jobExecutor.execute(() -> {
            final Date dbNow = taskStore.currentDate();
            // 记录触发器日志
            final TaskJobTriggerLog jobTriggerLog = newJobTriggerLog(dbNow, jobId);
            scheduledExecutor.execute(() -> {
                final long endTime = taskStore.currentTimeMillis();
                jobTriggerLog.setTriggerTime((int) (endTime - startTime));
                jobTriggeredListener(jobTriggerLog);
            });
            // 执行任务
            final TaskJobLog jobLog = newJobLog(dbNow, job, null, jobTriggerLog.getId());
            final String oldJobData = job.getJobData();
            // 控制并发执行 - 是否允许多节点并发执行
            final boolean allowConcurrent = Objects.equals(EnumConstant.JOB_ALLOW_CONCURRENT_1, job.getAllowConcurrent());
            if (allowConcurrent) {
                executeJob(dbNow, job, jobLog);
            } else {
                try {
                    // 获取定时任务分布式锁 - 判断是否被其他节点执行了
                    taskStore.getLockJob(job.getNamespace(), job.getId(), () -> {
                        // 二次校验数据
                        Long runCount = taskStore.beginReadOnlyTX(status -> taskStore.getJobRunCount(getNamespace(), jobId));
                        if (Objects.equals(runCount, job.getRunCount())) {
                            executeJob(dbNow, job, jobLog);
                        }
                    });
                } catch (Exception e) {
                    log.error("[TaskInstance] 手动执行Job失败 | id={} | name={} | instanceName={}", job.getId(), job.getName(), getInstanceName(), e);
                    jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_1);
                    jobLog.setExceptionInfo(ExceptionUtils.getStackTraceAsString(e));
                    jobEndRunListener(jobLog);
                }
            }
            // 更新任务数据
            final String newJobData = job.getJobData();
            if (!Objects.equals(oldJobData, newJobData) && Objects.equals(job.getIsUpdateData(), EnumConstant.JOB_IS_UPDATE_DATA_1)) {
                taskStore.beginTX(status -> taskStore.updateJodData(job.getNamespace(), job.getId(), newJobData));
            }
        });
    }

    /**
     * 批量立即执行定时任务
     */
    public void execJobs(Collection<Long> jobIds) {
        Assert.notEmpty(jobIds, "参数jobIds不能为空");
        Assert.noNullElements(jobIds, "参数jobIds含有空jobId");
        taskStore.beginTX(status -> {
            jobIds.forEach(this::execJob);
            return null;
        });
    }

    /**
     * 获取所有调度器
     */
    public List<SchedulerInfo> allSchedulers() {
        return taskStore.beginReadOnlyTX(status -> taskStore.queryAllSchedulerList(null));
    }

    /**
     * 获取所有的 “命名空间”
     */
    public List<String> allNamespace() {
        return taskStore.beginReadOnlyTX(status -> taskStore.allNamespace());
    }

    /**
     * 获取所有的 “实例名”
     */
    public List<String> allInstance() {
        return taskStore.beginReadOnlyTX(status -> taskStore.allInstance());
    }

    /**
     * 使用调度器指令(SchedulerCmd) 执行任务
     *
     * @param jobId        任务ID
     * @param instanceName 指定执行任务的节点(可以为空)
     * @return 成功返回 true
     */
    public boolean useCmdExecJob(Long jobId, String instanceName) {
        TaskJob job = taskStore.beginReadOnlyTX(status -> taskStore.getJob(jobId));
        Assert.notNull(job, "任务不存在");
        return createAndWaitSchedulerCmd(job.getNamespace(), instanceName, SchedulerCmdInfo.createExecJobCmd(jobId));
    }

    /**
     * 使用调度器指令(SchedulerCmd) 暂停调度器
     */
    public boolean useCmdPausedScheduler(Long schedulerId) {
        TaskScheduler scheduler = taskStore.beginReadOnlyTX(status -> taskStore.getScheduler(schedulerId));
        Assert.notNull(scheduler, "调度器不存在");
        return createAndWaitSchedulerCmd(scheduler.getNamespace(), scheduler.getInstanceName(), SchedulerCmdInfo.createPausedSchedulerCmd());
    }

    /**
     * 使用调度器指令(SchedulerCmd) 继续运行调度器
     */
    public boolean useCmdResumeScheduler(Long schedulerId) {
        TaskScheduler scheduler = taskStore.beginReadOnlyTX(status -> taskStore.getScheduler(schedulerId));
        Assert.notNull(scheduler, "调度器不存在");
        return createAndWaitSchedulerCmd(scheduler.getNamespace(), scheduler.getInstanceName(), SchedulerCmdInfo.createResumeSchedulerCmd());
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- service

    private void startScheduler(boolean startScheduler) {
        long initialDelay = 0;
        TaskScheduler taskScheduler = registerScheduler();
        taskContext.setCurrentScheduler(taskScheduler);
        // 维护当前集群可用的调度器列表
        reloadSchedulerFuture = scheduledExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    reloadScheduler();
                } catch (Exception e) {
                    log.error("[TaskInstance] 维护当前集群可用的调度器列表失败 | instanceName={}", getInstanceName(), e);
                    TaskSchedulerLog schedulerLog = newSchedulerLog();
                    schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_RELOAD_SCHEDULER_ERROR, ExceptionUtils.getStackTraceAsString(e));
                    schedulerErrorListener(schedulerLog, e);
                }
            },
            initialDelay, GlobalConstant.RELOAD_SCHEDULER_INTERVAL, TimeUnit.MILLISECONDS
        );
        // 触发接下来(N+M)秒内需要触发的触发器
        fireTriggerFuture = scheduledExecutor.submit(() -> {
            try {
                fireTriggers();
            } catch (Exception e) {
                log.error("[TaskInstance] 调度核心线程错误 | instanceName={}", getInstanceName(), e);
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_FIRE_TRIGGERS_ERROR, ExceptionUtils.getStackTraceAsString(e));
                schedulerErrorListener(schedulerLog, e);
                paused();
            }
        });
        if (startScheduler) {
            startStandbyScheduler(initialDelay, taskScheduler);
        }
    }

    private void startStandbyScheduler(long initialDelay, TaskScheduler taskScheduler) {
        // 调度器节点注册&更新调度器运行信息
        if (registerSchedulerFuture == null || registerSchedulerFuture.isCancelled()) {
            registerSchedulerFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        registerScheduler();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 调度器节点注册失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_REGISTER_SCHEDULER_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                10_000, GlobalConstant.REGISTER_SCHEDULER_INTERVAL, TimeUnit.MILLISECONDS
            );
        }
        // 集群节点心跳保持
        if (heartbeatFuture == null || heartbeatFuture.isCancelled()) {
            heartbeatFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        heartbeat();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 心跳保持失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_HEART_BEAT_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                initialDelay, taskScheduler.getHeartbeatInterval(), TimeUnit.MILLISECONDS
            );
        }
        // 数据完整性校验&一致性校验
        if (dataCheckFuture == null || dataCheckFuture.isCancelled()) {
            dataCheckFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        dataCheck();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 数据完整性校验失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_DATA_CHECK_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                initialDelay, GlobalConstant.DATA_CHECK_INTERVAL, TimeUnit.MILLISECONDS
            );
        }
        // 执行调度器指令
        if (execSchedulerCmdFuture == null || execSchedulerCmdFuture.isCancelled()) {
            execSchedulerCmdFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        execSchedulerCmd();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 执行调度器指令失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_EXEC_SCHEDULER_CMD_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                initialDelay, GlobalConstant.EXEC_SCHEDULER_CMD_INTERVAL, TimeUnit.MILLISECONDS
            );
        }
        // 清理日志数据任务
        if (clearLogFuture == null || clearLogFuture.isCancelled()) {
            clearLogFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        clearLogs();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 清理日志数据失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_CLEAR_LOG_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                initialDelay, GlobalConstant.CLEAR_LOG_INTERVAL, TimeUnit.MILLISECONDS
            );
        }
        // 收集任务执行报表
        if (collectReportFuture == null || collectReportFuture.isCancelled()) {
            collectReportFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        collectReport();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 收集任务执行报表失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_COLLECT_REPORT_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                initialDelay, GlobalConstant.COLLECT_REPORT_INTERVAL, TimeUnit.MILLISECONDS
            );
        }
    }

    private void stopFuture(boolean stopScheduler) {
        Consumer<Future<?>> stopFuture = future -> {
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }
        };
        if (stopScheduler) {
            // 停止调度器时
            stopFuture.accept(registerSchedulerFuture);
            stopFuture.accept(heartbeatFuture);
            stopFuture.accept(dataCheckFuture);
            stopFuture.accept(execSchedulerCmdFuture);
            stopFuture.accept(clearLogFuture);
            stopFuture.accept(collectReportFuture);
        }
        stopFuture.accept(reloadSchedulerFuture);
        stopFuture.accept(fireTriggerFuture);
    }

    private void doStandby() {
        try {
            long initialDelay = 0;
            TaskScheduler taskScheduler = registerScheduler();
            taskContext.setCurrentScheduler(taskScheduler);
            startStandbyScheduler(initialDelay, taskScheduler);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventName(TaskSchedulerLog.EVENT_STANDBY);
            scheduledExecutor.execute(() -> schedulerStartedListener(schedulerLog));
        } catch (Exception e) {
            log.error("[TaskInstance] 调度器准备失败 | instanceName={}", getInstanceName(), e);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_STANDBY_ERROR, ExceptionUtils.getStackTraceAsString(e));
            scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
            throw ExceptionUtils.unchecked(e);
        }
    }

    private void doStart() {
        try {
            startScheduler(true);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventName(TaskSchedulerLog.EVENT_STARTED);
            scheduledExecutor.execute(() -> schedulerStartedListener(schedulerLog));
        } catch (Exception e) {
            log.error("[TaskInstance] 调度器启动失败 | instanceName={}", getInstanceName(), e);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_STARTED_ERROR, ExceptionUtils.getStackTraceAsString(e));
            scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
            throw ExceptionUtils.unchecked(e);
        }
    }

    private void doPause() {
        try {
            stopFuture(false);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventName(TaskSchedulerLog.EVENT_PAUSED);
            scheduledExecutor.execute(() -> schedulerPausedListener(schedulerLog));
        } catch (Exception e) {
            log.error("[TaskInstance] 暂停调度器失败 | instanceName={}", getInstanceName(), e);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_PAUSED_ERROR, ExceptionUtils.getStackTraceAsString(e));
            scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
            throw ExceptionUtils.unchecked(e);
        }
    }

    private void doResume() {
        try {
            startScheduler(false);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventName(TaskSchedulerLog.EVENT_RESUME);
            scheduledExecutor.execute(() -> schedulerResumeListener(schedulerLog));
        } catch (Exception e) {
            log.error("[TaskInstance] 调度器恢复运行失败 | instanceName={}", getInstanceName(), e);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_RESUME_ERROR, ExceptionUtils.getStackTraceAsString(e));
            scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
            throw ExceptionUtils.unchecked(e);
        }
    }

    private void doStop() {
        try {
            stopFuture(true);
            scheduledExecutor.shutdownNow();
            jobExecutor.shutdownNow();
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventName(TaskSchedulerLog.EVENT_SHUTDOWN);
            schedulerStopListener(schedulerLog);
        } catch (Exception e) {
            log.error("[TaskInstance] 停止调度器失败 | instanceName={}", getInstanceName(), e);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_SHUTDOWN, ExceptionUtils.getStackTraceAsString(e));
            schedulerErrorListener(schedulerLog, e);
        }
    }

    /**
     * 调度器节点注册，返回注册后的调度器对象
     */
    private TaskScheduler registerScheduler() {
        TaskScheduler scheduler = toScheduler(taskContext.getSchedulerConfig());
        return taskStore.beginTX(status -> taskStore.addOrUpdateScheduler(scheduler));
    }

    /**
     * 心跳保持
     */
    private void heartbeat() {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        taskStore.beginTX(status -> taskStore.heartbeat(scheduler));
    }

    /**
     * 数据完整性校验&一致性校验
     */
    private void dataCheck() throws SchedulerException {
        Date now = taskStore.currentDate();
        // 最后一次校验时间
        Date lastDataCheckDate = taskStore.lastEventNameDate(getNamespace(), TaskSchedulerLog.EVENT_DATA_CHECK_ERROR);
        long maxWait = Math.max(GlobalConstant.DATA_CHECK_INTERVAL / 2, 300_000);
        if (lastDataCheckDate != null && (now.getTime() - lastDataCheckDate.getTime() < maxWait)) {
            return;
        }
        // 计算下一次触发时间，但不更新数据，仅检查有无异常
        calcNextFireTime(false);
        // 数据完整性校验&一致性校验
        // 1.task_http_job、task_java_job、task_js_job、task_shell_job 与 task_job 是否是一对一的关系
        // 2.task_job_trigger 与 task_job 是否是一对一的关系
        // 3. task_job: type、route_strategy、first_scheduler、whitelist_scheduler、blacklist_scheduler、load_balance 字段值的有效性
        // 4.task_http_job: request_method 字段值的有效性
        // 5.task_java_job: class_name、class_method 字段值的有效性
        // 6.task_shell_job: shell_type、shell_charset 字段值的有效性
        // 7.task_job_trigger: misfire_strategy、type、cron、fixed_interval 字段值的有效性
        String errMsg = taskStore.dataCheck(getNamespace());
        if (StringUtils.isNotBlank(errMsg)) {
            throw new SchedulerException(errMsg);
        }
    }

    /**
     * 初始化触发器下一次触发时间(校准触发器触发时间)
     */
    private void calcNextFireTime(boolean updateNextFireTime) {
        // 1.更新无效的Trigger配置
        long invalidCount = taskStore.beginReadOnlyTX(status -> taskStore.countInvalidTrigger(getNamespace()));
        long updateCount = taskStore.beginTX(status -> taskStore.updateInvalidTrigger(getNamespace()));
        if (updateCount > 0) {
            log.info("[TaskInstance] 更新异常触发器nextFireTime=null | 更新数量：{} | instanceName={}", updateCount, getInstanceName());
        }
        // 全部cron触发器列表
        List<TaskJobTrigger> cronTriggerList = taskStore.beginReadOnlyTX(status -> taskStore.queryEnableCronTrigger(getNamespace()));
        // 有效的cron触发器列表
        List<TaskJobTrigger> effectiveCronTriggerList = new ArrayList<>(cronTriggerList.size());
        // 检查cron格式有效性
        for (TaskJobTrigger cronTrigger : cronTriggerList) {
            boolean effective = CronExpressionUtil.isValidExpression(cronTrigger.getCron());
            if (effective) {
                effectiveCronTriggerList.add(cronTrigger);
            } else {
                invalidCount++;
                if (cronTrigger.getNextFireTime() != null) {
                    cronTrigger.setNextFireTime(null);
                    taskStore.beginTX(status -> taskStore.updateNextFireTime(cronTrigger.getId(), cronTrigger.getNextFireTime()));
                }
            }
        }
        // 2.计算触发器下一次触发时间
        if (updateNextFireTime) {
            // 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
            updateCount = taskStore.beginTX(status -> taskStore.updateNextFireTimeForType2(getNamespace()));
            // 更新cron触发器下一次触发时间 -> type=1
            for (TaskJobTrigger cronTrigger : effectiveCronTriggerList) {
                try {
                    final Date nextFireTime = JobTriggerUtils.getNextFireTime(cronTrigger);
                    if (cronTrigger.getNextFireTime() == null || cronTrigger.getNextFireTime().compareTo(nextFireTime) != 0) {
                        updateCount++;
                        cronTrigger.setNextFireTime(nextFireTime);
                        taskStore.beginTX(status -> taskStore.updateNextFireTime(cronTrigger.getId(), cronTrigger.getNextFireTime()));
                    }
                } catch (Exception e) {
                    log.error("[TaskInstance] 计算触发器下一次触发时间失败 | JobTrigger(id={}) | instanceName={}", cronTrigger.getId(), getInstanceName(), e);
                    TaskSchedulerLog schedulerLog = newSchedulerLog();
                    schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_CALC_CRON_NEXT_FIRE_TIME_ERROR, ExceptionUtils.getStackTraceAsString(e));
                    scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
                }
            }
            log.info("[TaskInstance] 更新触发器下一次触发时间nextFireTime字段 | 更新数量：{} | instanceName={}", updateCount, getInstanceName());
        }
        if (invalidCount > 0) {
            log.warn("[TaskInstance] 触发器配置检查完成，异常的触发器数量：{} | instanceName={}", invalidCount, getInstanceName());
        } else {
            log.info("[TaskInstance] 触发器配置检查完成，无异常触发器 | instanceName={}", getInstanceName());
        }
    }

    /**
     * 维护当前集群可用的调度器列表
     */
    private void reloadScheduler() {
        final List<TaskScheduler> availableSchedulerList = taskStore.beginReadOnlyTX(status -> taskStore.queryAvailableSchedulerList(getNamespace()));
        taskContext.setAvailableSchedulerList(availableSchedulerList);
    }

    /**
     * 维护接下来(N+M)秒内需要触发的触发器列表
     */
    private void reloadNextTrigger() {
        final int nextTime = GlobalConstant.NEXT_TRIGGER_N + GlobalConstant.NEXT_TRIGGER_M;
        final List<TaskJobTrigger> nextJobTriggerList = taskStore.beginReadOnlyTX(status -> taskStore.queryNextTrigger(getNamespace(), nextTime));
        // 添加任务到时间轮调度器
        for (TaskJobTrigger trigger : nextJobTriggerList) {
            taskContext.addNextTrigger(trigger);
        }
    }

    /**
     * 触发接下来(N+M)秒内需要触发的触发器
     */
    private void fireTriggers() {
        // 初始化触发器下一次触发时间(校准触发器触发时间)
        calcNextFireTime(true);
        // 维护接下来(N+M)秒内需要触发的触发器列表
        reloadNextTrigger();
        final long startTime = JobTriggerUtils.removeMillisecond(taskStore.currentTimeMillis()) + 1000;
        // 使用无限循环触发任务, 每一秒循环一次(每秒触发一次)
        for (long count = 1; ; count++) {
            log.info("fireTriggers | currentTimeMillis={}", taskStore.currentTimeMillis());
            // 判断调度器是否已暂停/停止
            int state = STATE_UPDATER.get(this);
            if (state == State.PAUSED || state == State.STOPPED) {
                break;
            }
            // 触发需要触发的触发器
            final long nowSecond = JobTriggerUtils.getSecond(taskStore.currentTimeMillis());
            final Map<Long, ConcurrentLinkedQueue<TaskJobTrigger>> triggerMap = taskContext.getNextTriggers(nowSecond);
            final int triggerCount = triggerMap.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
            if (triggerCount > GlobalConstant.NEXT_TRIGGER_MAX_COUNT) {
                log.warn(
                    "[TaskInstance] 在1秒内需要触发的触发器列表超过最大值：{}，当前值：{} | instanceName={}",
                    GlobalConstant.NEXT_TRIGGER_MAX_COUNT, triggerCount, getInstanceName()
                );
            }
            final Set<Long> triggerIds = new HashSet<>(triggerCount);
            final List<Future<?>> futures = new ArrayList<>(triggerCount);
            for (ConcurrentLinkedQueue<TaskJobTrigger> triggers : triggerMap.values()) {
                for (TaskJobTrigger trigger : triggers) {
                    if (!triggerIds.add(trigger.getId())) {
                        break;
                    }
                    try {
                        Future<?> future = scheduledExecutor.submit(() -> doFireTrigger(trigger));
                        futures.add(future);
                    } catch (Exception e) {
                        log.error("[TaskInstance] JobTrigger触发失败 | id={} | name={} | instanceName={}", trigger.getId(), trigger.getName(), getInstanceName(), e);
                    }
                }
            }
            triggerIds.clear();
            // 等待所有触发器触发完成
            long startMillis = taskStore.currentTimeMillis();
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("[TaskInstance] JobTrigger触发失败 | instanceName={}", getInstanceName(), e);
                }
            }
            futures.clear();
            final long waitTrigger = taskStore.currentTimeMillis() - startMillis;
            // 加载下一轮触发器-接下来(N+M)秒内需要触发的
            startMillis = taskStore.currentTimeMillis();
            try {
                reloadNextTrigger();
            } catch (Exception e) {
                log.error("[TaskInstance] 加载下一轮触发器失败 | instanceName={}", getInstanceName(), e);
            }
            final long waitReloadTrigger = taskStore.currentTimeMillis() - startMillis;
            // 计算休眠时间到下一次触发(保证每秒触发一次)
            long sleepTime = startTime + (count * 1000) - taskStore.currentTimeMillis();
            if (sleepTime <= -500) {
                String message = String.format(
                    "调度器超时，建议调整参数配置提高调度器性能 | instanceName=%s | waitTrigger=%s(ms) | waitReloadTrigger=%s(ms) | sleepTime=%s(ms)",
                    getInstanceName(), waitTrigger, waitReloadTrigger, sleepTime
                );
                log.error("[TaskInstance] {}", message);
                jobExecutor.execute(() -> {
                    try {
                        Date lastEvent = taskStore.lastEventNameDate(getNamespace(), TaskSchedulerLog.EVENT_OPTIMIZE_ALARMS);
                        long minInterval = 3_000;
                        if (lastEvent != null && (taskStore.currentTimeMillis() - lastEvent.getTime() < minInterval)) {
                            return;
                        }
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_OPTIMIZE_ALARMS, message);
                        schedulerErrorListener(schedulerLog, new SchedulerException(message));
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
            if (sleepTime > 0) {
                try {
                    // noinspection BusyWait
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                    state = STATE_UPDATER.get(this);
                    if (state == State.PAUSED || state == State.STOPPED) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 触发触发器
     */
    private void doFireTrigger(TaskJobTrigger trigger) {
        final Date dbNow = taskStore.currentDate();
        final TaskJobTriggerLog jobTriggerLog = newJobTriggerLog(dbNow, trigger);
        jobTriggerLog.setFireCount(taskContext.incrementAndGetJobFireCount(trigger.getId()));
        final long startFireTime = taskStore.currentTimeMillis();
        try {
            // 执行触发逻辑 - 是否允许多节点并行触发
            final boolean allowConcurrent = Objects.equals(EnumConstant.JOB_TRIGGER_ALLOW_CONCURRENT_1, trigger.getAllowConcurrent());
            if (allowConcurrent) {
                doTriggerJobExec(dbNow, trigger, jobTriggerLog);
            } else {
                // 获取触发器分布式锁 - 判断是否被其他节点触发了
                taskStore.getLockTrigger(trigger.getNamespace(), trigger.getId(), () -> {
                    // 二次校验数据
                    Long fireCount = taskStore.beginReadOnlyTX(status -> taskStore.getTriggerFireCount(trigger.getNamespace(), trigger.getId()));
                    log.info("@@@ 二次校验数据 InstanceName={} | fireCount({}={})", getInstanceName(), fireCount, trigger.getFireCount());
                    if (Objects.equals(fireCount, trigger.getFireCount())) {
                        doTriggerJobExec(dbNow, trigger, jobTriggerLog);
                    }
                });
            }
            // 是否在当前节点触发执行了任务
            if (jobTriggerLog.getMisFired() == null) {
                // 未触发 - 触发次数减1
                taskContext.decrementAndGetJobFireCount(trigger.getId());
                jobTriggerLog.setFireCount(jobTriggerLog.getFireCount() - 1);
            } else {
                // 已触发 - 触发器触发成功日志
                final long endFireTime = taskStore.currentTimeMillis();
                jobTriggerLog.setTriggerTime((int) (endFireTime - startFireTime));
                scheduledExecutor.execute(() -> jobTriggeredListener(jobTriggerLog));
            }
        } catch (Exception e) {
            log.error("[TaskInstance] JobTrigger触发失败 | id={} | name={} | instanceName={}", trigger.getId(), trigger.getName(), getInstanceName(), e);
            TaskSchedulerLog schedulerLog = newSchedulerLog();
            schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_JOB_TRIGGER_FIRE_ERROR, ExceptionUtils.getStackTraceAsString(e));
            scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
        }
    }

    /**
     * 触发执行定时任务逻辑
     */
    private void doTriggerJobExec(final Date dbNow, final TaskJobTrigger jobTrigger, final TaskJobTriggerLog jobTriggerLog) {
        final TaskJob job = taskStore.beginReadOnlyTX(status -> taskStore.getJob(jobTrigger.getNamespace(), jobTrigger.getJobId()));
        if (job == null) {
            throw new SchedulerException(String.format("JobTrigger对应的Job数据不存在，JobTrigger(id=%s|jobId=%s)", jobTrigger.getId(), jobTrigger.getJobId()));
        }
        // 1.当前任务是否禁用
        if (!Objects.equals(job.getDisable(), EnumConstant.JOB_DISABLE_0)) {
            // 当前任务被禁用
            jobTriggerLog.setMisFired(EnumConstant.JOB_TRIGGER_MIS_FIRED_1);
            jobTriggerLog.setTriggerMsg(String.format("当前任务被禁用，JobId=%s", job.getId()));
            return;
        }
        // 2.控制重入执行 - 最大重入执行数量
        final int jobReentryCount = taskContext.getJobReentryCount(jobTrigger.getJobId());
        if (jobReentryCount > Math.max((Objects.equals(EnumConstant.JOB_ALLOW_CONCURRENT_0, job.getAllowConcurrent()) ? 0 : job.getMaxReentry()), 0)) {
            jobTriggerLog.setMisFired(EnumConstant.JOB_TRIGGER_MIS_FIRED_1);
            jobTriggerLog.setTriggerMsg(String.format("当前节点超过最大重入执行次数，JobId=%s | jobReentryCount=%s | maxReentry=%s", job.getId(), jobReentryCount, job.getMaxReentry()));
            return;
        }
        // 3.控制任务执行节点
        final String currentInstanceName = getInstanceName();
        final List<String> instanceNames;
        List<TaskScheduler> runningScheduler;
        switch (job.getRouteStrategy()) {
            case EnumConstant.JOB_ROUTE_STRATEGY_1:
                // 指定节点优先
                instanceNames = job.getFirstInstanceNames();
                if (!instanceNames.isEmpty()) {
                    // 获取在线且正在运行的节点
                    runningScheduler = taskContext.getRunningSchedulerList();
                    // 存在正在运行的节点 & 当前节点不在集合里
                    if (!runningScheduler.isEmpty()
                        && runningScheduler.stream().anyMatch(scheduler -> instanceNames.contains(scheduler.getInstanceName()))
                        && instanceNames.stream().noneMatch(name -> Objects.equals(name, currentInstanceName))) {
                        return;
                    }
                }
                break;
            case EnumConstant.JOB_ROUTE_STRATEGY_2:
                // 固定节点白名单
                instanceNames = job.getWhitelistInstanceNames();
                // 当前节点不在白名单列表里面
                if (instanceNames.stream().noneMatch(name -> Objects.equals(name, currentInstanceName))) {
                    return;
                }
                break;
            case EnumConstant.JOB_ROUTE_STRATEGY_3:
                // 固定节点黑名单
                instanceNames = job.getBlacklistInstanceNames();
                // 当前节点在黑名单列表里面
                if (instanceNames.stream().anyMatch(name -> Objects.equals(name, currentInstanceName))) {
                    return;
                }
                break;
        }
        // 4.负载均衡策略
        int idx;
        TaskScheduler scheduler;
        switch (job.getLoadBalance()) {
            case EnumConstant.JOB_LOAD_BALANCE_1:
                // 抢占
                break;
            case EnumConstant.JOB_LOAD_BALANCE_2:
                // 随机
                runningScheduler = taskContext.getRunningSchedulerList();
                log.info("@@@ 随机-1 InstanceName={} | size={}", currentInstanceName, runningScheduler.size());
                if (runningScheduler.size() >= 2) {
                    runningScheduler.sort(Comparator.comparing(TaskScheduler::getInstanceName));
                    Object randomSeed = job.getRunCount() == null ? 0L : job.getRunCount();
                    idx = Math.abs(randomSeed.hashCode()) % runningScheduler.size();
                    scheduler = runningScheduler.get(idx);
                    log.info("@@@ 随机-2 InstanceName={} | randomSeed={} | idx={}", currentInstanceName, randomSeed, idx);
                    if (!Objects.equals(scheduler.getInstanceName(), currentInstanceName)) {
                        return;
                    }
                }
                break;
            case EnumConstant.JOB_LOAD_BALANCE_3:
                // 轮询
                runningScheduler = taskContext.getRunningSchedulerList();
                if (runningScheduler.size() >= 2) {
                    runningScheduler.sort(Comparator.comparing(TaskScheduler::getInstanceName));
                    idx = (int) (Math.abs(job.getRunCount() == null ? 0L : job.getRunCount()) % runningScheduler.size());
                    scheduler = runningScheduler.get(idx);
                    if (!Objects.equals(scheduler.getInstanceName(), currentInstanceName)) {
                        return;
                    }
                }
                break;
            case EnumConstant.JOB_LOAD_BALANCE_4:
                // 一致性HASH
                runningScheduler = taskContext.getRunningSchedulerList();
                if (runningScheduler.size() >= 2) {
                    runningScheduler.sort(Comparator.comparing(TaskScheduler::getInstanceName));
                    idx = Math.abs(job.getName().hashCode()) % runningScheduler.size();
                    scheduler = runningScheduler.get(idx);
                    if (!Objects.equals(scheduler.getInstanceName(), currentInstanceName)) {
                        return;
                    }
                }
                break;
        }
        // 触发定时任务
        boolean needRunJob = true;
        TupleOne<Date> lastFireTime = TupleOne.creat(jobTrigger.getNextFireTime());
        // 判断是否错过了触发
        final Integer misfireStrategy = jobTrigger.getMisfireStrategy();
        if (JobTriggerUtils.isMisFire(dbNow, jobTrigger)) {
            needRunJob = false;
            // 需要补偿触发
            switch (misfireStrategy) {
                case EnumConstant.JOB_TRIGGER_MISFIRE_STRATEGY_1:
                    // 忽略补偿触发
                    jobTriggerLog.setMisFired(EnumConstant.JOB_TRIGGER_MIS_FIRED_1);
                    jobTriggerLog.setTriggerMsg(String.format("忽略补偿触发，JobId=%s", job.getId()));
                    break;
                case EnumConstant.JOB_TRIGGER_MISFIRE_STRATEGY_2:
                    // 立即补偿触发一次
                    needRunJob = true;
                    lastFireTime.setValue1(JobTriggerUtils.removeMillisecond(dbNow));
                    jobTriggerLog.setTriggerMsg(String.format("补偿触发，JobId=%s", job.getId()));
                    break;
                default:
                    throw new SchedulerException(String.format("任务触发器misfireStrategy字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
            }
        }
        // 执行定时任务
        if (needRunJob) {
            // 执行任务
            jobTriggerLog.setMisFired(EnumConstant.JOB_TRIGGER_MIS_FIRED_0);
            jobExecutor.execute(() -> {
                final TaskJobLog jobLog = newJobLog(dbNow, job, jobTrigger, jobTriggerLog.getId());
                final String oldJobData = job.getJobData();
                // 控制并发执行 - 是否允许多节点并发执行
                final boolean allowConcurrent = Objects.equals(EnumConstant.JOB_ALLOW_CONCURRENT_1, job.getAllowConcurrent());
                if (allowConcurrent) {
                    executeJob(dbNow, job, jobLog);
                } else {
                    try {
                        // 获取定时任务分布式锁 - 判断是否被其他节点执行了
                        taskStore.getLockJob(job.getNamespace(), job.getId(), () -> {
                            // 二次校验数据
                            Long runCount = taskStore.beginReadOnlyTX(status -> taskStore.getJobRunCount(job.getNamespace(), job.getId()));
                            if (Objects.equals(runCount, job.getRunCount())) {
                                executeJob(dbNow, job, jobLog);
                            }
                        });
                    } catch (Exception e) {
                        log.error("[TaskInstance] Job执行失败 | id={} | name={} | instanceName={}", job.getId(), job.getName(), getInstanceName(), e);
                        jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_1);
                        jobLog.setExceptionInfo(ExceptionUtils.getStackTraceAsString(e));
                        jobEndRunListener(jobLog);
                    }
                }
                // 更新任务数据
                final String newJobData = job.getJobData();
                if (!Objects.equals(oldJobData, newJobData) && Objects.equals(job.getIsUpdateData(), EnumConstant.JOB_IS_UPDATE_DATA_1)) {
                    taskStore.beginTX(status -> taskStore.updateJodData(job.getNamespace(), job.getId(), newJobData));
                }
            });
        }
        // 计算下一次触发时间
        TaskJobTrigger newJobTrigger = taskStore.beginTX(status -> {
            TaskJobTrigger tmpTrigger = taskStore.getTrigger(jobTrigger.getNamespace(), jobTrigger.getId());
            if (tmpTrigger != null) {
                Date newNextFireTime = null;
                try {
                    newNextFireTime = JobTriggerUtils.getNextFireTime(dbNow, tmpTrigger);
                } catch (Exception e) {
                    log.error("[TaskInstance] 计算触发器下一次触发时间失败 | JobTrigger(id={}) | instanceName={}", tmpTrigger.getId(), getInstanceName(), e);
                }
                tmpTrigger.setLastFireTime(lastFireTime.getValue1());
                tmpTrigger.setNextFireTime(newNextFireTime);
                if (taskStore.updateFireTime(tmpTrigger.getId(), tmpTrigger.getLastFireTime(), tmpTrigger.getNextFireTime()) <= 0) {
                    tmpTrigger = null;
                }
            }
            return tmpTrigger;
        });
        if (newJobTrigger == null || newJobTrigger.getNextFireTime() == null) {
            taskContext.removeNextTrigger(jobTrigger);
        }
    }

    /**
     * 执行定时任务逻辑
     */
    private void executeJob(final Date dbNow, final TaskJob job, final TaskJobLog jobLog) {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        try {
            final int jobReentryCount = taskContext.getAndIncrementJobReentryCount(job.getId());
            final long jobRunCount = taskContext.incrementAndGetJobRunCount(job.getId());
            // 控制重入执行
            if (jobReentryCount > Math.max(job.getMaxReentry(), 0)) {
                // 最大重入执行数量
                jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_2);
                jobLog.setExceptionInfo(String.format("当前节点超过最大重入执行次数 jobReentryCount=%s | maxReentry=%s", jobReentryCount, job.getMaxReentry()));
                return;
            }
            // 记录任务执行日志(同步)
            jobLog.setRunCount(jobRunCount);
            jobStartRunListener(jobLog);
            // 获取JobExecutor
            JobExecutor jobExecutor = null;
            for (JobExecutor executor : jobExecutors) {
                if (executor.support(job.getType())) {
                    jobExecutor = executor;
                    break;
                }
            }
            if (jobExecutor == null) {
                throw new SchedulerException(String.format("暂不支持的任务类型，Job(id=%s)", job.getId()));
            }
            // 支持重试执行任务
            final int maxRetryCount = Math.max(job.getMaxRetryCount(), 1);
            final long startTime = taskStore.currentTimeMillis();
            int retryCount = 0;
            while (retryCount < maxRetryCount) {
                retryCount++;
                try {
                    JobContext jobContext = new JobContext(dbNow, job, jobLog, scheduler, taskStore);
                    jobExecutor.exec(jobContext);
                    jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_0);
                    if (!jobContext.getJobData().isEmpty() && Objects.equals(job.getIsUpdateData(), EnumConstant.JOB_IS_UPDATE_DATA_1)) {
                        job.setJobData(JacksonMapper.getInstance().toJson(jobContext.getJobData()));
                    }
                    break;
                } catch (Exception e) {
                    log.error("[TaskInstance] Job执行失败，重试次数：{} | id={} | name={} | instanceName={}", retryCount, job.getId(), job.getName(), getInstanceName(), e);
                    // 记录任务执行日志(同步)
                    final long endTime = taskStore.currentTimeMillis();
                    jobLog.setRunTime((int) (endTime - startTime));
                    jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_1);
                    jobLog.setRetryCount(retryCount);
                    jobLog.setExceptionInfo(ExceptionUtils.getStackTraceAsString(e));
                    jobRetryRunListener(jobLog, e);
                }
            }
            final long endTime = taskStore.currentTimeMillis();
            jobLog.setRunTime((int) (endTime - startTime));
            jobLog.setAfterJobData(job.getJobData());
        } catch (Exception e) {
            log.error("[TaskInstance] Job执行失败 | id={} | name={} | instanceName={}", job.getId(), job.getName(), getInstanceName(), e);
            jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_1);
            jobLog.setExceptionInfo(ExceptionUtils.getStackTraceAsString(e));
        } finally {
            // 更新 runCount
            taskStore.beginTX(status -> taskStore.updateJobRunCount(job.getNamespace(), job.getId()));
            // 任务执行事件处理
            taskContext.decrementAndGetJobReentryCount(job.getId());
            jobEndRunListener(jobLog);
        }
    }

    /**
     * 创建并等待调度器指定执行完成
     */
    private boolean createAndWaitSchedulerCmd(String namespace, String instanceName, SchedulerCmdInfo schedulerCmdInfo) {
        TaskSchedulerCmd schedulerCmd = new TaskSchedulerCmd();
        schedulerCmd.setNamespace(namespace);
        schedulerCmd.setInstanceName(instanceName);
        schedulerCmd.setCmdInfo(JacksonMapper.getInstance().toJson(schedulerCmdInfo));
        taskStore.beginTX(status -> taskStore.saveSchedulerCmd(schedulerCmd));
        boolean success = false;
        long awaitEndTime = taskStore.currentTimeMillis() + GlobalConstant.EXEC_SCHEDULER_CMD_INTERVAL + 30_000;
        while (taskStore.currentTimeMillis() <= awaitEndTime) {
            try {
                // noinspection BusyWait
                Thread.sleep(800);
            } catch (InterruptedException e) {
                log.info("休眠失败", e);
            }
            TaskSchedulerCmd newCmd = taskStore.beginReadOnlyTX(status -> taskStore.getSchedulerCmd(schedulerCmd.getId()));
            if (newCmd == null) {
                break;
            }
            success = Objects.equals(newCmd.getState(), EnumConstant.SCHEDULER_CMD_STATE_2);
            if (success) {
                break;
            }
        }
        taskStore.beginTX(status -> taskStore.delSchedulerCmd(schedulerCmd.getId()));
        return success;
    }

    private void execSchedulerCmd() {
        List<TaskSchedulerCmd> nextSchedulerCmd = taskStore.beginReadOnlyTX(status -> taskStore.getNextSchedulerCmd(getNamespace(), getInstanceName()));
        for (TaskSchedulerCmd schedulerCmd : nextSchedulerCmd) {
            try {
                // 在 state 字段上使用了乐观锁
                boolean locked = taskStore.beginTX(status -> taskStore.updateSchedulerCmdState(
                    schedulerCmd.getId(), EnumConstant.SCHEDULER_CMD_STATE_1, EnumConstant.SCHEDULER_CMD_STATE_0
                ));
                if (!locked) {
                    continue;
                }
                if (!Objects.equals(schedulerCmd.getState(), EnumConstant.SCHEDULER_CMD_STATE_0) || StringUtils.isBlank(schedulerCmd.getCmdInfo())) {
                    continue;
                }
                SchedulerCmdInfo cmdInfo = JacksonMapper.getInstance().fromJson(schedulerCmd.getCmdInfo(), SchedulerCmdInfo.class);
                if (cmdInfo == null) {
                    continue;
                }
                switch (cmdInfo.getOperation()) {
                    case SchedulerCmdInfo.EXEC_JOB:
                        execJob(cmdInfo.getJobId());
                        break;
                    case SchedulerCmdInfo.PAUSED_SCHEDULER:
                        if (Objects.equals(schedulerCmd.getInstanceName(), getInstanceName())) {
                            paused();
                            registerScheduler();
                        }
                        break;
                    case SchedulerCmdInfo.RESUME_SCHEDULER:
                        if (Objects.equals(schedulerCmd.getInstanceName(), getInstanceName())) {
                            resume();
                            registerScheduler();
                        }
                        break;
                    default:
                        throw new SchedulerException("不支持的操作: " + cmdInfo.getOperation());
                }
                taskStore.beginTX(status -> taskStore.updateSchedulerCmdState(schedulerCmd.getId(), EnumConstant.SCHEDULER_CMD_STATE_2));
            } catch (Exception e) {
                log.error("[TaskInstance] 执行调度器指令失败 | instanceName={}", getInstanceName(), e);
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_EXEC_SCHEDULER_CMD_ERROR, ExceptionUtils.getStackTraceAsString(e));
                schedulerErrorListener(schedulerLog, e);
            }
        }
    }

    private void clearLogs() {
        SchedulerConfig config = taskContext.getSchedulerConfig();
        long logRetention = -1;
        if (config.getLogRetention() != null) {
            logRetention = config.getLogRetention().toMillis();
        }
        if (logRetention <= 0) {
            return;
        }
        Date maxDate = new Date(taskStore.currentTimeMillis() - logRetention);
        log.info("清理超时的调度器指令开始...");
        long count = taskStore.beginTX(status -> taskStore.clearSchedulerCmd());
        log.info("清理超时的调度器指令数据量: {}", count);
        log.info("清理任务执行日志开始...");
        count = taskStore.beginTX(status -> taskStore.clearJobLog(getNamespace(), maxDate));
        log.info("清理任务执行日志数据量: {}", count);
        log.info("清理任务触发日志开始...");
        count = taskStore.beginTX(status -> taskStore.clearTriggerJobLog(getNamespace(), maxDate));
        log.info("清理任务触发日志数据量: {}", count);
        log.info("清理调度器日志开始...");
        count = taskStore.beginTX(status -> taskStore.clearSchedulerLog(getNamespace(), maxDate));
        log.info("清理调度器日志数据量: {}", count);
        log.info("清理任务控制台日志开始...");
        count = taskStore.beginTX(status -> taskStore.clearJobConsoleLog(getNamespace(), maxDate));
        log.info("清理任务控制台日志数据量: {}", count);
    }

    private void collectReport() {
        final Date maxDate = DateUtils.getDayStartTime(DateUtils.addDays(taskStore.currentDate(), -1));
        Date minDate;
        String lastReportTime = taskStore.beginReadOnlyTX(status -> taskStore.getLastReportTime(getNamespace()));
        if (StringUtils.isBlank(lastReportTime)) {
            minDate = taskStore.beginReadOnlyTX(status -> taskStore.getMinFireTime(getNamespace()));
        } else {
            minDate = DateUtils.parseDate(lastReportTime, DateUtils.yyyy_MM_dd);
        }
        if (minDate == null) {
            return;
        }
        List<TaskReport> listReport = new ArrayList<>();
        Date currentDate = DateUtils.getDayStartTime(minDate);
        while (currentDate.compareTo(maxDate) <= 0) {
            final Date finalDate = currentDate;
            TaskReport taskReport = taskStore.beginReadOnlyTX(status -> taskStore.createTaskReport(getNamespace(), finalDate));
            listReport.add(taskReport);
            currentDate = DateUtils.addDays(finalDate, 1);
        }
        if (!listReport.isEmpty()) {
            long count = taskStore.beginTX(status -> taskStore.addOrUpdateTaskReport(listReport));
            log.info("收集任务执行报表数据量: {}", count);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- listeners

    /**
     * 调度器启动完成
     */
    private void schedulerStartedListener(TaskSchedulerLog schedulerLog) {
        if (schedulerListeners == null || schedulerListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (SchedulerListener schedulerListener : schedulerListeners) {
            if (schedulerListener == null) {
                continue;
            }
            try {
                schedulerListener.onStarted(scheduler, taskStore, schedulerLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 调度器启动完成事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 调度器已暂停
     */
    private void schedulerPausedListener(TaskSchedulerLog schedulerLog) {
        if (schedulerListeners == null || schedulerListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (SchedulerListener schedulerListener : schedulerListeners) {
            if (schedulerListener == null) {
                continue;
            }
            try {
                schedulerListener.onPaused(scheduler, taskStore, schedulerLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 调度器暂停事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 调度器已取消暂停(继续运行)
     */
    private void schedulerResumeListener(TaskSchedulerLog schedulerLog) {
        if (schedulerListeners == null || schedulerListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (SchedulerListener schedulerListener : schedulerListeners) {
            if (schedulerListener == null) {
                continue;
            }
            try {
                schedulerListener.onResume(scheduler, taskStore, schedulerLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 调度器取消暂停事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 调度器已停止
     */
    private void schedulerStopListener(TaskSchedulerLog schedulerLog) {
        if (schedulerListeners == null || schedulerListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (SchedulerListener schedulerListener : schedulerListeners) {
            if (schedulerListener == null) {
                continue;
            }
            try {
                schedulerListener.onStop(scheduler, taskStore, schedulerLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 调度器停止事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 调度器出现错误
     */
    private void schedulerErrorListener(TaskSchedulerLog schedulerLog, Exception error) {
        if (schedulerListeners == null || schedulerListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (SchedulerListener schedulerListener : schedulerListeners) {
            if (schedulerListener == null) {
                continue;
            }
            try {
                schedulerListener.onErrorEvent(scheduler, taskStore, schedulerLog, error);
            } catch (Exception e) {
                log.error("[TaskInstance] 调度器出现错误事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 触发成功
     */
    private void jobTriggeredListener(TaskJobTriggerLog jobTriggerLog) {
        if (jobTriggerListeners == null || jobTriggerListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (JobTriggerListener jobTriggerListener : jobTriggerListeners) {
            if (jobTriggerListener == null) {
                continue;
            }
            try {
                jobTriggerListener.onTriggered(scheduler, taskStore, jobTriggerLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 触发器触发成功事件处理失败 | schedulerListener={} | instanceName={}", jobTriggerListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 开始执行
     */
    private void jobStartRunListener(TaskJobLog jobLog) {
        if (jobListeners == null || jobListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (JobListener jobListener : jobListeners) {
            if (jobListener == null) {
                continue;
            }
            try {
                jobListener.onStartRun(scheduler, taskStore, jobLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 任务开始执行事件处理失败 | schedulerListener={} | instanceName={}", jobListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 执行完成(成功或者失败)
     */
    private void jobEndRunListener(TaskJobLog jobLog) {
        if (jobListeners == null || jobListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (JobListener jobListener : jobListeners) {
            if (jobListener == null) {
                continue;
            }
            try {
                jobListener.onEndRun(scheduler, taskStore, jobLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 任务执行完成事件处理失败 | schedulerListener={} | instanceName={}", jobListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    /**
     * 重试执行
     */
    private void jobRetryRunListener(TaskJobLog jobLog, Exception error) {
        if (jobListeners == null || jobListeners.isEmpty()) {
            return;
        }
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        for (JobListener jobListener : jobListeners) {
            if (jobListener == null) {
                continue;
            }
            try {
                jobListener.onRetryRun(scheduler, taskStore, jobLog, error);
            } catch (Exception e) {
                log.error("[TaskInstance] 任务重试执行事件处理失败 | schedulerListener={} | instanceName={}", jobListener.getClass().getName(), getInstanceName(), e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- support

    // SchedulerConfig 转换成 Scheduler
    private TaskScheduler toScheduler(SchedulerConfig schedulerConfig) {
        SchedulerRuntimeConfig config = toSchedulerConfig(schedulerConfig);
        SchedulerRuntimeInfo runtimeInfo = getSchedulerRuntimeInfo();
        TaskScheduler scheduler = new TaskScheduler();
        scheduler.setNamespace(schedulerConfig.getNamespace());
        scheduler.setInstanceName(schedulerConfig.getInstanceName());
        scheduler.setHeartbeatInterval(schedulerConfig.getHeartbeatInterval());
        scheduler.setConfig(JacksonMapper.getInstance().toJson(config));
        scheduler.setRuntimeInfo(JacksonMapper.getInstance().toJson(runtimeInfo));
        scheduler.setDescription(schedulerConfig.getDescription());
        return scheduler;
    }

    private SchedulerRuntimeConfig toSchedulerConfig(SchedulerConfig schedulerConfig) {
        SchedulerRuntimeConfig config = new SchedulerRuntimeConfig();
        config.setSchedulerExecutorPoolSize(schedulerConfig.getSchedulerExecutorPoolSize());
        config.setJobExecutorPoolSize(schedulerConfig.getJobExecutorPoolSize());
        config.setJobExecutorQueueSize(schedulerConfig.getJobExecutorQueueSize());
        config.setLoadWeight(schedulerConfig.getLoadWeight());
        return config;
    }

    private SchedulerRuntimeInfo getSchedulerRuntimeInfo() {
        SchedulerRuntimeInfo runtimeInfo = new SchedulerRuntimeInfo();
        runtimeInfo.setState(getState());
        runtimeInfo.setStateText(getStateText());
        runtimeInfo.setScheduledExecutorState(ThreadUtils.getPoolInfo(scheduledExecutor));
        runtimeInfo.setJobExecutorState(ThreadUtils.getPoolInfo(jobExecutor));
        return runtimeInfo;
    }

    private TaskSchedulerLog newSchedulerLog() {
        TaskSchedulerLog schedulerLog = new TaskSchedulerLog();
        schedulerLog.setNamespace(getNamespace());
        schedulerLog.setInstanceName(getInstanceName());
        return schedulerLog;
    }

    // 自动触发
    private TaskJobTriggerLog newJobTriggerLog(Date dbNow, TaskJobTrigger jobTrigger) {
        TaskJobTriggerLog jobTriggerLog = new TaskJobTriggerLog();
        jobTriggerLog.setId(taskStore.getSnowFlake().nextId());
        jobTriggerLog.setNamespace(getNamespace());
        jobTriggerLog.setInstanceName(getInstanceName());
        jobTriggerLog.setJobTriggerId(jobTrigger.getId());
        jobTriggerLog.setJobId(jobTrigger.getJobId());
        jobTriggerLog.setTriggerName(jobTrigger.getName());
        jobTriggerLog.setFireTime(dbNow);
        jobTriggerLog.setIsManual(EnumConstant.JOB_TRIGGER_IS_MANUAL_0);
        jobTriggerLog.setLastFireTime(jobTrigger.getLastFireTime());
        jobTriggerLog.setNextFireTime(jobTrigger.getNextFireTime());
        return jobTriggerLog;
    }

    // 手动触发
    private TaskJobTriggerLog newJobTriggerLog(Date dbNow, Long jobId) {
        TaskJobTriggerLog jobTriggerLog = new TaskJobTriggerLog();
        jobTriggerLog.setId(taskStore.getSnowFlake().nextId());
        jobTriggerLog.setNamespace(getNamespace());
        jobTriggerLog.setInstanceName(getInstanceName());
        jobTriggerLog.setJobId(jobId);
        jobTriggerLog.setTriggerName("用户手动触发");
        jobTriggerLog.setFireTime(dbNow);
        jobTriggerLog.setIsManual(EnumConstant.JOB_TRIGGER_IS_MANUAL_1);
        jobTriggerLog.setFireCount(-1L);
        jobTriggerLog.setMisFired(EnumConstant.JOB_TRIGGER_MIS_FIRED_0);
        return jobTriggerLog;
    }

    private TaskJobLog newJobLog(Date dbNow, TaskJob job, TaskJobTrigger jobTrigger, Long jobTriggerLogId) {
        TaskJobLog jobLog = new TaskJobLog();
        jobLog.setId(taskStore.getSnowFlake().nextId());
        jobLog.setNamespace(getNamespace());
        jobLog.setInstanceName(getInstanceName());
        jobLog.setJobTriggerLogId(jobTriggerLogId);
        if (jobTrigger != null) {
            jobLog.setJobTriggerId(jobTrigger.getId());
        }
        jobLog.setJobId(job.getId());
        jobLog.setFireTime(dbNow);
        jobLog.setRetryCount(0);
        jobLog.setBeforeJobData(job.getJobData());
        return jobLog;
    }
}
