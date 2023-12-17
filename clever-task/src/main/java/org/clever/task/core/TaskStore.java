package org.clever.task.core;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.DateUtils;
import org.clever.core.exception.BusinessException;
import org.clever.core.id.SnowFlake;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.Page;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.jdbc.querydsl.utils.QueryDslUtils;
import org.clever.task.TaskDataSource;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.JobInfo;
import org.clever.task.core.model.JobLogInfo;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.model.entity.*;
import org.clever.task.core.model.request.*;
import org.clever.task.core.model.response.*;
import org.clever.task.core.support.DataBaseClock;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.clever.task.core.model.query.QTaskHttpJob.taskHttpJob;
import static org.clever.task.core.model.query.QTaskJavaJob.taskJavaJob;
import static org.clever.task.core.model.query.QTaskJob.taskJob;
import static org.clever.task.core.model.query.QTaskJobLog.taskJobLog;
import static org.clever.task.core.model.query.QTaskJobTrigger.taskJobTrigger;
import static org.clever.task.core.model.query.QTaskJobTriggerLog.taskJobTriggerLog;
import static org.clever.task.core.model.query.QTaskJsJob.taskJsJob;
import static org.clever.task.core.model.query.QTaskScheduler.taskScheduler;
import static org.clever.task.core.model.query.QTaskSchedulerLog.taskSchedulerLog;
import static org.clever.task.core.model.query.QTaskShellJob.taskShellJob;

/**
 * 定时任务调度器数据存储
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 16:14 <br/>
 */
@Slf4j
public class TaskStore {
    @Getter
    private final SnowFlake snowFlake;
    private final Jdbc jdbc;
    private final QueryDSL queryDSL;
    @Getter
    private final DataBaseClock clock;

    public TaskStore(SnowFlake snowFlake, QueryDSL queryDSL) {
        Assert.notNull(snowFlake, "参数 snowFlake 不能为null");
        Assert.notNull(queryDSL, "参数 queryDSL 不能为null");
        this.snowFlake = snowFlake;
        this.queryDSL = queryDSL;
        this.jdbc = queryDSL.getJdbc();
        this.clock = new DataBaseClock(this.jdbc);
    }

    public TaskStore(SnowFlake snowFlake) {
        this(snowFlake, TaskDataSource.getQueryDSL());
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- dao

    /**
     * 获取数据库当前时间戳(毫秒)
     */
    public long currentTimeMillis() {
        return clock.currentTimeMillis();
    }

    /**
     * 获取数据库当前时间戳(精确到毫秒)
     */
    public Date currentDate() {
        return new Date(clock.currentTimeMillis());
    }

    /**
     * 更新或保存 Scheduler
     */
    public TaskScheduler addOrUpdateScheduler(TaskScheduler scheduler) {
        List<TaskScheduler> schedulerList = queryDSL.select(taskScheduler)
            .from(taskScheduler)
            .where(taskScheduler.namespace.eq(scheduler.getNamespace()))
            .where(taskScheduler.instanceName.eq(scheduler.getInstanceName()))
            .fetch();
        if (schedulerList.size() > 1) {
            throw new SchedulerException(String.format(
                "集群[namespace=%s]的调度器实例[instanceName=%s]存在多个",
                scheduler.getNamespace(),
                scheduler.getInstanceName()
            ));
        }
        TaskScheduler registered = null;
        if (!schedulerList.isEmpty()) {
            registered = schedulerList.get(0);
        }
        final Date now = currentDate();
        if (registered == null) {
            // 需要注册
            scheduler.setId(snowFlake.nextId());
            scheduler.setLastHeartbeatTime(now);
            scheduler.setCreateAt(now);
            queryDSL.insert(taskScheduler).populate(scheduler).execute();
        } else {
            // 需要更新
            queryDSL.update(taskScheduler)
                // .set(taskScheduler.lastHeartbeatTime, now)
                .set(taskScheduler.heartbeatInterval, scheduler.getHeartbeatInterval())
                .set(taskScheduler.config, scheduler.getConfig())
                .set(taskScheduler.description, scheduler.getDescription())
                .set(taskScheduler.updateAt, now)
                .where(taskScheduler.namespace.eq(scheduler.getNamespace()))
                .where(taskScheduler.instanceName.eq(scheduler.getInstanceName()))
                .execute();
        }
        // 查询
        schedulerList = queryDSL.select(taskScheduler)
            .from(taskScheduler)
            .where(taskScheduler.namespace.eq(scheduler.getNamespace()))
            .where(taskScheduler.instanceName.eq(scheduler.getInstanceName()))
            .fetch();
        if (schedulerList.isEmpty()) {
            throw new SchedulerException(String.format(
                "调度器注册失败[namespace=%s, instanceName=%s]",
                scheduler.getNamespace(),
                scheduler.getInstanceName()
            ));
        }
        registered = schedulerList.get(0);
        return registered;
    }

    /**
     * 更新心跳时间
     */
    public int heartbeat(TaskScheduler scheduler) {
        final Date now = currentDate();
        int count = (int) queryDSL.update(taskScheduler)
            .set(taskScheduler.lastHeartbeatTime, now)
            .set(taskScheduler.heartbeatInterval, scheduler.getHeartbeatInterval())
            .set(taskScheduler.config, scheduler.getConfig())
            .set(taskScheduler.description, scheduler.getDescription())
            .where(taskScheduler.namespace.eq(scheduler.getNamespace()))
            .where(taskScheduler.instanceName.eq(scheduler.getInstanceName()))
            .execute();
        if (count != 1) {
            throw new SchedulerException(String.format(
                "心跳维持失败[namespace=%s, instanceName=%s]",
                scheduler.getNamespace(),
                scheduler.getInstanceName()
            ));
        }
        return count;
    }

    /**
     * 查询集群中在线的调度器列表
     */
    public List<TaskScheduler> queryAvailableSchedulerList(String namespace) {
        // heartbeat_interval * 2 > now - last_heartbeat_time
        BooleanExpression whereCondition = taskScheduler.heartbeatInterval.multiply(2).gt(
            Expressions.numberOperation(
                Long.TYPE, Ops.DateTimeOps.DIFF_SECONDS, taskScheduler.lastHeartbeatTime, Expressions.currentTimestamp()
            ).multiply(1000)
        );
        return queryDSL.select(taskScheduler)
            .from(taskScheduler)
            .where(taskScheduler.namespace.eq(namespace))
            .where(taskScheduler.lastHeartbeatTime.isNotNull())
            .where(whereCondition)
            .fetch();
    }

    /**
     * 所有调度器
     */
    public List<SchedulerInfo> queryAllSchedulerList(String namespace) {
        // heartbeat_interval * 2 > now - last_heartbeat_time
        BooleanExpression available = taskScheduler.heartbeatInterval.multiply(2).gt(
            Expressions.numberOperation(
                Long.TYPE, Ops.DateTimeOps.DIFF_SECONDS, taskScheduler.lastHeartbeatTime, Expressions.currentTimestamp()
            ).multiply(1000)
        ).as("available");
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskScheduler, available).from(taskScheduler);
        if (StringUtils.isNotBlank(namespace)) {
            sqlQuery.where(taskScheduler.namespace.eq(namespace));
        }
        List<Tuple> list = sqlQuery
            .orderBy(taskScheduler.namespace.asc())
            .orderBy(taskScheduler.instanceName.asc())
            .fetch();
        return list.stream().map(tuple -> {
            TaskScheduler scheduler = tuple.get(taskScheduler);
            Assert.notNull(scheduler, "scheduler 不能为 null, 未知的错误");
            SchedulerInfo info = new SchedulerInfo();
            info.setId(scheduler.getId());
            info.setNamespace(scheduler.getNamespace());
            info.setInstanceName(scheduler.getInstanceName());
            info.setAvailable(tuple.get(available));
            info.setLastHeartbeatTime(scheduler.getLastHeartbeatTime());
            info.setHeartbeatInterval(scheduler.getHeartbeatInterval());
            info.setConfig(scheduler.getConfig());
            info.setRuntimeInfo(scheduler.getRuntimeInfo());
            info.setDescription(scheduler.getDescription());
            info.setCreateAt(scheduler.getCreateAt());
            info.setUpdateAt(scheduler.getUpdateAt());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 获取无效的触发器配置数量 -> type=2|3
     */
    public int countInvalidTrigger(String namespace) {
        return (int) queryDSL.select(taskJobTrigger)
            .from(taskJobTrigger)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.type.eq(EnumConstant.JOB_TRIGGER_TYPE_2).and(taskJobTrigger.fixedInterval.loe(0)))
            .where(taskJobTrigger.namespace.eq(namespace))
            .fetchCount();
    }

    /**
     * 查询集群中启用的触发器列表
     */
    public List<TaskJobTrigger> queryEnableTrigger(String namespace) {
        return queryDSL.select(taskJobTrigger)
            .from(taskJobTrigger)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.namespace.eq(namespace))
            .fetch();
    }

    /**
     * 查询集群中启用的cron触发器列表
     */
    public List<TaskJobTrigger> queryEnableCronTrigger(String namespace) {
        return queryDSL.select(taskJobTrigger)
            .from(taskJobTrigger)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.type.eq(EnumConstant.JOB_TRIGGER_TYPE_1))
            .where(taskJobTrigger.namespace.eq(namespace))
            .fetch();
    }

    /**
     * 接下来(N+M)秒内需要触发的触发器列表(根据“下一次触发时间”有小到大排序返回)
     *
     * @param nextTime (N+M)秒对应的毫秒时间
     */
    public List<TaskJobTrigger> queryNextTrigger(String namespace, int nextTime) {
        final Date now = currentDate();
        return queryDSL.select(taskJobTrigger)
            .from(taskJobTrigger)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.namespace.eq(namespace))
            .where(taskJobTrigger.startTime.loe(now))
            .where(taskJobTrigger.endTime.isNull().or(taskJobTrigger.endTime.goe(now)))
            .where(taskJobTrigger.nextFireTime.isNotNull())
            // next_fire_time - now <= nextTime --> next_fire_time <= now + nextTime
            .where(taskJobTrigger.nextFireTime.loe(DateUtils.addMilliseconds(now, nextTime)))
            .orderBy(taskJobTrigger.nextFireTime.asc())
            .limit(2000)
            .fetch();
    }

    /**
     * 根据 namespace jobTriggerId 查询启用状态的 Trigger
     */
    public TaskJobTrigger getTrigger(String namespace, Long jobTriggerId) {
        return queryDSL.select(taskJobTrigger)
            .from(taskJobTrigger)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.namespace.eq(namespace))
            .where(taskJobTrigger.id.eq(jobTriggerId))
            .fetchOne();
    }

    public Long getTriggerFireCount(String namespace, Long jobTriggerId) {
        return queryDSL.select(taskJobTrigger.fireCount)
            .from(taskJobTrigger)
            .where(taskJobTrigger.namespace.eq(namespace))
            .where(taskJobTrigger.id.eq(jobTriggerId))
            .fetchOne();
    }

    /**
     * 获取定时任务锁
     *
     * @param namespace 命名空间
     * @param jobId     任务ID
     * @param syncBlock 同步回调函数(可保证分布式串行执行): () -> { ... }
     */
    public void getLockJob(String namespace, Long jobId, Runnable syncBlock) {
        lock(namespace, String.format("job_%s", jobId), syncBlock);
    }

    /**
     * 更新job Data
     */
    public int updateJodData(String namespace, Long jobId, String jobData) {
        return (int) queryDSL.update(taskJob)
            .set(taskJob.jobData, jobData)
            .where(taskJob.namespace.eq(namespace))
            .where(taskJob.id.eq(jobId))
            .execute();
    }

    /**
     * 根据 namespace jobId 查询
     */
    public TaskJob getJob(String namespace, Long jobId) {
        return queryDSL.select(taskJob)
            .from(taskJob)
            .where(taskJob.namespace.eq(namespace))
            .where(taskJob.id.eq(jobId))
            .fetchOne();
    }

    /**
     * 根据 namespace jobId 查询 runCount
     */
    public Long getJobRunCount(String namespace, Long jobId) {
        return queryDSL.select(taskJob.runCount)
            .from(taskJob)
            .where(taskJob.namespace.eq(namespace))
            .where(taskJob.id.eq(jobId))
            .fetchOne();
    }

    /**
     * 根据 namespace jobId 更新 runCount
     */
    public int updateJobRunCount(String namespace, Long jobId) {
        return (int) queryDSL.update(taskJob)
            .set(taskJob.runCount, taskJob.runCount.add(1))
            .where(taskJob.namespace.eq(namespace))
            .where(taskJob.id.eq(jobId))
            .execute();
    }

    /**
     * 查询当前集群所有定时任务信息
     */
    public List<TaskJob> queryAllJob(String namespace) {
        return queryDSL.select(taskJob)
            .from(taskJob)
            .where(taskJob.namespace.eq(namespace))
            .fetch();
    }

    /**
     * 获取HttpJob
     */
    public TaskHttpJob getHttpJob(String namespace, Long jobId) {
        return queryDSL.select(taskHttpJob)
            .from(taskHttpJob)
            .where(taskHttpJob.namespace.eq(namespace))
            .where(taskHttpJob.jobId.eq(jobId))
            .fetchOne();
    }

    /**
     * 获取JavaJob
     */
    public TaskJavaJob getJavaJob(String namespace, Long jobId) {
        return queryDSL.select(taskJavaJob)
            .from(taskJavaJob)
            .where(taskJavaJob.namespace.eq(namespace))
            .where(taskJavaJob.jobId.eq(jobId))
            .fetchOne();
    }

    /**
     * 获取JsJob
     */
    public TaskJsJob getJsJob(String namespace, Long jobId) {
        return queryDSL.select(taskJsJob)
            .from(taskJsJob)
            .where(taskJsJob.namespace.eq(namespace))
            .where(taskJsJob.jobId.eq(jobId))
            .fetchOne();
    }

    /**
     * 获取ShellJob
     */
    public TaskShellJob getShellJob(String namespace, Long jobId) {
        return queryDSL.select(taskShellJob)
            .from(taskShellJob)
            .where(taskShellJob.namespace.eq(namespace))
            .where(taskShellJob.jobId.eq(jobId))
            .fetchOne();
    }

    /**
     * 更新无效的触发器配置 -> type=2|3 更新 next_fire_time=null
     */
    public int updateInvalidTrigger(String namespace) {
        return (int) queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.nextFireTime, Expressions.nullExpression())
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.type.eq(EnumConstant.JOB_TRIGGER_TYPE_2).and(taskJobTrigger.fixedInterval.loe(0)))
            .where(taskJobTrigger.namespace.eq(namespace))
            .execute();
    }

    /**
     * 更新触发器下一次触发时间
     */
    public int updateNextFireTime(TaskJobTrigger jobTrigger) {
        int count = (int) queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.nextFireTime, jobTrigger.getNextFireTime())
            .where(taskJobTrigger.id.eq(jobTrigger.getId()))
            .where(taskJobTrigger.namespace.eq(jobTrigger.getNamespace()))
            .execute();
        if (count != 1) {
            throw new SchedulerException(String.format("更新触发器下一次触发时间失败，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        return count;
    }

    /**
     * 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
     */
    public int updateNextFireTimeForType2(String namespace) {
        // "  case " +
        // "    when isnull(last_fire_time) then date_add(start_time, interval fixed_interval second) " +
        // "    when timestampdiff(microsecond, last_fire_time, start_time)>=0 then date_add(start_time, interval fixed_interval second) " +
        // "    when timestampdiff(microsecond, last_fire_time, start_time)<0 then date_add(last_fire_time, interval fixed_interval second) " +
        // "    else date_add(now(), interval fixed_interval second) " +
        // "  end " +
        DateTimeExpression<Date> nextFireTime = Expressions.cases()
            .when(taskJobTrigger.lastFireTime.isNull()) //
            .then(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, taskJobTrigger.startTime, taskJobTrigger.fixedInterval)) //
            .when(taskJobTrigger.lastFireTime.goe(taskJobTrigger.startTime)) //
            .then(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, taskJobTrigger.startTime, taskJobTrigger.fixedInterval)) //
            .when(taskJobTrigger.lastFireTime.lt(taskJobTrigger.startTime)) //
            .then(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, taskJobTrigger.lastFireTime, taskJobTrigger.fixedInterval)) //
            .otherwise(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, Expressions.currentTimestamp(), taskJobTrigger.fixedInterval));
        return (int) queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.nextFireTime, nextFireTime)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.type.eq(EnumConstant.JOB_TRIGGER_TYPE_2))
            .where(taskJobTrigger.fixedInterval.gt(0))
            .where(taskJobTrigger.nextFireTime.isNotNull().or(taskJobTrigger.nextFireTime.ne(nextFireTime)))
            .where(taskJobTrigger.namespace.eq(namespace))
            .execute();
    }

    /**
     * 更新触发器“上一次触发时间”、“下一次触发时间”
     */
    public long updateFireTime(TaskJobTrigger jobTrigger) {
        return queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.lastFireTime, jobTrigger.getLastFireTime())
            .set(taskJobTrigger.nextFireTime, jobTrigger.getNextFireTime())
            .set(taskJobTrigger.fireCount, taskJobTrigger.fireCount.add(1))
            .where(taskJobTrigger.id.eq(jobTrigger.getId()))
            .where(taskJobTrigger.namespace.eq(jobTrigger.getNamespace()))
            .execute();
    }

    /**
     * 获取触发器锁
     *
     * @param namespace    命名空间
     * @param jobTriggerId 任务触发器ID
     * @param syncBlock    同步回调函数(可保证分布式串行执行): () -> { ... }
     */
    public void getLockTrigger(String namespace, Long jobTriggerId, Runnable syncBlock) {
        lock(namespace, String.format("job_trigger_%s", jobTriggerId), syncBlock);
    }

    public int addSchedulerLog(TaskSchedulerLog schedulerLog) {
        schedulerLog.setId(snowFlake.nextId());
        schedulerLog.setCreateAt(currentDate());
        return (int) queryDSL.insert(taskSchedulerLog).populate(schedulerLog).execute();
    }

    public void addJobTriggerLogs(List<TaskJobTriggerLog> jobTriggerLogs) {
        if (jobTriggerLogs == null || jobTriggerLogs.isEmpty()) {
            return;
        }
        SQLInsertClause insert = queryDSL.insert(taskJobTriggerLog);
        for (TaskJobTriggerLog jobTriggerLog : jobTriggerLogs) {
            jobTriggerLog.setId(snowFlake.nextId());
            insert.populate(jobTriggerLog).addBatch();
        }
        insert.execute();
    }

    public void addJobLogs(List<TaskJobLog> jobLogs) {
        if (jobLogs == null || jobLogs.isEmpty()) {
            return;
        }
        SQLInsertClause insert = queryDSL.insert(taskJobLog);
        for (TaskJobLog jobLog : jobLogs) {
            jobLog.setId(snowFlake.nextId());
            jobLog.setEndTime(null);
            jobLog.setRunTime(null);
            jobLog.setStatus(null);
            insert.populate(jobLog).addBatch();
        }
        insert.execute();
    }

    public void updateJobLogsByEnd(List<TaskJobLog> jobLogs) {
        if (jobLogs == null || jobLogs.isEmpty()) {
            return;
        }
        SQLUpdateClause update = queryDSL.update(taskJobLog);
        for (TaskJobLog jobLog : jobLogs) {
            update.set(taskJobLog.endTime, jobLog.getEndTime())
                .set(taskJobLog.runTime, jobLog.getRunTime())
                .set(taskJobLog.status, jobLog.getStatus())
                .set(taskJobLog.exceptionInfo, jobLog.getExceptionInfo())
                .set(taskJobLog.afterJobData, jobLog.getAfterJobData())
                .where(taskJobLog.namespace.eq(jobLog.getNamespace()))
                .where(taskJobLog.id.eq(jobLog.getId()))
                .addBatch();
        }
        update.execute();
    }

    public void updateJobLogByRetry(List<TaskJobLog> jobLogs) {
        if (jobLogs == null || jobLogs.isEmpty()) {
            return;
        }
        SQLUpdateClause update = queryDSL.update(taskJobLog);
        for (TaskJobLog jobLog : jobLogs) {
            update.set(taskJobLog.runTime, jobLog.getRunTime())
                .set(taskJobLog.status, jobLog.getStatus())
                .set(taskJobLog.exceptionInfo, jobLog.getExceptionInfo())
                .set(taskJobLog.retryCount, jobLog.getRetryCount())
                .where(taskJobLog.namespace.eq(jobLog.getNamespace()))
                .where(taskJobLog.id.eq(jobLog.getId()))
                .addBatch();
        }
        update.execute();
    }

    public int addJobTriggerLog(TaskJobTriggerLog jobTriggerLog) {
        if (jobTriggerLog.getId() == null) {
            jobTriggerLog.setId(snowFlake.nextId());
        }
        jobTriggerLog.setCreateAt(currentDate());
        return (int) queryDSL.insert(taskJobTriggerLog).populate(jobTriggerLog).execute();
    }

    public int addJobLog(TaskJobLog jobLog) {
        jobLog.setId(snowFlake.nextId());
        jobLog.setStartTime(currentDate());
        jobLog.setEndTime(null);
        jobLog.setRunTime(null);
        jobLog.setStatus(null);
        return (int) queryDSL.insert(taskJobLog).populate(jobLog).execute();
    }

    public int updateJobLogByEnd(TaskJobLog jobLog) {
        jobLog.setEndTime(currentDate());
        return (int) queryDSL.update(taskJobLog)
            .set(taskJobLog.endTime, jobLog.getEndTime())
            .set(taskJobLog.runTime, jobLog.getRunTime())
            .set(taskJobLog.status, jobLog.getStatus())
            .set(taskJobLog.exceptionInfo, jobLog.getExceptionInfo())
            .set(taskJobLog.afterJobData, jobLog.getAfterJobData())
            .where(taskJobLog.namespace.eq(jobLog.getNamespace()))
            .where(taskJobLog.id.eq(jobLog.getId()))
            .execute();
    }

    public int updateJobLogByRetry(TaskJobLog jobLog) {
        return (int) queryDSL.update(taskJobLog)
            .set(taskJobLog.runTime, jobLog.getRunTime())
            .set(taskJobLog.status, jobLog.getStatus())
            .set(taskJobLog.exceptionInfo, jobLog.getExceptionInfo())
            .set(taskJobLog.retryCount, jobLog.getRetryCount())
            .where(taskJobLog.namespace.eq(jobLog.getNamespace()))
            .where(taskJobLog.id.eq(jobLog.getId()))
            .execute();
    }

    /**
     * 利用数据库行级锁实现分布式锁
     *
     * @param namespace   命名空间
     * @param lockName    锁名称
     * @param waitSeconds 等待锁的最大时间(小于等于0表示一直等待)
     * @param syncBlock   同步回调函数(可保证分布式串行执行): locked -> { ... }
     * @see Jdbc#tryLock(String, int, Function)
     */
    @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
    private <T> T lock(String namespace, String lockName, int waitSeconds, Function<Boolean, T> syncBlock) {
        Assert.isNotBlank(namespace, "参数 namespace 不能为空");
        Assert.isNotBlank(lockName, "参数 lockName 不能为空");
        Assert.notNull(syncBlock, "参数 syncBlock 不能为空");
        final String nativeLockName = String.format("__inner_task_%s_%s", namespace, lockName);
        if (waitSeconds <= 0) {
            return jdbc.nativeLock(nativeLockName, () -> syncBlock.apply(true));
        } else {
            return jdbc.nativeTryLock(nativeLockName, waitSeconds, syncBlock);
        }
    }

    /**
     * 利用数据库行级锁实现分布式锁
     *
     * @param namespace 命名空间
     * @param lockName  锁名称
     * @param syncBlock 同步回调函数(可保证分布式串行执行): () -> { ... }
     */
    private void lock(String namespace, String lockName, Runnable syncBlock) {
        lock(namespace, lockName, GlobalConstant.CONCURRENT_LOCK_WAIT, locked -> {
            if (locked) {
                syncBlock.run();
            }
            return null;
        });
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- manage

    /**
     * 根据 jobTriggerId 查询
     */
    public TaskJobTrigger getTrigger(Long jobTriggerId) {
        return queryDSL.select(taskJobTrigger)
            .from(taskJobTrigger)
            .where(taskJobTrigger.id.eq(jobTriggerId))
            .fetchOne();
    }

    public Long getTriggerId(Long jobId) {
        return queryDSL.select(taskJobTrigger.id)
            .from(taskJobTrigger)
            .where(taskJobTrigger.jobId.eq(jobId))
            .fetchOne();
    }

    /**
     * 根据 jobId 查询
     */
    public TaskJob getJob(Long jobId) {
        return queryDSL.select(taskJob)
            .from(taskJob)
            .where(taskJob.id.eq(jobId))
            .fetchOne();
    }

    public int updateDisableJob(Integer disable, Long... jobIds) {
        return (int) queryDSL.update(taskJob)
            .set(taskJob.disable, disable)
            .where(taskJob.id.in(jobIds))
            .execute();
    }

    public int updateDisableTrigger(Integer disable, Long... triggerIds) {
        return (int) queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.disable, disable)
            .where(taskJobTrigger.id.in(triggerIds))
            .execute();
    }

    public int addTrigger(TaskJobTrigger jobTrigger) {
        if (jobTrigger.getId() == null) {
            jobTrigger.setId(snowFlake.nextId());
        }
        jobTrigger.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJobTrigger).populate(jobTrigger).execute();
    }

    public int addJob(TaskJob job) {
        if (job.getId() == null) {
            job.setId(snowFlake.nextId());
        }
        job.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJob).populate(job).execute();
    }

    public int addHttpJob(TaskHttpJob httpJob) {
        if (httpJob.getId() == null) {
            httpJob.setId(snowFlake.nextId());
        }
        httpJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskHttpJob).populate(httpJob).execute();
    }

    public int addJavaJob(TaskJavaJob javaJob) {
        if (javaJob.getId() == null) {
            javaJob.setId(snowFlake.nextId());
        }
        javaJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJavaJob).populate(javaJob).execute();
    }

    public int addJsJob(TaskJsJob jsJob) {
        if (jsJob.getId() == null) {
            jsJob.setId(snowFlake.nextId());
        }
        jsJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJsJob).populate(jsJob).execute();
    }

    public int addShellJob(TaskShellJob shellJob) {
        if (shellJob.getId() == null) {
            shellJob.setId(snowFlake.nextId());
        }
        shellJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskShellJob).populate(shellJob).execute();
    }

    public int updateTrigger(TaskJobTrigger jobTrigger) {
        return queryDSL.update(
            taskJobTrigger,
            jobTrigger.getId() != null ? taskJobTrigger.id.eq(jobTrigger.getId()) : taskJobTrigger.jobId.eq(jobTrigger.getJobId()),
            jobTrigger,
            update -> update.set(taskJobTrigger.updateAt, queryDSL.currentDate()),
            taskJobTrigger.id,
            taskJobTrigger.fireCount,
            taskJobTrigger.createAt,
            taskJobTrigger.updateAt
        ) ? 1 : 0;
    }

    public int updateJob(TaskJob job) {
        return queryDSL.update(
            taskJob,
            taskJob.id.eq(job.getId()),
            job,
            update -> update.set(taskJob.updateAt, queryDSL.currentDate()),
            taskJob.id,
            taskJob.runCount,
            taskJob.createAt,
            taskJob.updateAt
        ) ? 1 : 0;
    }

    public int updateHttpJob(TaskHttpJob httpJob) {
        return queryDSL.update(
            taskHttpJob,
            httpJob.getId() != null ? taskHttpJob.id.eq(httpJob.getId()) : taskHttpJob.jobId.eq(httpJob.getJobId()),
            httpJob,
            update -> update.set(taskHttpJob.updateAt, queryDSL.currentDate()),
            taskHttpJob.id,
            taskHttpJob.createAt,
            taskHttpJob.updateAt
        ) ? 1 : 0;
    }

    public int updateJavaJob(TaskJavaJob javaJob) {
        return queryDSL.update(
            taskJavaJob,
            javaJob.getId() != null ? taskJavaJob.id.eq(javaJob.getId()) : taskJavaJob.jobId.eq(javaJob.getJobId()),
            javaJob,
            update -> update.set(taskJavaJob.updateAt, queryDSL.currentDate()),
            taskJavaJob.id,
            taskJavaJob.createAt,
            taskJavaJob.updateAt
        ) ? 1 : 0;
    }

    public int updateJsJob(TaskJsJob jsJob) {
        return queryDSL.update(
            taskJsJob,
            jsJob.getId() != null ? taskJsJob.id.eq(jsJob.getId()) : taskJsJob.jobId.eq(jsJob.getJobId()),
            jsJob,
            update -> update.set(taskJsJob.updateAt, queryDSL.currentDate()),
            taskJsJob.id,
            taskJsJob.createAt,
            taskJsJob.updateAt
        ) ? 1 : 0;
    }

    public int updateShellJob(TaskShellJob shellJob) {
        return queryDSL.update(
            taskShellJob,
            shellJob.getId() != null ? taskShellJob.id.eq(shellJob.getId()) : taskShellJob.jobId.eq(shellJob.getJobId()),
            shellJob,
            update -> update.set(taskShellJob.updateAt, queryDSL.currentDate()),
            taskShellJob.id,
            taskShellJob.createAt,
            taskShellJob.updateAt
        ) ? 1 : 0;
    }

    public void delTriggerByJobId(Long jobId) {
        queryDSL.delete(taskJobTrigger).where(taskJobTrigger.jobId.eq(jobId)).execute();
    }

    public void delJobByJobId(Long jobId) {
        queryDSL.delete(taskJob).where(taskJob.id.eq(jobId)).execute();
    }

    public void delHttpJobByJobId(Long jobId) {
        queryDSL.delete(taskHttpJob).where(taskHttpJob.jobId.eq(jobId)).execute();
    }

    public void delJavaJobByJobId(Long jobId) {
        queryDSL.delete(taskJavaJob).where(taskJavaJob.jobId.eq(jobId)).execute();
    }

    public void delJsJobByJobId(Long jobId) {
        queryDSL.delete(taskJsJob).where(taskJsJob.jobId.eq(jobId)).execute();
    }

    public void delShellJobByJobId(Long jobId) {
        queryDSL.delete(taskShellJob).where(taskShellJob.jobId.eq(jobId)).execute();
    }

    public List<String> allNamespace() {
        return queryDSL.select(taskScheduler.namespace).distinct()
            .from(taskScheduler)
            .orderBy(taskScheduler.namespace.asc())
            .fetch();
    }

    public List<String> allInstance() {
        return queryDSL.select(taskScheduler.instanceName).distinct()
            .from(taskScheduler)
            .orderBy(taskScheduler.instanceName.asc())
            .fetch();
    }

    public Page<TaskSchedulerLog> querySchedulerLog(SchedulerLogReq query) {
        SQLQuery<TaskSchedulerLog> sqlQuery = queryDSL.selectFrom(taskSchedulerLog);
        if (StringUtils.isNotBlank(query.getNamespace())) {
            sqlQuery.where(taskSchedulerLog.namespace.eq(query.getNamespace()));
        }
        if (StringUtils.isNotBlank(query.getInstanceName())) {
            sqlQuery.where(taskSchedulerLog.instanceName.eq(query.getInstanceName()));
        }
        if (StringUtils.isNotBlank(query.getEventName())) {
            sqlQuery.where(taskSchedulerLog.eventName.eq(query.getEventName()));
        }
        if (query.getCreateAtStart() != null) {
            sqlQuery.where(taskSchedulerLog.createAt.goe(query.getCreateAtStart()));
        }
        if (query.getCreateAtEnd() != null) {
            sqlQuery.where(taskSchedulerLog.createAt.loe(query.getCreateAtEnd()));
        }
        if (query.isOrderEmpty()) {
            query.addOrderField(ColumnMetadata.getName(taskSchedulerLog.createAt), QueryBySort.DESC);
        }
        return QueryDslUtils.queryByPage(sqlQuery, query);
    }

    @SuppressWarnings("DuplicatedCode")
    public Page<TaskJobLog> queryTaskJobLog(TaskJobLogReq query) {
        SQLQuery<TaskJobLog> sqlQuery = queryDSL.selectFrom(taskJobLog);
        if (StringUtils.isNotBlank(query.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(query.getNamespace()));
        }
        if (StringUtils.isNotBlank(query.getInstanceName())) {
            sqlQuery.where(taskJobLog.instanceName.eq(query.getInstanceName()));
        }
        if (query.getJobId() != null) {
            sqlQuery.where(taskJobLog.jobId.eq(query.getJobId()));
        }
        if (query.getFireTimeStart() != null) {
            sqlQuery.where(taskJobLog.fireTime.goe(query.getFireTimeStart()));
        }
        if (query.getFireTimeEnd() != null) {
            sqlQuery.where(taskJobLog.fireTime.loe(query.getFireTimeEnd()));
        }
        if (query.isOrderEmpty()) {
            query.addOrderField(ColumnMetadata.getName(taskJobLog.fireTime), QueryBySort.DESC);
        }
        return QueryDslUtils.queryByPage(sqlQuery, query);
    }

    @SuppressWarnings("DuplicatedCode")
    public Page<JobLogInfo> queryJobLogInfo(TaskJobLogReq query) {
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobLog, taskJobTriggerLog, taskJob)
            .from(taskJobLog)
            .leftJoin(taskJobTriggerLog).on(taskJobLog.jobTriggerLogId.eq(taskJobTriggerLog.id))
            .leftJoin(taskJob).on(taskJobLog.jobId.eq(taskJob.id))
            .orderBy(taskJob.namespace.asc())
            .orderBy(taskJob.name.asc());
        if (StringUtils.isNotBlank(query.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(query.getNamespace()));
        }
        if (StringUtils.isNotBlank(query.getInstanceName())) {
            sqlQuery.where(taskJobLog.instanceName.eq(query.getInstanceName()));
        }
        if (query.getJobId() != null) {
            sqlQuery.where(taskJobLog.jobId.eq(query.getJobId()));
        }
        if (query.getFireTimeStart() != null) {
            sqlQuery.where(taskJobLog.fireTime.goe(query.getFireTimeStart()));
        }
        if (query.getFireTimeEnd() != null) {
            sqlQuery.where(taskJobLog.fireTime.loe(query.getFireTimeEnd()));
        }
        if (query.isOrderEmpty()) {
            query.addOrderField(ColumnMetadata.getName(taskJobLog.fireTime), QueryBySort.DESC);
        }
        Page<Tuple> page = QueryDslUtils.queryByPage(sqlQuery, query);
        return page.convertRecords(tuple -> {
            TaskJobLog jobLog = tuple.get(taskJobLog);
            TaskJobTriggerLog triggerLog = tuple.get(taskJobTriggerLog);
            TaskJob job = tuple.get(taskJob);
            return new JobLogInfo(jobLog, triggerLog, job);
        });
    }

    @SuppressWarnings("DuplicatedCode")
    public Page<JobInfo> queryJobs(TaskJobReq query) {
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJob, taskJobTrigger)
            .from(taskJob)
            .leftJoin(taskJobTrigger).on(taskJob.id.eq(taskJobTrigger.jobId))
            .orderBy(taskJob.namespace.asc())
            .orderBy(taskJob.name.asc());
        if (StringUtils.isNotBlank(query.getNamespace())) {
            sqlQuery.where(taskJob.namespace.eq(query.getNamespace()));
        }
        if (StringUtils.isNotBlank(query.getName())) {
            sqlQuery.where(taskJob.name.like(query.getName()));
        }
        if (query.getType() != null) {
            sqlQuery.where(taskJob.type.eq(query.getType()));
        }
        if (query.getCreateStart() != null) {
            sqlQuery.where(taskJob.createAt.goe(query.getCreateStart()));
        }
        if (query.getCreateEnd() != null) {
            sqlQuery.where(taskJob.createAt.loe(query.getCreateEnd()));
        }
        Page<Tuple> page = QueryDslUtils.queryByPage(sqlQuery, query);
        return page.convertRecords(tuple -> {
            TaskJob job = tuple.get(taskJob);
            TaskJobTrigger jobTrigger = tuple.get(taskJobTrigger);
            return new JobInfo(job, null, null, null, null, jobTrigger);
        });
    }

    public JobInfo getJobInfo(Long id) {
        Tuple tuple = queryDSL.select(taskJob, taskHttpJob, taskJavaJob, taskJsJob, taskShellJob, taskJobTrigger)
            .from(taskJob)
            .leftJoin(taskHttpJob).on(taskJob.id.eq(taskHttpJob.jobId))
            .leftJoin(taskJavaJob).on(taskJob.id.eq(taskJavaJob.jobId))
            .leftJoin(taskJsJob).on(taskJob.id.eq(taskJsJob.jobId))
            .leftJoin(taskShellJob).on(taskJob.id.eq(taskShellJob.jobId))
            .leftJoin(taskJobTrigger).on(taskJob.id.eq(taskJobTrigger.jobId))
            .where(taskJob.id.eq(id))
            .orderBy(taskJob.namespace.asc())
            .orderBy(taskJob.name.asc())
            .fetchOne();
        if (tuple == null) {
            return null;
        }
        TaskJob job = tuple.get(taskJob);
        TaskHttpJob httpJob = tuple.get(taskHttpJob);
        TaskJavaJob javaJob = tuple.get(taskJavaJob);
        TaskJsJob jsJob = tuple.get(taskJsJob);
        TaskShellJob shellJob = tuple.get(taskShellJob);
        TaskJobTrigger jobTrigger = tuple.get(taskJobTrigger);
        return new JobInfo(job, httpJob, javaJob, jsJob, shellJob, jobTrigger);
    }

    @SuppressWarnings("DuplicatedCode")
    public Page<JobInfo> queryTaskJobTriggers(TaskJobTriggerReq query) {
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobTrigger, taskJob)
            .from(taskJobTrigger)
            .leftJoin(taskJob).on(taskJob.id.eq(taskJobTrigger.jobId))
            .orderBy(taskJobTrigger.namespace.asc())
            .orderBy(taskJobTrigger.name.asc());
        if (StringUtils.isNotBlank(query.getNamespace())) {
            sqlQuery.where(taskJob.namespace.eq(query.getNamespace()));
        }
        if (StringUtils.isNotBlank(query.getName())) {
            sqlQuery.where(taskJobTrigger.name.like(query.getName()));
        }
        if (query.getType() != null) {
            sqlQuery.where(taskJobTrigger.type.eq(query.getType()));
        }
        if (query.getCreateStart() != null) {
            sqlQuery.where(taskJobTrigger.createAt.goe(query.getCreateStart()));
        }
        if (query.getCreateEnd() != null) {
            sqlQuery.where(taskJobTrigger.createAt.loe(query.getCreateEnd()));
        }
        Page<Tuple> page = QueryDslUtils.queryByPage(sqlQuery, query);
        return page.convertRecords(tuple -> {
            TaskJob job = tuple.get(taskJob);
            TaskJobTrigger jobTrigger = tuple.get(taskJobTrigger);
            return new JobInfo(job, null, null, null, null, jobTrigger);
        });
    }

    @SuppressWarnings("DuplicatedCode")
    public Page<TaskJobTriggerLog> queryTaskJobTriggerLogs(TaskJobTriggerLogReq query) {
        SQLQuery<TaskJobTriggerLog> sqlQuery = queryDSL.selectFrom(taskJobTriggerLog);
        if (StringUtils.isNotBlank(query.getNamespace())) {
            sqlQuery.where(taskJobTriggerLog.namespace.eq(query.getNamespace()));
        }
        if (StringUtils.isNotBlank(query.getInstanceName())) {
            sqlQuery.where(taskJobTriggerLog.instanceName.eq(query.getInstanceName()));
        }
        if (query.getJobTriggerId() != null) {
            sqlQuery.where(taskJobTriggerLog.jobTriggerId.eq(query.getJobTriggerId()));
        }
        if (query.getFireTimeStart() != null) {
            sqlQuery.where(taskJobTriggerLog.fireTime.goe(query.getFireTimeStart()));
        }
        if (query.getFireTimeEnd() != null) {
            sqlQuery.where(taskJobTriggerLog.fireTime.loe(query.getFireTimeEnd()));
        }
        if (query.isOrderEmpty()) {
            query.addOrderField(ColumnMetadata.getName(taskJobTriggerLog.fireTime), QueryBySort.DESC);
        }
        return QueryDslUtils.queryByPage(sqlQuery, query);
    }

    public TaskJobTriggerLog getTaskJobTriggerLog(Long jobTriggerLogId) {
        return queryDSL.selectFrom(taskJobTriggerLog).where(taskJobTriggerLog.id.eq(jobTriggerLogId)).fetchOne();
    }

    @SuppressWarnings("ExtractMethodRecommender")
    public StatisticsInfoRes getStatistics() {
        StatisticsInfoRes res = new StatisticsInfoRes();
        res.setJobCount(Conv.asInteger(queryDSL.selectFrom(taskJob).fetchCount()));
        res.setTriggerCount(Conv.asInteger(queryDSL.select(taskJobTrigger.fireCount.sum()).from(taskJobTrigger).fetchOne()));
        Tuple tuple = queryDSL.select(taskScheduler.namespace.countDistinct(), taskScheduler.instanceName.count()).from(taskScheduler).fetchOne();
        if (tuple != null) {
            res.setNamespaceCount(Conv.asInteger(tuple.get(taskScheduler.namespace.countDistinct())));
            res.setInstanceCount(Conv.asInteger(tuple.get(taskScheduler.instanceName.count())));
        }
        // heartbeat_interval * 2 > now - last_heartbeat_time
        BooleanExpression available = taskScheduler.heartbeatInterval.multiply(2).gt(
            Expressions.numberOperation(
                Long.TYPE, Ops.DateTimeOps.DIFF_SECONDS, taskScheduler.lastHeartbeatTime, Expressions.currentTimestamp()
            ).multiply(1000)
        );
        res.setActiveInstanceCount(Conv.asInteger(queryDSL.selectFrom(taskScheduler).where(available).fetchCount()));
        List<Tuple> list = queryDSL.select(taskJob.namespace, taskJob.type, taskJob.type.count())
            .from(taskJob)
            .groupBy(taskJob.namespace, taskJob.type)
            .fetch();
        Map<String, StatisticsInfoRes.JobTypeCount> namespaceJobTypeCountMap = new HashMap<>();
        for (Tuple item : list) {
            String namespace = item.get(taskJob.namespace);
            int type = Conv.asInteger(item.get(taskJob.type));
            int count = Conv.asInteger(item.get(taskJob.type.count()));
            StatisticsInfoRes.JobTypeCount jobTypeCount = namespaceJobTypeCountMap.computeIfAbsent(namespace, name -> new StatisticsInfoRes.JobTypeCount());
            switch (type) {
                case EnumConstant.JOB_TYPE_1:
                    jobTypeCount.setHttp(jobTypeCount.getHttp() + count);
                    break;
                case EnumConstant.JOB_TYPE_2:
                    jobTypeCount.setJava(jobTypeCount.getJava() + count);
                    break;
                case EnumConstant.JOB_TYPE_3:
                    jobTypeCount.setJs(jobTypeCount.getJs() + count);
                    break;
                case EnumConstant.JOB_TYPE_4:
                    jobTypeCount.setShell(jobTypeCount.getShell() + count);
                    break;
            }
        }
        res.setNamespaceJobTypeCountMap(namespaceJobTypeCountMap);
        StatisticsInfoRes.JobTypeCount jobTypeCountSum = new StatisticsInfoRes.JobTypeCount();
        for (StatisticsInfoRes.JobTypeCount count : namespaceJobTypeCountMap.values()) {
            jobTypeCountSum.setHttp(jobTypeCountSum.getHttp() + count.getHttp());
            jobTypeCountSum.setJava(jobTypeCountSum.getJava() + count.getJava());
            jobTypeCountSum.setJs(jobTypeCountSum.getJs() + count.getJs());
            jobTypeCountSum.setShell(jobTypeCountSum.getShell() + count.getShell());
        }
        res.setJobTypeCount(jobTypeCountSum);
        return res;
    }

    @SuppressWarnings("DuplicatedCode")
    public List<JobLogInfo> getLastRunJobs(RunJobsReq req) {
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(50);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        // 最近运行的任务
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobLog, taskJob)
            .from(taskJobLog).leftJoin(taskJob).on(taskJobLog.jobId.eq(taskJob.id))
            .where(taskJobLog.status.isNotNull())
            .orderBy(taskJobLog.fireTime.desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(req.getNamespace()));
        }
        List<Tuple> list = sqlQuery.fetch();
        return list.stream().map(tuple -> {
            JobLogInfo jobLogInfo = new JobLogInfo();
            jobLogInfo.setJob(tuple.get(taskJob));
            jobLogInfo.setLog(tuple.get(taskJobLog));
            return jobLogInfo;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("DuplicatedCode")
    public List<JobLogInfo> getLastRunningJobs(RunJobsReq req) {
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(50);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        // 正在运行的任务
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobLog, taskJob)
            .from(taskJobLog).leftJoin(taskJob).on(taskJobLog.jobId.eq(taskJob.id))
            .where(taskJobLog.status.isNull())
            .orderBy(taskJobLog.fireTime.desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(req.getNamespace()));
        }
        List<Tuple> list = sqlQuery.fetch();
        return list.stream().map(tuple -> {
            JobLogInfo jobLogInfo = new JobLogInfo();
            jobLogInfo.setJob(tuple.get(taskJob));
            jobLogInfo.setLog(tuple.get(taskJobLog));
            return jobLogInfo;
        }).collect(Collectors.toList());
    }

    public List<JobLogInfo> getWaitRunJobs(RunJobsReq req) {
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(50);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        // 即将运行的任务
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobTrigger, taskJob)
            .from(taskJobTrigger).leftJoin(taskJob).on(taskJobTrigger.jobId.eq(taskJob.id))
            .orderBy(taskJobTrigger.nextFireTime.desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobTrigger.namespace.eq(req.getNamespace()));
        }
        List<Tuple> list = sqlQuery.fetch();
        return list.stream().map(tuple -> {
            JobLogInfo jobLogInfo = new JobLogInfo();
            jobLogInfo.setJob(tuple.get(taskJob));
            jobLogInfo.setTrigger(tuple.get(taskJobTrigger));
            return jobLogInfo;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("DuplicatedCode")
    public JobMisfireRankRes getMisfireJobs(JobErrorRankReq req) {
        if (req.getEnd() == null) {
            req.setEnd(new Date());
        }
        if (req.getStart() == null) {
            req.setStart(DateUtils.addMonths(req.getEnd(), -1));
        }
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(50);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        JobMisfireRankRes res = new JobMisfireRankRes();
        // 错过触发最多的任务
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobTriggerLog.jobId, taskJobTriggerLog.jobId.count())
            .from(taskJobTriggerLog)
            .where(taskJobTriggerLog.createAt.goe(req.getStart()))
            .where(taskJobTriggerLog.createAt.loe(req.getEnd()))
            .where(taskJobTriggerLog.misFired.eq(EnumConstant.JOB_TRIGGER_MIS_FIRED_1))
            .groupBy(taskJobTriggerLog.jobId)
            .orderBy(taskJobTriggerLog.jobId.count().desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobTriggerLog.namespace.eq(req.getNamespace()));
        }
        List<Tuple> tuples = sqlQuery.fetch();
        tuples.forEach(tuple -> res.getMisfireJobMap().put(tuple.get(taskJobTriggerLog.jobId), Conv.asInteger(tuple.get(taskJobTriggerLog.jobId.count()))));
        List<TaskJob> jobs = queryDSL.selectFrom(taskJob).where(taskJob.id.in(res.getMisfireJobMap().keySet())).fetch();
        for (Long jobId : res.getMisfireJobMap().keySet()) {
            TaskJob jobEntity = jobs.stream()
                .filter(job -> Objects.equals(job.getId(), jobId))
                .findFirst().orElseThrow(() -> new BusinessException("未知的错误"));
            res.getMisfireJobs().add(jobEntity);
        }
        return res;
    }

    @SuppressWarnings("DuplicatedCode")
    public JobFailRankRes getFailJobs(JobErrorRankReq req) {
        if (req.getEnd() == null) {
            req.setEnd(new Date());
        }
        if (req.getStart() == null) {
            req.setStart(DateUtils.addMonths(req.getEnd(), -1));
        }
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(50);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        JobFailRankRes res = new JobFailRankRes();
        // 运行失败最多的任务
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobLog.jobId, taskJobLog.jobId.count())
            .from(taskJobLog)
            .where(taskJobLog.fireTime.goe(req.getStart()))
            .where(taskJobLog.fireTime.loe(req.getEnd()))
            .where(taskJobLog.status.eq(EnumConstant.JOB_LOG_STATUS_1))
            .groupBy(taskJobLog.jobId)
            .orderBy(taskJobLog.jobId.count().desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(req.getNamespace()));
        }
        List<Tuple> tuples = sqlQuery.fetch();
        tuples.forEach(tuple -> res.getFailJobMap().put(tuple.get(taskJobLog.jobId), Conv.asInteger(tuple.get(taskJobLog.jobId.count()))));
        List<TaskJob> jobs = queryDSL.selectFrom(taskJob).where(taskJob.id.in(res.getFailJobMap().keySet())).fetch();
        for (Long jobId : res.getFailJobMap().keySet()) {
            TaskJob jobEntity = jobs.stream()
                .filter(job -> Objects.equals(job.getId(), jobId))
                .findFirst().orElseThrow(() -> new BusinessException("未知的错误"));
            res.getFailJobs().add(jobEntity);
        }
        return res;
    }

    @SuppressWarnings("DuplicatedCode")
    public JobRunTimeRankRes getRunTimeJobs(JobErrorRankReq req) {
        if (req.getEnd() == null) {
            req.setEnd(new Date());
        }
        if (req.getStart() == null) {
            req.setStart(DateUtils.addMonths(req.getEnd(), -1));
        }
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(50);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        JobRunTimeRankRes res = new JobRunTimeRankRes();
        // 运行耗时最长的任务
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobLog.jobId, taskJobLog.runTime.avg())
            .from(taskJobLog)
            .where(taskJobLog.fireTime.goe(req.getStart()))
            .where(taskJobLog.fireTime.loe(req.getEnd()))
            .where(taskJobLog.runTime.isNotNull())
            .groupBy(taskJobLog.jobId)
            .orderBy(taskJobLog.runTime.avg().desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(req.getNamespace()));
        }
        List<Tuple> tuples = sqlQuery.fetch();
        tuples.forEach(tuple -> res.getAvgRunTimeJobMap().put(tuple.get(taskJobLog.jobId), Conv.asInteger(tuple.get(taskJobLog.runTime.avg()))));
        List<TaskJob> jobs = queryDSL.selectFrom(taskJob).where(taskJob.id.in(res.getAvgRunTimeJobMap().keySet())).fetch();
        for (Long jobId : res.getAvgRunTimeJobMap().keySet()) {
            TaskJob jobEntity = jobs.stream()
                .filter(job -> Objects.equals(job.getId(), jobId))
                .findFirst().orElseThrow(() -> new BusinessException("未知的错误"));
            res.getMaxRunTimeJobs().add(jobEntity);
        }
        return res;
    }

    @SuppressWarnings("DuplicatedCode")
    public JobRetryRankRes getRetryJobs(JobErrorRankReq req) {
        if (req.getEnd() == null) {
            req.setEnd(new Date());
        }
        if (req.getStart() == null) {
            req.setStart(DateUtils.addMonths(req.getEnd(), -1));
        }
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(50);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        JobRetryRankRes res = new JobRetryRankRes();
        // 重试次数最多的任务
        SQLQuery<Tuple> sqlQuery = queryDSL.select(taskJobLog.jobId, taskJobLog.retryCount.sum())
            .from(taskJobLog)
            .where(taskJobLog.fireTime.goe(req.getStart()))
            .where(taskJobLog.fireTime.loe(req.getEnd()))
            .where(taskJobLog.retryCount.goe(1))
            .groupBy(taskJobLog.jobId)
            .orderBy(taskJobLog.retryCount.sum().desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(req.getNamespace()));
        }
        List<Tuple> tuples = sqlQuery.fetch();
        tuples.forEach(tuple -> res.getRetryJobMap().put(tuple.get(taskJobLog.jobId), Conv.asInteger(tuple.get(taskJobLog.retryCount.sum()))));
        List<TaskJob> jobs = queryDSL.selectFrom(taskJob).where(taskJob.id.in(res.getRetryJobMap().keySet())).fetch();
        for (Long jobId : res.getRetryJobMap().keySet()) {
            TaskJob jobEntity = jobs.stream()
                .filter(job -> Objects.equals(job.getId(), jobId))
                .findFirst().orElseThrow(() -> new BusinessException("未知的错误"));
            res.getMaxRetryJobs().add(jobEntity);
        }
        return res;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- transaction support

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     */
    public <T> T beginTX(TransactionCallback<T> action) {
        return jdbc.beginTX(action);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action) {
        return jdbc.beginReadOnlyTX(action);
    }
}
