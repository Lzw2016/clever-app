package org.clever.task.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.core.Conv;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.id.SnowFlake;
import org.clever.core.mapper.JacksonMapper;
import org.clever.data.jdbc.QueryDSL;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.job.JobExecutor;
import org.clever.task.core.listeners.JobListener;
import org.clever.task.core.listeners.JobTriggerListener;
import org.clever.task.core.listeners.SchedulerListener;
import org.clever.task.core.model.*;
import org.clever.task.core.model.entity.*;
import org.clever.task.core.support.DataBaseClock;
import org.clever.task.core.support.JobTriggerUtils;
import org.clever.task.core.support.TaskContext;
import org.clever.task.core.support.WheelTimer;
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
     */
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
     * 调度器节点注册
     */
    private ScheduledFuture<?> registerSchedulerFuture;
    /**
     * 集群节点心跳保持
     */
    private ScheduledFuture<?> heartbeatFuture;
    /**
     * 数据完整性校验、一致性校验
     */
    private ScheduledFuture<?> dataCheckFuture;
    /**
     * 初始化触发器下一次触发时间(校准触发器触发时间)
     */
    private ScheduledFuture<?> calcNextFireTimeFuture;
    /**
     * 维护当前集群可用的调度器列表
     */
    private ScheduledFuture<?> reloadSchedulerFuture;
    /**
     * 维护接下来N秒内需要触发的触发器列表
     */
    private ScheduledFuture<?> reloadNextTriggerFuture;
    /**
     * 调度器状态
     */
    private volatile int state = State.INIT;
    /**
     * 内部时间轮调度器
     */
    private final WheelTimer wheelTimer;

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
        this.wheelTimer = new WheelTimer(
                new BasicThreadFactory.Builder().namingPattern("task-wheel-pool-%d").daemon(true).build(),
                this.jobExecutor,
                new DataBaseClock(queryDSL.getJdbc()),
                GlobalConstant.TRIGGER_JOB_EXEC_INTERVAL,
                TimeUnit.MILLISECONDS,
                512
        );
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- api

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
        Assert.isTrue(State.INIT != getState(), "当前定时任务调度器还未启动");
        return taskContext.getCurrentScheduler().getNamespace();
    }

    /**
     * 当前调度器实例名
     */
    public String getInstanceName() {
        Assert.isTrue(State.INIT != getState(), "当前定时任务调度器还未启动");
        return taskContext.getCurrentScheduler().getInstanceName();
    }

    /**
     * 获取调度器 TaskStore
     */
    public TaskStore getTaskStore() {
        return taskStore;
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
     * 新增或更新 http 定时任务，基于 jobId 判断是更新还是新增
     */
    public void addOrUpdateHttpJob(long jobId, HttpJobModel httpJob, AbstractTrigger trigger) {
        // TODO addOrUpdateHttpJob
    }

    /**
     * 新增或更新 java 定时任务，基于 jobId 判断是更新还是新增
     */
    public void addOrUpdateJavaJob(long jobId, JavaJobModel javaJob, AbstractTrigger trigger) {
    }

    /**
     * 新增或更新 js 定时任务，基于 jobId 判断是更新还是新增
     */
    public void addOrUpdateJsJob(long jobId, JsJobModel jsJob, AbstractTrigger trigger) {
    }

    /**
     * 新增或更新 shell 定时任务，基于 jobId 判断是更新还是新增
     */
    public void addOrUpdateShellJob(long jobId, ShellJobModel shellJob, AbstractTrigger trigger) {
    }

    /**
     * 新增 http 定时任务
     */
    public void addHttpJob(HttpJobModel httpJob, AbstractTrigger trigger) {
    }

    /**
     * 新增 java 定时任务
     */
    public void addJavaJob(JavaJobModel javaJob, AbstractTrigger trigger) {
    }

    /**
     * 新增 js 定时任务
     */
    public void addJsJob(JsJobModel jsJob, AbstractTrigger trigger) {
    }

    /**
     * 新增 shell 定时任务
     */
    public void addShellJob(ShellJobModel shellJob, AbstractTrigger trigger) {
    }

    /**
     * 更新 http 定时任务
     */
    public void updateHttpJob(long jobId, HttpJobModel httpJob) {
    }

    /**
     * 更新 java 定时任务
     */
    public void updateJavaJob(long jobId, JavaJobModel javaJob) {
    }

    /**
     * 更新 js 定时任务
     */
    public void updateJsJob(long jobId, JsJobModel jsJob) {
    }

    /**
     * 更新 shell 定时任务
     */
    public void updateShellJob(long jobId, ShellJobModel shellJob) {
    }

    /**
     * 更新触发器
     */
    public void updateTrigger(long triggerId, AbstractTrigger trigger) {
    }

    /**
     * 增加定时任务
     */
    public AddJobRes addJob(AbstractJob jobModel, AbstractTrigger trigger) {
        Assert.notNull(jobModel, "参数jobModel不能为空");
        Assert.notNull(trigger, "参数trigger不能为空");
        final AddJobRes addJobRes = new AddJobRes();
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        final TaskJob job = jobModel.toJob();
        job.setNamespace(scheduler.getNamespace());
        final TaskJobTrigger jobTrigger = trigger.toJobTrigger();
        jobTrigger.setNamespace(scheduler.getNamespace());
        taskStore.beginTX(status -> {
            int count = 0;
            final Date dbNow = taskStore.currentDate();
            // 新增 job
            count += taskStore.addJob(job);
            // 新增 job_trigger
            jobTrigger.setJobId(job.getId());
            if (jobTrigger.getStartTime() == null || jobTrigger.getStartTime().compareTo(dbNow) < 0) {
                jobTrigger.setStartTime(JobTriggerUtils.nextSecond(dbNow));
            }
            final Date nextFireTime = JobTriggerUtils.getNextFireTime(jobTrigger);
            jobTrigger.setNextFireTime(nextFireTime);
            count += taskStore.addJobTrigger(jobTrigger);
            addJobRes.setJob(job);
            addJobRes.setJobTrigger(jobTrigger);
            if (jobModel instanceof HttpJobModel) {
                // 新增 http_job
                HttpJobModel httpJobModel = (HttpJobModel) jobModel;
                TaskHttpJob httpJob = httpJobModel.toJobEntity();
                httpJob.setNamespace(scheduler.getNamespace());
                httpJob.setJobId(job.getId());
                count += taskStore.addHttpJob(httpJob);
                addJobRes.setHttpJob(httpJob);
            } else if (jobModel instanceof JavaJobModel) {
                // 新增 java_job
                JavaJobModel javaJobModel = (JavaJobModel) jobModel;
                TaskJavaJob javaJob = javaJobModel.toJobEntity();
                javaJob.setNamespace(scheduler.getNamespace());
                javaJob.setJobId(job.getId());
                count += taskStore.addJavaJob(javaJob);
                addJobRes.setJavaJob(javaJob);
            } else if (jobModel instanceof JsJobModel) {
                // 新增 js_job
                JsJobModel javaJobModel = (JsJobModel) jobModel;
                TaskJsJob jsJob = javaJobModel.toJobEntity();
                jsJob.setNamespace(scheduler.getNamespace());
                jsJob.setJobId(job.getId());
                count += taskStore.addJsJob(jsJob);
                addJobRes.setJsJob(jsJob);
            } else if (jobModel instanceof ShellJobModel) {
                // 新增 shell_job
                ShellJobModel shellJobModel = (ShellJobModel) jobModel;
                TaskShellJob shellJob = shellJobModel.toJobEntity();
                shellJob.setNamespace(scheduler.getNamespace());
                shellJob.setJobId(job.getId());
                count += taskStore.addShellJob(shellJob);
                addJobRes.setShellJob(shellJob);
            } else {
                throw new IllegalArgumentException("不支持的任务类型:" + jobModel.getClass().getName());
            }
            return count;
        });
        return addJobRes;
    }

    /**
     * 禁用定时任务
     */
    public int disableJob(Long jobId) {
        Assert.notNull(jobId, "参数jobId不能为空");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(getNamespace(), EnumConstant.JOB_DISABLE_1, jobId));
    }

    /**
     * 批量禁用定时任务
     */
    public int disableJobs(Collection<Long> jobIds) {
        Assert.notEmpty(jobIds, "参数jobIds不能为空");
        Assert.noNullElements(jobIds, "参数jobIds含有空jobId");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(getNamespace(), EnumConstant.JOB_DISABLE_1, jobIds.toArray(new Long[0])));
    }

    /**
     * 启用定时任务
     */
    public int enableJob(Long jobId) {
        Assert.notNull(jobId, "参数jobId不能为空");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(getNamespace(), EnumConstant.JOB_DISABLE_0, jobId));
    }

    /**
     * 批量启用定时任务
     */
    public int enableJobs(Collection<Long> jobIds) {
        Assert.notEmpty(jobIds, "参数jobIds不能为空");
        Assert.noNullElements(jobIds, "参数jobIds含有空jobId");
        return taskStore.beginTX(status -> taskStore.updateDisableJob(getNamespace(), EnumConstant.JOB_DISABLE_0, jobIds.toArray(new Long[0])));
    }

    /**
     * 禁用触发器
     */
    public int disableTrigger(Long triggerId) {
        return taskStore.beginTX(status -> taskStore.updateDisableTrigger(getNamespace(), EnumConstant.JOB_TRIGGER_DISABLE_1, triggerId));
    }

    /**
     * 批量禁用触发器
     */
    public int disableTriggers(Collection<Long> triggerIds) {
        Assert.notEmpty(triggerIds, "参数triggerIds不能为空");
        Assert.noNullElements(triggerIds, "参数triggerIds含有空triggerId");
        return taskStore.beginTX(status -> taskStore.updateDisableTrigger(getNamespace(), EnumConstant.JOB_TRIGGER_DISABLE_1, triggerIds.toArray(new Long[0])));
    }

    /**
     * 启用触发器
     */
    public int enableTrigger(Long triggerId) {
        return taskStore.beginTX(status -> taskStore.updateDisableTrigger(getNamespace(), EnumConstant.JOB_TRIGGER_DISABLE_0, triggerId));
    }

    /**
     * 批量启用触发器
     */
    public int enableTriggers(Collection<Long> triggerIds) {
        Assert.notEmpty(triggerIds, "参数jobIds不能为空");
        Assert.noNullElements(triggerIds, "参数triggerIds含有空triggerId");
        return taskStore.beginTX(status -> taskStore.updateDisableTrigger(getNamespace(), EnumConstant.JOB_TRIGGER_DISABLE_0, triggerIds.toArray(new Long[0])));
    }

    /**
     * 删除定时任务(同时删除触发器)
     */
    public void deleteJob(Long jobId) {
        Assert.notNull(jobId, "参数jobId不能为空");
        final String namespace = getNamespace();
        taskStore.beginTX(status -> {
            TaskJob job = taskStore.getJob(namespace, jobId);
            Assert.notNull(job, "job不存在");
            taskStore.delJobByJobId(namespace, jobId);
            taskStore.delTriggerByJobId(namespace, jobId);
            switch (job.getType()) {
                case EnumConstant.JOB_TYPE_1:
                    taskStore.delHttpJobByJobId(namespace, jobId);
                    break;
                case EnumConstant.JOB_TYPE_2:
                    taskStore.delJavaJobByJobId(namespace, jobId);
                    break;
                case EnumConstant.JOB_TYPE_3:
                    taskStore.delJsJobByJobId(namespace, jobId);
                    break;
                case EnumConstant.JOB_TYPE_4:
                    taskStore.delShellJobByJobId(namespace, jobId);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的任务类型: Type=" + job.getType());
            }
            return null;
        });
        // TODO 处理程序内部状态
        // taskContext & wheelTimer
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
        final long startTime = System.currentTimeMillis();
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        final TaskJob job = taskStore.beginReadOnlyTX(status -> taskStore.getJob(scheduler.getNamespace(), jobId));
        Assert.notNull(job, "job不存在");
        jobExecutor.execute(() -> {
            final Date dbNow = taskStore.currentDate();
            // 记录触发器日志
            final TaskJobTriggerLog jobTriggerLog = newJobTriggerLog(dbNow, jobId);
            scheduledExecutor.execute(() -> {
                final long endTime = System.currentTimeMillis();
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
                        Long runCount = taskStore.beginReadOnlyTX(status -> taskStore.getJobRunCount(scheduler.getNamespace(), jobId));
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
     * 分页查询定时任务列表
     */
    public void queryJobs() {
        // TODO queryJobs
    }

    /**
     * 获取所有调度器
     */
    public List<SchedulerInfo> allSchedulers() {
        return taskStore.beginReadOnlyTX(status -> taskStore.queryAllSchedulerList(getNamespace()));
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- service

    private void startScheduler() {
        long initialDelay = 0;
        TaskScheduler taskScheduler = registerScheduler(toScheduler(taskContext.getSchedulerConfig()));
        taskContext.setCurrentScheduler(taskScheduler);
        // 调度器节点注册
        registerSchedulerFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        registerScheduler(taskContext.getCurrentScheduler());
                    } catch (Exception e) {
                        log.error("[TaskInstance] 调度器节点注册失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_REGISTER_SCHEDULER_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                GlobalConstant.REGISTER_SCHEDULER_INTERVAL, GlobalConstant.REGISTER_SCHEDULER_INTERVAL, TimeUnit.MILLISECONDS
        );
        // 集群节点心跳保持
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
        // 数据完整性校验、一致性校验
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
        // 初始化触发器下一次触发时间(校准触发器触发时间)
        calcNextFireTimeFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        calcNextFireTime();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 校准触发器触发时间失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_CALC_NEXT_FIRE_TIME_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                initialDelay, GlobalConstant.CALC_NEXT_FIRE_TIME_INTERVAL, TimeUnit.MILLISECONDS
        );
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
        // 维护接下来N秒内需要触发的触发器列表
        reloadNextTriggerFuture = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        reloadNextTrigger();
                    } catch (Exception e) {
                        log.error("[TaskInstance] 维护接下来N秒内需要触发的触发器列表失败 | instanceName={}", getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_RELOAD_NEXT_TRIGGER_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerErrorListener(schedulerLog, e);
                    }
                },
                initialDelay, GlobalConstant.RELOAD_NEXT_TRIGGER_INTERVAL, TimeUnit.MILLISECONDS
        );
    }

    private void stopScheduler() {
        Consumer<ScheduledFuture<?>> stopScheduler = future -> {
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }
        };
        stopScheduler.accept(registerSchedulerFuture);
        stopScheduler.accept(heartbeatFuture);
        stopScheduler.accept(dataCheckFuture);
        stopScheduler.accept(calcNextFireTimeFuture);
        stopScheduler.accept(reloadSchedulerFuture);
        stopScheduler.accept(reloadNextTriggerFuture);
    }

    private void doStart() {
        try {
            startScheduler();
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
            stopScheduler();
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
            startScheduler();
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
            stopScheduler();
            wheelTimer.stop();
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
    private TaskScheduler registerScheduler(TaskScheduler scheduler) {
        return taskStore.beginTX(status -> taskStore.addOrUpdateScheduler(scheduler));
    }

    /**
     * 数据完整性校验、一致性校验
     */
    private void dataCheck() {
        // TODO 数据完整性校验、一致性校验
        // 1.task_http_job、task_java_job、task_js_job、task_shell_job 与 task_job 是否是一对一的关系
        // 2.task_job_trigger 与 task_job 是否是一对一的关系
        // 3. task_job: type、route_strategy、first_scheduler、whitelist_scheduler、blacklist_scheduler、load_balance 字段值的有效性
        // 4.task_http_job: request_method 字段值的有效性
        // 5.task_java_job: class_name、class_method 字段值的有效性
        // 6.task_shell_job: shell_type、shell_charset 字段值的有效性
        // 7.task_job_trigger: misfire_strategy、type、cron、fixed_interval 字段值的有效性
    }

    /**
     * 初始化触发器下一次触发时间(校准触发器触发时间)
     */
    private void calcNextFireTime() {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        // 1.更新无效的Trigger配置
        int invalidCount = taskStore.beginReadOnlyTX(status -> taskStore.countInvalidTrigger(scheduler.getNamespace()));
        int updateCount = taskStore.beginTX(status -> taskStore.updateInvalidTrigger(scheduler.getNamespace()));
        if (updateCount > 0) {
            log.info("[TaskInstance] 更新异常触发器nextFireTime=null | 更新数量：{} | instanceName={}", updateCount, getInstanceName());
        }
        // 全部cron触发器列表
        List<TaskJobTrigger> cronTriggerList = taskStore.beginReadOnlyTX(status -> taskStore.queryEnableCronTrigger(scheduler.getNamespace()));
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
                    taskStore.beginTX(status -> taskStore.updateNextFireTime(cronTrigger));
                }
            }
        }
        // 2.计算触发器下一次触发时间
        // 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
        updateCount = taskStore.beginTX(status -> taskStore.updateNextFireTimeForType2(scheduler.getNamespace()));
        // 更新cron触发器下一次触发时间 -> type=1
        for (TaskJobTrigger cronTrigger : effectiveCronTriggerList) {
            try {
                final Date nextFireTime = JobTriggerUtils.getNextFireTime(cronTrigger);
                if (cronTrigger.getNextFireTime() == null || cronTrigger.getNextFireTime().compareTo(nextFireTime) != 0) {
                    updateCount++;
                    cronTrigger.setNextFireTime(nextFireTime);
                    taskStore.beginTX(status -> taskStore.updateNextFireTime(cronTrigger));
                }
            } catch (Exception e) {
                log.error("[TaskInstance] 计算触发器下一次触发时间失败 | JobTrigger(id={}) | instanceName={}", cronTrigger.getId(), getInstanceName(), e);
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_CALC_CRON_NEXT_FIRE_TIME_ERROR, ExceptionUtils.getStackTraceAsString(e));
                scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
            }
        }
        log.info("[TaskInstance] 更新触发器下一次触发时间nextFireTime字段 | 更新数量：{} | instanceName={}", updateCount, getInstanceName());
        if (invalidCount > 0) {
            log.warn("[TaskInstance] 触发器配置检查完成，异常的触发器数量：{} | instanceName={}", invalidCount, getInstanceName());
        } else {
            log.info("[TaskInstance] 触发器配置检查完成，无异常触发器 | instanceName={}", getInstanceName());
        }
    }

    /**
     * 心跳保持
     */
    private void heartbeat() {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        taskStore.beginTX(status -> taskStore.heartbeat(scheduler));
    }

    /**
     * 维护当前集群可用的调度器列表
     */
    private void reloadScheduler() {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        final List<TaskScheduler> availableSchedulerList = taskStore.beginReadOnlyTX(status -> taskStore.queryAvailableSchedulerList(scheduler.getNamespace()));
        taskContext.setAvailableSchedulerList(availableSchedulerList);
    }

    /**
     * 维护接下来N秒内需要触发的触发器列表
     */
    private void reloadNextTrigger() {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        // noinspection ConstantConditions
        final int nextTime = Conv.asInteger(GlobalConstant.RELOAD_NEXT_TRIGGER_INTERVAL * GlobalConstant.NEXT_TRIGGER_N + 1, 2000);
        final List<TaskJobTrigger> nextJobTriggerList = taskStore.beginReadOnlyTX(status -> taskStore.queryNextTrigger(scheduler.getNamespace(), nextTime));
        final int size = nextJobTriggerList.size();
        // 同一秒触发任务过多打印警告
        if (size > GlobalConstant.NEXT_TRIGGER_MAX_COUNT) {
            log.warn(
                    "[TaskInstance] 接下来{}秒内需要触发的触发器列表超过最大值：{}，当前值：{} | instanceName={}",
                    nextTime, GlobalConstant.NEXT_TRIGGER_MAX_COUNT, size, getInstanceName()
            );
        }
        taskContext.setNextJobTriggerMap(nextJobTriggerList);
    }

    /**
     * 调度器轮询任务(不会并发执行)
     */
    private void triggerJobExec() {
        // TODO 使用时间轮算法改良触发逻辑
        final long startTime = System.currentTimeMillis();
        // 轮询触发 job
        final List<TaskJobTrigger> nextJobTriggerList = taskContext.getNextJobTriggerList();
        final Date dbNow = taskStore.currentDate();
        final List<Future<?>> triggerFutureList = new ArrayList<>();
        for (final TaskJobTrigger jobTrigger : nextJobTriggerList) {
            // 判断触发时间是否已到
            if (dbNow.compareTo(jobTrigger.getNextFireTime()) < 0) {
                continue;
            }
            // 同一秒不触发两次
            if ((dbNow.getTime() / 1000) == (taskContext.getLastTriggerFireTime(jobTrigger.getId()) / 1000)) {
                continue;
            }
            // 判断是否正在触发
            if (!taskContext.addTriggering(jobTrigger)) {
                continue;
            }
            try {
                Future<?> future = scheduledExecutor.submit(() -> {
                    final TaskJobTriggerLog jobTriggerLog = newJobTriggerLog(dbNow, jobTrigger);
                    jobTriggerLog.setFireCount(taskContext.incrementAndGetJobFireCount(jobTrigger.getId()));
                    taskContext.setLastTriggerFireTime(jobTrigger.getId(), dbNow.getTime());
                    final long startFireTime = System.currentTimeMillis();
                    try {
                        // 执行触发逻辑 - 是否允许多节点并行触发
                        final boolean allowConcurrent = Objects.equals(EnumConstant.JOB_TRIGGER_ALLOW_CONCURRENT_1, jobTrigger.getAllowConcurrent());
                        if (allowConcurrent) {
                            doTriggerJobExec(dbNow, jobTrigger, jobTriggerLog);
                        } else {
                            // 获取触发器分布式锁 - 判断是否被其他节点触发了
                            taskStore.getLockTrigger(jobTrigger.getNamespace(), jobTrigger.getId(), () -> {
                                // 二次校验数据
                                Long fireCount = taskStore.beginReadOnlyTX(status -> taskStore.getTriggerFireCount(jobTrigger.getNamespace(), jobTrigger.getId()));
                                if (Objects.equals(fireCount, jobTrigger.getFireCount())) {
                                    doTriggerJobExec(dbNow, jobTrigger, jobTriggerLog);
                                }
                            });
                        }
                        // 是否在当前节点触发执行了任务
                        if (jobTriggerLog.getMisFired() == null) {
                            // 未触发 - 触发次数减1
                            taskContext.decrementAndGetJobFireCount(jobTrigger.getId());
                            jobTriggerLog.setFireCount(jobTriggerLog.getFireCount() - 1);
                        } else {
                            // 已触发 - 触发器触发成功日志(异步)
                            final long endFireTime = System.currentTimeMillis();
                            jobTriggerLog.setTriggerTime((int) (endFireTime - startFireTime));
                            scheduledExecutor.execute(() -> jobTriggeredListener(jobTriggerLog));
                        }
                    } catch (Exception e) {
                        log.error("[TaskInstance] JobTrigger触发失败 | id={} | name={} | instanceName={}", jobTrigger.getId(), jobTrigger.getName(), getInstanceName(), e);
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_JOB_TRIGGER_FIRE_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
                    } finally {
                        taskContext.removeTriggering(jobTrigger);
                    }
                });
                triggerFutureList.add(future);
                log.debug("[TaskInstance] JobTrigger触发完成 | id={} | name={} | instanceName={}", jobTrigger.getId(), jobTrigger.getName(), getInstanceName());
            } catch (Exception e) {
                log.error("[TaskInstance] JobTrigger触发失败 | id={} | name={} | instanceName={}", jobTrigger.getId(), jobTrigger.getName(), getInstanceName(), e);
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_TRIGGER_JOB_EXEC_ITEM_ERROR, ExceptionUtils.getStackTraceAsString(e));
                scheduledExecutor.execute(() -> schedulerErrorListener(schedulerLog, e));
            }
        }
        // 等待触发任务结束(最多等TRIGGER_JOB_EXEC_MAX_INTERVAL毫秒)
        while (true) {
            boolean done = true;
            for (Future<?> future : triggerFutureList) {
                done = future.isDone() || future.isCancelled();
                if (!done) {
                    break;
                }
            }
            if (done || (System.currentTimeMillis() - startTime) >= GlobalConstant.TRIGGER_JOB_EXEC_MAX_INTERVAL) {
                break;
            }
            try {
                // noinspection BusyWait
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                Thread.yield();
            }
        }
        final long endTime = System.currentTimeMillis();
        final long sunFireTime = endTime - startTime;
        if (sunFireTime >= 1000) {
            log.warn("[TaskInstance] 定时任务触发线程完成 | 耗时：{}ms | instanceName={}", sunFireTime, getInstanceName());
        } else {
            log.debug("[TaskInstance] 定时任务触发线程完成 | 耗时：{}ms | instanceName={}", sunFireTime, getInstanceName());
        }
    }

    /**
     * 触发定时任务逻辑
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
        // 3.控制任务执行节点 // TODO 暂不支持控制任务执行节点
        switch (job.getRouteStrategy()) {
            case EnumConstant.JOB_ROUTE_STRATEGY_1:
                // 指定节点优先
                break;
            case EnumConstant.JOB_ROUTE_STRATEGY_2:
                // 固定节点白名单
                break;
            case EnumConstant.JOB_ROUTE_STRATEGY_3:
                // 固定节点黑名单
                break;
        }
        // 4.负载均衡策略 // TODO 暂不支持负载均衡策略
        switch (job.getLoadBalance()) {
            case EnumConstant.JOB_LOAD_BALANCE_1:
                // 抢占
                break;
            case EnumConstant.JOB_LOAD_BALANCE_2:
                // 随机
                break;
            case EnumConstant.JOB_LOAD_BALANCE_3:
                // 轮询
                break;
            case EnumConstant.JOB_LOAD_BALANCE_4:
                // 一致性HASH
                break;
        }
        // 触发定时任务
        boolean needRunJob = true;
        Date lastFireTime = jobTrigger.getNextFireTime();
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
                    lastFireTime = JobTriggerUtils.removeMillisecond(dbNow);
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
        final Date newNextFireTime = JobTriggerUtils.getNextFireTime(dbNow, jobTrigger);
        jobTrigger.setLastFireTime(lastFireTime);
        jobTrigger.setNextFireTime(newNextFireTime);
        // 更新JobTrigger并获取最新的JobTrigger
        final TaskJobTrigger newJobTrigger = taskStore.beginTX(status -> {
            taskStore.updateFireTime(jobTrigger);
            return taskStore.getTrigger(jobTrigger.getNamespace(), jobTrigger.getId());
        });
        if (newJobTrigger == null) {
            taskContext.removeNextJobTrigger(jobTrigger.getId());
        } else {
            taskContext.putNextJobTrigger(newJobTrigger);
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
            final long startTime = System.currentTimeMillis();
            int retryCount = 0;
            while (retryCount < maxRetryCount) {
                retryCount++;
                try {
                    jobExecutor.exec(dbNow, job, scheduler, taskStore);
                    jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_0);
                    break;
                } catch (Exception e) {
                    log.error("[TaskInstance] Job执行失败，重试次数：{} | id={} | name={} | instanceName={}", retryCount, job.getId(), job.getName(), getInstanceName(), e);
                    // 记录任务执行日志(同步)
                    final long endTime = System.currentTimeMillis();
                    jobLog.setRunTime((int) (endTime - startTime));
                    jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_1);
                    jobLog.setRetryCount(retryCount);
                    jobLog.setExceptionInfo(ExceptionUtils.getStackTraceAsString(e));
                    jobRetryRunListener(jobLog, e);
                }
            }
            final long endTime = System.currentTimeMillis();
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
        TaskScheduler.Config config = new TaskScheduler.Config();
        config.setSchedulerExecutorPoolSize(schedulerConfig.getSchedulerExecutorPoolSize());
        config.setJobExecutorPoolSize(schedulerConfig.getJobExecutorPoolSize());
        config.setJobExecutorQueueSize(schedulerConfig.getJobExecutorQueueSize());
        config.setLoadWeight(schedulerConfig.getLoadWeight());
        TaskScheduler scheduler = new TaskScheduler();
        scheduler.setNamespace(schedulerConfig.getNamespace());
        scheduler.setInstanceName(schedulerConfig.getInstanceName());
        scheduler.setHeartbeatInterval(schedulerConfig.getHeartbeatInterval());
        scheduler.setConfig(JacksonMapper.getInstance().toJson(config));
        scheduler.setDescription(schedulerConfig.getDescription());
        return scheduler;
    }

    private TaskSchedulerLog newSchedulerLog() {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        TaskSchedulerLog schedulerLog = new TaskSchedulerLog();
        schedulerLog.setNamespace(scheduler.getNamespace());
        schedulerLog.setInstanceName(scheduler.getInstanceName());
        return schedulerLog;
    }

    // 自动触发
    private TaskJobTriggerLog newJobTriggerLog(Date dbNow, TaskJobTrigger jobTrigger) {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        TaskJobTriggerLog jobTriggerLog = new TaskJobTriggerLog();
        jobTriggerLog.setId(taskStore.getSnowFlake().nextId());
        jobTriggerLog.setNamespace(scheduler.getNamespace());
        jobTriggerLog.setInstanceName(scheduler.getInstanceName());
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
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        TaskJobTriggerLog jobTriggerLog = new TaskJobTriggerLog();
        jobTriggerLog.setId(taskStore.getSnowFlake().nextId());
        jobTriggerLog.setNamespace(scheduler.getNamespace());
        jobTriggerLog.setInstanceName(scheduler.getInstanceName());
        jobTriggerLog.setJobId(jobId);
        jobTriggerLog.setTriggerName("用户手动触发");
        jobTriggerLog.setFireTime(dbNow);
        jobTriggerLog.setIsManual(EnumConstant.JOB_TRIGGER_IS_MANUAL_1);
        jobTriggerLog.setFireCount(-1L);
        jobTriggerLog.setMisFired(EnumConstant.JOB_TRIGGER_MIS_FIRED_0);
        return jobTriggerLog;
    }

    private TaskJobLog newJobLog(Date dbNow, TaskJob job, TaskJobTrigger jobTrigger, Long jobTriggerLogId) {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        TaskJobLog jobLog = new TaskJobLog();
        jobLog.setNamespace(scheduler.getNamespace());
        jobLog.setInstanceName(scheduler.getInstanceName());
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
