package org.clever.task.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import org.clever.task.core.support.*;
import org.clever.util.Assert;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度器实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:38 <br/>
 */
@Slf4j
public class TaskInstance {
    /**
     * 调度器数据存储对象
     */
    @Getter
    private final TaskStore taskStore;
    /**
     * 调度器上下文
     */
    private final TaskContext taskContext;
    /**
     * 调度器状态
     */
    private volatile TaskState taskState = TaskState.None;
    /**
     * 调度器锁
     */
    private final Object schedulerLock = new Object();

    /**
     * 数据完整性校验、一致性校验 (守护线程)
     */
    private final DaemonExecutor dataCheckDaemon;
    /**
     * 调度器节点注册 (守护线程)
     */
    private final DaemonExecutor registerSchedulerDaemon;
    /**
     * 初始化触发器下一次触发时间(校准触发器触发时间) (守护线程)
     */
    private final DaemonExecutor calcNextFireTimeDaemon;
    /**
     * 心跳保持 (守护线程)
     */
    private final DaemonExecutor heartbeatDaemon;
    /**
     * 维护当前集群可用的调度器列表 (守护线程)
     */
    private final DaemonExecutor reloadSchedulerDaemon;
    /**
     * 维护接下来N秒内需要触发的触发器列表 (守护线程)
     */
    private final DaemonExecutor reloadNextTriggerDaemon;
    /**
     * 调度器轮询任务 (守护线程)
     */
    private final DaemonExecutor triggerJobExecDaemon;
    /**
     * 调度工作线程池
     */
    private final WorkExecutor schedulerWorker;
    /**
     * 定时任务执行工作线程池
     */
    private final WorkExecutor jobWorker;
    /**
     * 调度工作线程池 与 校准触发器触发时间守护线程 之间的协调器
     */
    private final Semaphore schedulerCoordinator = new Semaphore(1);
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
        final SnowFlake snowFlake = new SnowFlake(
                Math.abs(schedulerConfig.getInstanceName().hashCode() % SnowFlake.MAX_WORKER_ID),
                Math.abs(schedulerConfig.getNamespace().hashCode() % SnowFlake.MAX_DATACENTER_ID)
        );
        // 初始化数据源
        taskStore = new TaskStore(snowFlake, queryDSL);
        // 注册调度器
        TaskScheduler scheduler = registerScheduler(toScheduler(schedulerConfig));
        taskContext = new TaskContext(schedulerConfig, scheduler, snowFlake);
        // 初始化守护线程池
        dataCheckDaemon = new DaemonExecutor(GlobalConstant.DATA_CHECK_DAEMON_NAME, schedulerConfig.getInstanceName());
        registerSchedulerDaemon = new DaemonExecutor(GlobalConstant.REGISTER_SCHEDULER_DAEMON_NAME, schedulerConfig.getInstanceName());
        calcNextFireTimeDaemon = new DaemonExecutor(GlobalConstant.CALC_NEXT_FIRE_TIME_DAEMON_NAME, schedulerConfig.getInstanceName());
        heartbeatDaemon = new DaemonExecutor(GlobalConstant.HEARTBEAT_DAEMON_NAME, schedulerConfig.getInstanceName());
        reloadSchedulerDaemon = new DaemonExecutor(GlobalConstant.RELOAD_SCHEDULER_DAEMON_NAME, schedulerConfig.getInstanceName());
        reloadNextTriggerDaemon = new DaemonExecutor(GlobalConstant.RELOAD_NEXT_TRIGGER_DAEMON_NAME, schedulerConfig.getInstanceName());
        triggerJobExecDaemon = new DaemonExecutor(GlobalConstant.TRIGGER_JOB_EXEC_DAEMON_NAME, schedulerConfig.getInstanceName());
        // 初始化工作线程池
        schedulerWorker = new WorkExecutor(
                GlobalConstant.SCHEDULER_EXECUTOR_NAME,
                schedulerConfig.getInstanceName(),
                schedulerConfig.getSchedulerExecutorPoolSize(),
                schedulerConfig.getSchedulerExecutorQueueSize()
        );
        jobWorker = new WorkExecutor(
                GlobalConstant.JOB_EXECUTOR_NAME,
                schedulerConfig.getInstanceName(),
                schedulerConfig.getJobExecutorPoolSize(),
                schedulerConfig.getJobExecutorQueueSize()
        );
        // 初始化定时任务执行器实现列表
        jobExecutors.sort(Comparator.comparingInt(JobExecutor::order));
        this.jobExecutors = jobExecutors;
        if (this.jobExecutors.isEmpty()) {
            log.error("[TaskInstance] 定时任务执行器实现列表为空 | instanceName={}", schedulerConfig.getInstanceName());
        }
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            this.jobExecutors.forEach(jobExecutor -> sb.append("\n").append(jobExecutor.getClass().getName()));
            log.info("[TaskInstance] 定时任务执行器实现列表顺序如下 | instanceName={} {}", schedulerConfig.getInstanceName(), sb);
        }
        // 事件监听器
        this.schedulerListeners = schedulerListeners;
        this.jobTriggerListeners = jobTriggerListeners;
        this.jobListeners = jobListeners;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- api

    /**
     * 当前集群 namespace
     */
    public String getNamespace() {
        return taskContext.getCurrentScheduler().getNamespace();
    }

    /**
     * 当前调度器实例名
     */
    public String getInstanceName() {
        return taskContext.getCurrentScheduler().getInstanceName();
    }

    /**
     * 调度器上下文
     */
    public TaskContext getContext() {
        return taskContext;
    }

    /**
     * 当前调度器状态
     */
    public TaskState getTaskState() {
        return taskState;
    }

    /**
     * 同步启动调度器
     */
    public void start() {
        startCheck();
        synchronized (schedulerLock) {
            startCheck();
            final TaskScheduler scheduler = taskContext.getCurrentScheduler();
            // 备份之前的状态
            final TaskState oldState = taskState;
            try {
                // 开始初始化
                taskState = TaskState.Initializing;
                // 1.数据完整性校验、一致性校验
                dataCheckDaemon.scheduleAtFixedRate(
                        () -> {
                            try {
                                dataCheck();
                            } catch (Exception e) {
                                log.error("[TaskInstance] 数据完整性校验失败 | instanceName={}", this.getInstanceName(), e);
                                // 记录调度器日志(异步)
                                TaskSchedulerLog schedulerLog = newSchedulerLog();
                                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_DATA_CHECK_ERROR, ExceptionUtils.getStackTraceAsString(e));
                                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                            }
                        },
                        GlobalConstant.DATA_CHECK_INTERVAL
                );
                // 2.调度器节点注册
                registerSchedulerDaemon.scheduleAtFixedRate(
                        () -> {
                            try {
                                registerScheduler(taskContext.getCurrentScheduler());
                            } catch (Exception e) {
                                log.error("[TaskInstance] 调度器节点注册失败 | instanceName={}", this.getInstanceName(), e);
                                TaskSchedulerLog schedulerLog = newSchedulerLog();
                                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_REGISTER_SCHEDULER_ERROR, ExceptionUtils.getStackTraceAsString(e));
                                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                            }
                        },
                        GlobalConstant.REGISTER_SCHEDULER_INTERVAL
                );
                // 3.初始化触发器下一次触发时间(校准触发器触发时间)
                calcNextFireTimeDaemon.scheduleAtFixedRate(
                        () -> {
                            try {
                                calcNextFireTime();
                            } catch (Exception e) {
                                log.error("[TaskInstance] 校准触发器触发时间失败 | instanceName={}", this.getInstanceName(), e);
                                // 记录调度器日志(异步)
                                TaskSchedulerLog schedulerLog = newSchedulerLog();
                                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_CALC_NEXT_FIRE_TIME_ERROR, ExceptionUtils.getStackTraceAsString(e));
                                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                            }
                        },
                        GlobalConstant.CALC_NEXT_FIRE_TIME_INTERVAL
                );
                // 1.心跳保持
                heartbeatDaemon.scheduleAtFixedRate(
                        () -> {
                            try {
                                heartbeat();
                            } catch (Exception e) {
                                log.error("[TaskInstance] 心跳保持失败 | instanceName={}", this.getInstanceName(), e);
                                // 记录调度器日志(异步)
                                TaskSchedulerLog schedulerLog = newSchedulerLog();
                                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_HEART_BEAT_ERROR, ExceptionUtils.getStackTraceAsString(e));
                                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                            }
                        },
                        scheduler.getHeartbeatInterval()
                );
                // 2.维护当前集群可用的调度器列表
                reloadSchedulerDaemon.scheduleAtFixedRate(
                        () -> {
                            try {
                                reloadScheduler();
                            } catch (Exception e) {
                                log.error("[TaskInstance] 维护当前集群可用的调度器列表失败 | instanceName={}", this.getInstanceName(), e);
                                // 记录调度器日志(异步)
                                TaskSchedulerLog schedulerLog = newSchedulerLog();
                                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_RELOAD_SCHEDULER_ERROR, ExceptionUtils.getStackTraceAsString(e));
                                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                            }
                        },
                        GlobalConstant.RELOAD_SCHEDULER_INTERVAL
                );
                // 3.维护接下来N秒内需要触发的触发器列表
                reloadNextTriggerDaemon.scheduleAtFixedRate(
                        () -> {
                            boolean hasPermit = false;
                            try {
                                hasPermit = schedulerCoordinator.tryAcquire(GlobalConstant.RELOAD_NEXT_TRIGGER_INTERVAL, TimeUnit.MILLISECONDS);
                                reloadNextTrigger();
                            } catch (InterruptedException e) {
                                log.warn("[TaskInstance] 维护接下来N秒内需要触发的触发器列表被中断 | instanceName={}", this.getInstanceName());
                            } catch (Exception e) {
                                log.error("[TaskInstance] 维护接下来N秒内需要触发的触发器列表失败 | instanceName={}", this.getInstanceName(), e);
                                // 记录调度器日志(异步)
                                TaskSchedulerLog schedulerLog = newSchedulerLog();
                                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_RELOAD_NEXT_TRIGGER_ERROR, ExceptionUtils.getStackTraceAsString(e));
                                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                            } finally {
                                if (hasPermit) {
                                    schedulerCoordinator.release();
                                }
                            }
                        },
                        GlobalConstant.RELOAD_NEXT_TRIGGER_INTERVAL
                );
                // 4.调度器轮询任务
                triggerJobExecDaemon.scheduleAtFixedRate(
                        () -> {
                            boolean hasPermit = false;
                            try {
                                hasPermit = schedulerCoordinator.tryAcquire(GlobalConstant.TRIGGER_JOB_EXEC_MAX_INTERVAL, TimeUnit.MILLISECONDS);
                                triggerJobExec();
                            } catch (InterruptedException e) {
                                log.warn("[TaskInstance] 调度器轮询任务被中断 | instanceName={}", this.getInstanceName());
                            } catch (Exception e) {
                                log.error("[TaskInstance] 调度器轮询任务失败 | instanceName={}", this.getInstanceName(), e);
                                // 记录调度器日志(异步)
                                TaskSchedulerLog schedulerLog = newSchedulerLog();
                                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_TRIGGER_JOB_EXEC_ERROR, ExceptionUtils.getStackTraceAsString(e));
                                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                            } finally {
                                if (hasPermit) {
                                    schedulerCoordinator.release();
                                }
                            }
                        },
                        GlobalConstant.TRIGGER_JOB_EXEC_INTERVAL
                );
                // 初始化完成就是运行中
                taskState = TaskState.Running;
                // 调度器启动成功日志(异步)
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventName(TaskSchedulerLog.EVENT_STARTED);
                schedulerWorker.execute(() -> this.schedulerStartedListener(schedulerLog));
            } catch (Exception e) {
                // 异常就还原之前的状态
                taskState = oldState;
                log.error("[TaskInstance] 调度器启动失败 | instanceName={}", this.getInstanceName(), e);
                // 记录调度器日志(异步)
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_STARTED_ERROR, ExceptionUtils.getStackTraceAsString(e));
                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
            }
        }
    }

    /**
     * 异步延时启动调度器
     *
     * @param seconds 延时时间(单位：秒)
     */
    public void startDelayed(int seconds) {
        schedulerWorker.execute(() -> {
            if (seconds > 0) {
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    log.warn("[TaskInstance] 异步延时启动，延时失败 | instanceName={}", this.getInstanceName(), e);
                }
            }
            try {
                start();
            } catch (Exception e) {
                log.error("[TaskInstance] 异步延时启动失败 | instanceName={}", this.getInstanceName(), e);
            }
        });
    }

    /**
     * 暂停调度器
     */
    public void pause() {
        if (pauseCheck()) {
            return;
        }
        synchronized (schedulerLock) {
            if (pauseCheck()) {
                return;
            }
            try {
                dataCheckDaemon.stop();
                registerSchedulerDaemon.stop();
                calcNextFireTimeDaemon.stop();
                heartbeatDaemon.stop();
                reloadSchedulerDaemon.stop();
                reloadNextTriggerDaemon.stop();
                triggerJobExecDaemon.stop();
                // 调度器暂停成功日志(异步)
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventName(TaskSchedulerLog.EVENT_PAUSED);
                schedulerWorker.execute(() -> this.schedulerPausedListener(schedulerLog));
            } catch (Exception e) {
                log.error("[TaskInstance] 暂停调度器失败 | instanceName={}", this.getInstanceName(), e);
                // 记录调度器日志(异步)
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_PAUSED_ERROR, ExceptionUtils.getStackTraceAsString(e));
                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
            } finally {
                taskState = TaskState.Pause;
            }
        }
    }

    /**
     * 停止调度器
     */
    public void stop() {
        if (stopCheck()) {
            return;
        }
        synchronized (schedulerLock) {
            if (stopCheck()) {
                return;
            }
            try {
                dataCheckDaemon.shutdown();
                registerSchedulerDaemon.shutdown();
                calcNextFireTimeDaemon.shutdown();
                heartbeatDaemon.shutdown();
                reloadSchedulerDaemon.shutdown();
                reloadNextTriggerDaemon.shutdown();
                triggerJobExecDaemon.shutdown();
                schedulerWorker.shutdown();
                jobWorker.shutdown();
                // 调度器停止日志
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventName(TaskSchedulerLog.EVENT_SHUTDOWN);
                this.schedulerPausedListener(schedulerLog);
            } catch (Exception e) {
                log.error("[TaskInstance] 停止调度器失败 | instanceName={}", this.getInstanceName(), e);
                // 调度器停止日志
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_SHUTDOWN, ExceptionUtils.getStackTraceAsString(e));
                this.schedulerErrorListener(schedulerLog, e);
            } finally {
                taskState = TaskState.Stopped;
            }
        }
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
     * 批量增加定时任务
     */
    public List<AddJobRes> addJobs(Map<AbstractJob, AbstractTrigger> jobs) {
        Assert.notEmpty(jobs, "参数jobs不能为空");
        Assert.noNullElements(jobs.keySet(), "参数jobs含有空job");
        Assert.noNullElements(jobs.values(), "参数jobs含有空trigger");
        final List<AddJobRes> resList = new ArrayList<>(jobs.size());
        taskStore.beginTX(status -> {
            jobs.forEach((abstractJob, trigger) -> resList.add(this.addJob(abstractJob, trigger)));
            return resList;
        });
        return resList;
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
     * 删除定时任务
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
    }

    /**
     * 批量删除定时任务
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
        jobWorker.execute(() -> {
            final Date dbNow = taskStore.currentDate();
            // 记录触发器日志
            final TaskJobTriggerLog jobTriggerLog = newJobTriggerLog(dbNow, jobId);
            schedulerWorker.execute(() -> {
                final long endTime = System.currentTimeMillis();
                jobTriggerLog.setTriggerTime((int) (endTime - startTime));
                this.jobTriggeredListener(jobTriggerLog);
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
                    taskStore.beginTX2(status -> {
                        // 获取定时任务悲观锁(事务范围控制锁范围) - 判断是否被其他节点执行了
                        boolean lock = taskStore.getLockJob(job.getNamespace(), job.getId(), job.getLockVersion());
                        if (lock) {
                            executeJob(dbNow, job, jobLog);
                        }
                    });
                } catch (Exception e) {
                    log.error("[TaskInstance] 手动执行Job失败 | id={} | name={} | instanceName={}", job.getId(), job.getName(), this.getInstanceName(), e);
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
     * 中断定时任务
     */
    public void interruptJob(Long jobId) {
        // TODO 中断定时任务
    }

    /**
     * 批量中断定时任务
     */
    public void interruptJobs(Collection<Long> jobIds) {
        Assert.notEmpty(jobIds, "参数jobIds不能为空");
        Assert.noNullElements(jobIds, "参数jobIds含有空jobId");
        taskStore.beginTX(status -> {
            jobIds.forEach(this::interruptJob);
            return null;
        });
    }

//    /**
//     * TODO 更新任务信息
//     */
//    public void updateJob() {
//    }
//
//    /**
//     *
//     */
//    public void queryJobs() {
//    }
//

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
     * 获取所有调度器
     */
    public List<SchedulerInfo> allSchedulers() {
        return taskStore.beginReadOnlyTX(status -> taskStore.queryAllSchedulerList(getNamespace()));
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- service

    /**
     * 启动调度器前的校验
     */
    private void startCheck() {
        if (taskState != TaskState.None && taskState != TaskState.Pause) {
            throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", taskState));
        }
    }

    private boolean pauseCheck() {
        return taskState != TaskState.Running;
    }

    private boolean stopCheck() {
        return taskState != TaskState.Running && taskState != TaskState.Pause;
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
            log.info("[TaskInstance] 更新异常触发器nextFireTime=null | 更新数量：{} | instanceName={}", updateCount, this.getInstanceName());
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
                log.error("[TaskInstance] 计算触发器下一次触发时间失败 | JobTrigger(id={}) | instanceName={}", cronTrigger.getId(), this.getInstanceName(), e);
                // 记录调度器日志(异步)
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_CALC_CRON_NEXT_FIRE_TIME_ERROR, ExceptionUtils.getStackTraceAsString(e));
                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
            }
        }
        log.info("[TaskInstance] 更新触发器下一次触发时间nextFireTime字段 | 更新数量：{} | instanceName={}", updateCount, this.getInstanceName());
        if (invalidCount > 0) {
            log.warn("[TaskInstance] 触发器配置检查完成，异常的触发器数量：{} | instanceName={}", invalidCount, this.getInstanceName());
        } else {
            log.info("[TaskInstance] 触发器配置检查完成，无异常触发器 | instanceName={}", this.getInstanceName());
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
            log.warn("[TaskInstance] 接下来{}秒内需要触发的触发器列表超过最大值：{}，当前值：{} | instanceName={}", nextTime, GlobalConstant.NEXT_TRIGGER_MAX_COUNT, size, this.getInstanceName());
        }
        taskContext.setNextJobTriggerMap(nextJobTriggerList);
    }

    /**
     * 调度器轮询任务(不会并发执行)
     */
    private void triggerJobExec() {
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
                Future<?> future = schedulerWorker.submit(() -> {
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
                            taskStore.beginTX2(status -> {
                                // 获取触发器悲观锁(事务范围控制锁范围) - 判断是否被其他节点触发了
                                boolean lock = taskStore.getLockTrigger(jobTrigger.getNamespace(), jobTrigger.getId(), jobTrigger.getLockVersion());
                                if (lock) {
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
                            schedulerWorker.execute(() -> this.jobTriggeredListener(jobTriggerLog));
                        }
                    } catch (Exception e) {
                        log.error("[TaskInstance] JobTrigger触发失败 | id={} | name={} | instanceName={}", jobTrigger.getId(), jobTrigger.getName(), this.getInstanceName(), e);
                        // 记录调度器日志(异步)
                        TaskSchedulerLog schedulerLog = newSchedulerLog();
                        schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_JOB_TRIGGER_FIRE_ERROR, ExceptionUtils.getStackTraceAsString(e));
                        schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
                    } finally {
                        taskContext.removeTriggering(jobTrigger);
                    }
                });
                triggerFutureList.add(future);
                log.debug("[TaskInstance] JobTrigger触发完成 | id={} | name={} | instanceName={}", jobTrigger.getId(), jobTrigger.getName(), this.getInstanceName());
            } catch (Exception e) {
                log.error("[TaskInstance] JobTrigger触发失败 | id={} | name={} | instanceName={}", jobTrigger.getId(), jobTrigger.getName(), this.getInstanceName(), e);
                // 记录调度器日志(异步)
                TaskSchedulerLog schedulerLog = newSchedulerLog();
                schedulerLog.setEventInfo(TaskSchedulerLog.EVENT_TRIGGER_JOB_EXEC_ITEM_ERROR, ExceptionUtils.getStackTraceAsString(e));
                schedulerWorker.execute(() -> this.schedulerErrorListener(schedulerLog, e));
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
            log.warn("[TaskInstance] 定时任务触发线程完成 | 耗时：{}ms | instanceName={}", sunFireTime, this.getInstanceName());
        } else {
            log.debug("[TaskInstance] 定时任务触发线程完成 | 耗时：{}ms | instanceName={}", sunFireTime, this.getInstanceName());
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
            jobWorker.execute(() -> {
                final TaskJobLog jobLog = newJobLog(dbNow, job, jobTrigger, jobTriggerLog.getId());
                final String oldJobData = job.getJobData();
                // 控制并发执行 - 是否允许多节点并发执行
                final boolean allowConcurrent = Objects.equals(EnumConstant.JOB_ALLOW_CONCURRENT_1, job.getAllowConcurrent());
                if (allowConcurrent) {
                    executeJob(dbNow, job, jobLog);
                } else {
                    try {
                        taskStore.beginTX2(status -> {
                            // 获取定时任务悲观锁(事务范围控制锁范围) - 判断是否被其他节点执行了
                            boolean lock = taskStore.getLockJob(job.getNamespace(), job.getId(), job.getLockVersion());
                            if (lock) {
                                executeJob(dbNow, job, jobLog);
                            }
                        });
                    } catch (Exception e) {
                        log.error("[TaskInstance] Job执行失败 | id={} | name={} | instanceName={}", job.getId(), job.getName(), this.getInstanceName(), e);
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
                    log.error("[TaskInstance] Job执行失败，重试次数：{} | id={} | name={} | instanceName={}", retryCount, job.getId(), job.getName(), this.getInstanceName(), e);
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
            log.error("[TaskInstance] Job执行失败 | id={} | name={} | instanceName={}", job.getId(), job.getName(), this.getInstanceName(), e);
            jobLog.setStatus(EnumConstant.JOB_LOG_STATUS_1);
            jobLog.setExceptionInfo(ExceptionUtils.getStackTraceAsString(e));
        } finally {
            // 任务执行事件处理
            taskContext.decrementAndGetJobReentryCount(job.getId());
            jobEndRunListener(jobLog);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- listeners

    /**
     * 调度器启动完成
     */
    public void schedulerStartedListener(TaskSchedulerLog schedulerLog) {
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
                log.error("[TaskInstance] 调度器启动完成事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), this.getInstanceName(), e);
            }
        }
    }

    /**
     * 调度器已停止
     */
    public void schedulerPausedListener(TaskSchedulerLog schedulerLog) {
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
                log.error("[TaskInstance] 调度器已停止事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), this.getInstanceName(), e);
            }
        }
    }

    /**
     * 调度器出现错误
     */
    public void schedulerErrorListener(TaskSchedulerLog schedulerLog, Exception error) {
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
                log.error("[TaskInstance] 调度器出现错误事件处理失败 | schedulerListener={} | instanceName={}", schedulerListener.getClass().getName(), this.getInstanceName(), e);
            }
        }
    }

    /**
     * 触发成功
     */
    public void jobTriggeredListener(TaskJobTriggerLog jobTriggerLog) {
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
                log.error("[TaskInstance] 触发器触发成功事件处理失败 | schedulerListener={} | instanceName={}", jobTriggerListener.getClass().getName(), this.getInstanceName(), e);
            }
        }
    }

    /**
     * 开始执行
     */
    public void jobStartRunListener(TaskJobLog jobLog) {
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
                log.error("[TaskInstance] 任务开始执行事件处理失败 | schedulerListener={} | instanceName={}", jobListener.getClass().getName(), this.getInstanceName(), e);
            }
        }
    }

    /**
     * 执行完成(成功或者失败)
     */
    public void jobEndRunListener(TaskJobLog jobLog) {
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
                log.error("[TaskInstance] 任务执行完成事件处理失败 | schedulerListener={} | instanceName={}", jobListener.getClass().getName(), this.getInstanceName(), e);
            }
        }
    }

    /**
     * 重试执行
     */
    public void jobRetryRunListener(TaskJobLog jobLog, Exception error) {
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
                log.error("[TaskInstance] 任务重试执行事件处理失败 | schedulerListener={} | instanceName={}", jobListener.getClass().getName(), this.getInstanceName(), e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- support

    /**
     * SchedulerConfig 转换成 Scheduler
     */
    private TaskScheduler toScheduler(SchedulerConfig schedulerConfig) {
        TaskScheduler.Config config = new TaskScheduler.Config();
        config.setSchedulerExecutorPoolSize(schedulerConfig.getSchedulerExecutorPoolSize());
        config.setSchedulerExecutorQueueSize(schedulerConfig.getSchedulerExecutorQueueSize());
        config.setJobExecutorQueueSize(schedulerConfig.getJobExecutorQueueSize());
        config.setJobExecutorPoolSize(schedulerConfig.getJobExecutorPoolSize());
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

    /**
     * 自动触发
     */
    private TaskJobTriggerLog newJobTriggerLog(Date dbNow, TaskJobTrigger jobTrigger) {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        TaskJobTriggerLog jobTriggerLog = new TaskJobTriggerLog();
        jobTriggerLog.setId(taskContext.getSnowFlake().nextId());
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

    /**
     * 手动触发
     */
    private TaskJobTriggerLog newJobTriggerLog(Date dbNow, Long jobId) {
        final TaskScheduler scheduler = taskContext.getCurrentScheduler();
        TaskJobTriggerLog jobTriggerLog = new TaskJobTriggerLog();
        jobTriggerLog.setId(taskContext.getSnowFlake().nextId());
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
