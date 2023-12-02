package org.clever.task.core;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.*;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.DateUtils;
import org.clever.core.SystemClock;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.id.SnowFlake;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.Page;
import org.clever.dao.DataAccessException;
import org.clever.dao.DuplicateKeyException;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.jdbc.querydsl.utils.QueryDslUtils;
import org.clever.task.TaskDataSource;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.JobInfo;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.model.entity.*;
import org.clever.task.core.model.request.SchedulerLogReq;
import org.clever.task.core.model.request.TaskJobLogReq;
import org.clever.task.core.model.request.TaskJobReq;
import org.clever.task.core.support.DataBaseClock;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import static org.clever.task.core.model.query.QTaskSchedulerLock.taskSchedulerLock;
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
        jobTriggerLog.setId(snowFlake.nextId());
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
        final long startTime = SystemClock.now();
        final boolean wait = waitSeconds > 0;
        final Function<Connection, SQLQueryFactory> newDSL = connection -> {
            Configuration configuration = new Configuration(QueryDSL.getSQLTemplates(jdbc.getDbType()));
            if (wait) {
                configuration.addListener(new SQLBaseListener() {
                    @Override
                    public void preExecute(SQLListenerContext context) {
                        try {
                            int timeout = waitSeconds - ((int) ((SystemClock.now() - startTime) / 1000));
                            context.getPreparedStatement().setQueryTimeout(Math.max(1, timeout));
                        } catch (SQLException e) {
                            throw ExceptionUtils.unchecked(e);
                        }
                    }
                });
            }
            return new SQLQueryFactory(configuration, () -> connection);
        };
        try {
            // 在一个新连接中操作，不会受到 JDBC 原始的事务影响
            return jdbc.newConnectionExecute(connection -> {
                // 这里使用新的连接获取数据库行级锁
                final SQLQueryFactory dsl = newDSL.apply(connection);
                // 使用数据库行级锁保证并发性
                long lock = dsl.update(taskSchedulerLock)
                    .set(taskSchedulerLock.lockCount, taskSchedulerLock.lockCount.add(1))
                    .set(taskSchedulerLock.updateAt, Expressions.currentTimestamp())
                    .where(taskSchedulerLock.lockName.eq(lockName))
                    .execute();
                // 锁数据不存在就创建锁数据
                if (lock <= 0) {
                    try {
                        // 在一个新事物里新增锁数据(尽可能让其他事务能使用这个锁)
                        jdbc.newConnectionExecute(innerCon -> {
                            SQLQueryFactory tmpDSL = newDSL.apply(innerCon);
                            return tmpDSL.insert(taskSchedulerLock)
                                .set(taskSchedulerLock.id, snowFlake.nextId())
                                .set(taskSchedulerLock.namespace, namespace)
                                .set(taskSchedulerLock.lockName, lockName)
                                .set(taskSchedulerLock.lockCount, 0L)
                                .set(taskSchedulerLock.description, "系统自动生成")
                                .set(taskSchedulerLock.createAt, Expressions.currentTimestamp())
                                .execute();
                        });
                    } catch (DuplicateKeyException e) {
                        // 插入数据失败: 唯一约束错误
                        log.warn("插入 {} 表失败: {}", taskSchedulerLock.getTableName(), e.getMessage());
                    } catch (DataAccessException e) {
                        log.warn("插入 {} 表失败", taskSchedulerLock.getTableName(), e);
                    }
                    // 等待锁数据插入完成
                    final int maxRetryCount = 128;
                    for (int i = 0; i < maxRetryCount; i++) {
                        Long id = dsl.select(taskSchedulerLock.id).from(taskSchedulerLock).where(taskSchedulerLock.lockName.eq(lockName)).fetchFirst();
                        if (id != null) {
                            break;
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {
                            Thread.yield();
                        }
                        if (wait && (waitSeconds * 1000L) < (SystemClock.now() - startTime)) {
                            // 执行同步代码块(未得到锁)
                            return syncBlock.apply(false);
                        }
                    }
                    // 使用数据库行级锁保证并发性
                    lock = dsl.update(taskSchedulerLock)
                        .set(taskSchedulerLock.lockCount, taskSchedulerLock.lockCount.add(1))
                        .set(taskSchedulerLock.updateAt, Expressions.currentTimestamp())
                        .where(taskSchedulerLock.lockName.eq(lockName))
                        .execute();
                    if (lock <= 0) {
                        throw new RuntimeException(taskSchedulerLock.getTableName() + " 表数据不存在(未知的异常)");
                    }
                }
                // 执行同步代码块
                return syncBlock.apply(true);
            });
        } catch (Exception e) {
            // 超时异常
            if (ExceptionUtils.isCausedBy(e, Collections.singletonList(SQLTimeoutException.class))) {
                // 执行同步代码块(未得到锁)
                return syncBlock.apply(false);
            }
            throw ExceptionUtils.unchecked(e);
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
