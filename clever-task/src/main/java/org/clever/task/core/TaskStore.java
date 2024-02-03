package org.clever.task.core;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.DateUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.id.SnowFlake;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.jdbc.querydsl.utils.QueryDslUtils;
import org.clever.task.TaskDataSource;
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.JobInfo;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.model.entity.*;
import org.clever.task.core.support.ClassMethodLoader;
import org.clever.task.core.support.DataBaseClock;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.annotation.Propagation;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.clever.task.core.model.query.QTaskHttpJob.taskHttpJob;
import static org.clever.task.core.model.query.QTaskJavaJob.taskJavaJob;
import static org.clever.task.core.model.query.QTaskJob.taskJob;
import static org.clever.task.core.model.query.QTaskJobConsoleLog.taskJobConsoleLog;
import static org.clever.task.core.model.query.QTaskJobLog.taskJobLog;
import static org.clever.task.core.model.query.QTaskJobTrigger.taskJobTrigger;
import static org.clever.task.core.model.query.QTaskJobTriggerLog.taskJobTriggerLog;
import static org.clever.task.core.model.query.QTaskJsJob.taskJsJob;
import static org.clever.task.core.model.query.QTaskReport.taskReport;
import static org.clever.task.core.model.query.QTaskScheduler.taskScheduler;
import static org.clever.task.core.model.query.QTaskSchedulerCmd.taskSchedulerCmd;
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
    @Getter
    private final Jdbc jdbc;
    @Getter
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
                .set(taskScheduler.runtimeInfo, scheduler.getRuntimeInfo())
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
    public long heartbeat(TaskScheduler scheduler) {
        final Date now = currentDate();
        long count = queryDSL.update(taskScheduler)
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
    public long countInvalidTrigger(String namespace) {
        return queryDSL.select(taskJobTrigger)
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

    public Date getNextFireTime(String namespace, Long jobTriggerId) {
        return queryDSL.select(taskJobTrigger.nextFireTime)
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
        jdbc.beginTX(status -> {
            lock(namespace, String.format("job_%s", jobId), syncBlock);
        }, Propagation.REQUIRES_NEW, 60 * 60 * 24);
    }

    /**
     * 更新job Data
     */
    public long updateJodData(String namespace, Long jobId, String jobData) {
        return queryDSL.update(taskJob)
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
    public long updateJobRunCount(String namespace, Long jobId) {
        return queryDSL.update(taskJob)
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
    public long updateInvalidTrigger(String namespace) {
        return queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.nextFireTime, Expressions.nullExpression())
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.type.eq(EnumConstant.JOB_TRIGGER_TYPE_2).and(taskJobTrigger.fixedInterval.loe(0)))
            .where(taskJobTrigger.namespace.eq(namespace))
            .execute();
    }

    /**
     * 更新触发器下一次触发时间
     */
    public long updateNextFireTime(Long triggerId, Date nextFireTime) {
        SQLUpdateClause update = queryDSL.update(taskJobTrigger).where(taskJobTrigger.id.eq(triggerId));
        if (nextFireTime == null) {
            update.setNull(taskJobTrigger.nextFireTime);
        } else {
            update.set(taskJobTrigger.nextFireTime, nextFireTime);
            update.where(taskJobTrigger.endTime.isNull().or(taskJobTrigger.endTime.goe(currentDate())));
        }
        return update.execute();
    }

    /**
     * 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
     */
    public long updateNextFireTimeForType2(String namespace) {
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
        return queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.nextFireTime, nextFireTime)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.type.eq(EnumConstant.JOB_TRIGGER_TYPE_2))
            .where(taskJobTrigger.fixedInterval.gt(0))
            .where(taskJobTrigger.nextFireTime.isNotNull().or(taskJobTrigger.nextFireTime.ne(nextFireTime)))
            .where(taskJobTrigger.namespace.eq(namespace))
            .where(taskJobTrigger.endTime.isNull().or(taskJobTrigger.endTime.goe(currentDate())))
            .execute();
    }

    /**
     * 更新触发器“上一次触发时间”、“下一次触发时间”
     */
    public long updateFireTime(Long triggerId, Date lastFireTime, Date nextFireTime) {
        SQLUpdateClause update = queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.lastFireTime, lastFireTime)
            .set(taskJobTrigger.fireCount, taskJobTrigger.fireCount.add(1))
            .where(taskJobTrigger.id.eq(triggerId));
        if (nextFireTime == null) {
            update.setNull(taskJobTrigger.nextFireTime);
        } else {
            update.set(taskJobTrigger.nextFireTime, nextFireTime);
            update.where(taskJobTrigger.endTime.isNull().or(taskJobTrigger.endTime.goe(currentDate())));
        }
        return update.execute();
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

    public long addSchedulerLog(TaskSchedulerLog schedulerLog) {
        schedulerLog.setId(snowFlake.nextId());
        schedulerLog.setCreateAt(currentDate());
        return queryDSL.insert(taskSchedulerLog).populate(schedulerLog).execute();
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
            if (jobLog.getId() == null) {
                jobLog.setId(snowFlake.nextId());
            }
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

    public long addJobTriggerLog(TaskJobTriggerLog jobTriggerLog) {
        if (jobTriggerLog.getId() == null) {
            jobTriggerLog.setId(snowFlake.nextId());
        }
        jobTriggerLog.setCreateAt(currentDate());
        return queryDSL.insert(taskJobTriggerLog).populate(jobTriggerLog).execute();
    }

    public long addJobLog(TaskJobLog jobLog) {
        if (jobLog.getId() == null) {
            jobLog.setId(snowFlake.nextId());
        }
        jobLog.setStartTime(currentDate());
        jobLog.setEndTime(null);
        jobLog.setRunTime(null);
        jobLog.setStatus(null);
        return queryDSL.insert(taskJobLog).populate(jobLog).execute();
    }

    public long updateJobLogByEnd(TaskJobLog jobLog) {
        jobLog.setEndTime(currentDate());
        return queryDSL.update(taskJobLog)
            .set(taskJobLog.endTime, jobLog.getEndTime())
            .set(taskJobLog.runTime, jobLog.getRunTime())
            .set(taskJobLog.status, jobLog.getStatus())
            .set(taskJobLog.exceptionInfo, jobLog.getExceptionInfo())
            .set(taskJobLog.afterJobData, jobLog.getAfterJobData())
            .where(taskJobLog.namespace.eq(jobLog.getNamespace()))
            .where(taskJobLog.id.eq(jobLog.getId()))
            .execute();
    }

    public long updateJobLogByRetry(TaskJobLog jobLog) {
        return queryDSL.update(taskJobLog)
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

    public TaskScheduler getScheduler(long schedulerId) {
        return queryDSL.select(taskScheduler)
            .from(taskScheduler)
            .where(taskScheduler.id.eq(schedulerId))
            .fetchOne();
    }

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

    public long updateDisableJob(Integer disable, Long... jobIds) {
        return queryDSL.update(taskJob)
            .set(taskJob.disable, disable)
            .where(taskJob.id.in(jobIds))
            .execute();
    }

    public long updateDisableTrigger(Integer disable, Long... triggerIds) {
        return queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.disable, disable)
            .where(taskJobTrigger.id.in(triggerIds))
            .execute();
    }

    public long addTrigger(TaskJobTrigger jobTrigger) {
        if (jobTrigger.getId() == null) {
            jobTrigger.setId(snowFlake.nextId());
        }
        jobTrigger.setCreateAt(currentDate());
        return queryDSL.insert(taskJobTrigger).populate(jobTrigger).execute();
    }

    public long addJob(TaskJob job) {
        if (job.getId() == null) {
            job.setId(snowFlake.nextId());
        }
        job.setCreateAt(currentDate());
        return queryDSL.insert(taskJob).populate(job).execute();
    }

    public long addHttpJob(TaskHttpJob httpJob) {
        if (httpJob.getId() == null) {
            httpJob.setId(snowFlake.nextId());
        }
        httpJob.setCreateAt(currentDate());
        return queryDSL.insert(taskHttpJob).populate(httpJob).execute();
    }

    public long addJavaJob(TaskJavaJob javaJob) {
        if (javaJob.getId() == null) {
            javaJob.setId(snowFlake.nextId());
        }
        javaJob.setCreateAt(currentDate());
        return queryDSL.insert(taskJavaJob).populate(javaJob).execute();
    }

    public long addJsJob(TaskJsJob jsJob) {
        if (jsJob.getId() == null) {
            jsJob.setId(snowFlake.nextId());
        }
        jsJob.setCreateAt(currentDate());
        return queryDSL.insert(taskJsJob).populate(jsJob).execute();
    }

    public long addShellJob(TaskShellJob shellJob) {
        if (shellJob.getId() == null) {
            shellJob.setId(snowFlake.nextId());
        }
        shellJob.setCreateAt(currentDate());
        return queryDSL.insert(taskShellJob).populate(shellJob).execute();
    }

    public boolean updateTrigger(TaskJobTrigger jobTrigger) {
        return queryDSL.update(
            taskJobTrigger,
            jobTrigger.getId() != null ? taskJobTrigger.id.eq(jobTrigger.getId()) : taskJobTrigger.jobId.eq(jobTrigger.getJobId()),
            jobTrigger,
            update -> update.set(taskJobTrigger.updateAt, currentDate()),
            taskJobTrigger.id,
            taskJobTrigger.lastFireTime,
            taskJobTrigger.fireCount,
            taskJobTrigger.createAt,
            taskJobTrigger.updateAt
        );
    }

    public boolean updateJob(TaskJob job) {
        return queryDSL.update(
            taskJob,
            taskJob.id.eq(job.getId()),
            job,
            update -> update.set(taskJob.updateAt, currentDate()),
            taskJob.id,
            taskJob.runCount,
            taskJob.createAt,
            taskJob.updateAt
        );
    }

    public boolean updateHttpJob(TaskHttpJob httpJob) {
        return queryDSL.update(
            taskHttpJob,
            httpJob.getId() != null ? taskHttpJob.id.eq(httpJob.getId()) : taskHttpJob.jobId.eq(httpJob.getJobId()),
            httpJob,
            update -> update.set(taskHttpJob.updateAt, currentDate()),
            taskHttpJob.id,
            taskHttpJob.createAt,
            taskHttpJob.updateAt
        );
    }

    public boolean updateJavaJob(TaskJavaJob javaJob) {
        return queryDSL.update(
            taskJavaJob,
            javaJob.getId() != null ? taskJavaJob.id.eq(javaJob.getId()) : taskJavaJob.jobId.eq(javaJob.getJobId()),
            javaJob,
            update -> update.set(taskJavaJob.updateAt, currentDate()),
            taskJavaJob.id,
            taskJavaJob.createAt,
            taskJavaJob.updateAt
        );
    }

    public boolean updateJsJob(TaskJsJob jsJob) {
        return queryDSL.update(
            taskJsJob,
            jsJob.getId() != null ? taskJsJob.id.eq(jsJob.getId()) : taskJsJob.jobId.eq(jsJob.getJobId()),
            jsJob,
            update -> update.set(taskJsJob.updateAt, currentDate()),
            taskJsJob.id,
            taskJsJob.createAt,
            taskJsJob.updateAt
        );
    }

    public boolean updateShellJob(TaskShellJob shellJob) {
        return queryDSL.update(
            taskShellJob,
            shellJob.getId() != null ? taskShellJob.id.eq(shellJob.getId()) : taskShellJob.jobId.eq(shellJob.getJobId()),
            shellJob,
            update -> update.set(taskShellJob.updateAt, currentDate()),
            taskShellJob.id,
            taskShellJob.createAt,
            taskShellJob.updateAt
        );
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

    public long clearSchedulerCmd() {
        Date maxDate = new Date(currentTimeMillis() - GlobalConstant.EXEC_SCHEDULER_CMD_INTERVAL - 10_000);
        return queryDSL.delete(taskSchedulerCmd)
            .where(taskSchedulerCmd.createAt.loe(maxDate))
            .execute();
    }

    public long clearJobLog(String namespace, Date maxDate) {
        return queryDSL.delete(taskJobLog)
            .where(taskJobLog.namespace.eq(namespace))
            .where(taskJobLog.endTime.loe(maxDate))
            .execute();
    }

    public long clearTriggerJobLog(String namespace, Date maxDate) {
        return queryDSL.delete(taskJobTriggerLog)
            .where(taskJobTriggerLog.namespace.eq(namespace))
            .where(taskJobTriggerLog.createAt.loe(maxDate))
            .execute();
    }

    public long clearSchedulerLog(String namespace, Date maxDate) {
        return queryDSL.delete(taskSchedulerLog)
            .where(taskSchedulerLog.namespace.eq(namespace))
            .where(taskSchedulerLog.createAt.loe(maxDate))
            .execute();
    }

    public long clearJobConsoleLog(String namespace, Date maxDate) {
        return queryDSL.delete(taskJobConsoleLog)
            .where(taskJobConsoleLog.namespace.eq(namespace))
            .where(taskJobConsoleLog.createAt.loe(maxDate))
            .execute();
    }

    public TaskReport createTaskReport(String namespace, Date date) {
        final TaskReport taskReport = new TaskReport();
        taskReport.setId(snowFlake.nextId());
        taskReport.setNamespace(namespace);
        taskReport.setReportTime(DateUtils.formatToString(date, DateUtils.yyyy_MM_dd));
        final Date dayStart = DateUtils.getDayStartTime(date);
        final Date dayEnd = DateUtils.addDays(dayStart, 1);
        NumberTemplate<Long> jobErrCountField = Expressions.numberTemplate(Long.class, "sum(case when {0}=1 then 1 else 0 end)", taskJobLog.status);
        Tuple tuple = queryDSL.select(taskJobLog.fireTime.count(), jobErrCountField)
            .from(taskJobLog)
            .where(taskJobLog.namespace.eq(namespace))
            .where(taskJobLog.fireTime.goe(dayStart))
            .where(taskJobLog.fireTime.lt(dayEnd))
            .fetchOne();
        if (tuple != null) {
            taskReport.setJobCount(tuple.get(taskJobLog.fireTime.count()));
            taskReport.setJobErrCount(tuple.get(jobErrCountField));
        }
        NumberTemplate<Long> misfireCountField = Expressions.numberTemplate(Long.class, "sum(case when {0}=1 then 1 else 0 end)", taskJobTriggerLog.misFired);
        tuple = queryDSL.select(taskJobTriggerLog.fireTime.count(), misfireCountField)
            .from(taskJobTriggerLog)
            .where(taskJobTriggerLog.namespace.eq(namespace))
            .where(taskJobTriggerLog.fireTime.goe(dayStart))
            .where(taskJobTriggerLog.fireTime.lt(dayEnd))
            .fetchOne();
        if (tuple != null) {
            taskReport.setTriggerCount(tuple.get(taskJobTriggerLog.fireTime.count()));
            taskReport.setMisfireCount(tuple.get(misfireCountField));
        }
        if (taskReport.getJobCount() == null) {
            taskReport.setJobCount(0L);
        }
        if (taskReport.getJobErrCount() == null) {
            taskReport.setJobErrCount(0L);
        }
        if (taskReport.getTriggerCount() == null) {
            taskReport.setTriggerCount(0L);
        }
        if (taskReport.getMisfireCount() == null) {
            taskReport.setMisfireCount(0L);
        }
        return taskReport;
    }

    public String getLastReportTime(String namespace) {
        return queryDSL.select(taskReport.reportTime.max())
            .from(taskReport)
            .where(taskReport.namespace.eq(namespace))
            .fetchOne();
    }

    public Date getMinFireTime(String namespace) {
        Date jobLogMin = queryDSL.select(taskJobLog.fireTime.min())
            .from(taskJobLog)
            .where(taskJobLog.namespace.eq(namespace))
            .fetchOne();
        Date triggerLogMin = queryDSL.select(taskJobTriggerLog.fireTime.min())
            .from(taskJobTriggerLog)
            .where(taskJobTriggerLog.namespace.eq(namespace))
            .fetchOne();
        if (jobLogMin == null && triggerLogMin == null) {
            return null;
        }
        if (jobLogMin == null || triggerLogMin == null) {
            return jobLogMin == null ? triggerLogMin : jobLogMin;
        }
        return jobLogMin.compareTo(triggerLogMin) > 0 ? triggerLogMin : jobLogMin;
    }

    public long addOrUpdateTaskReport(List<TaskReport> listReport) {
        if (listReport == null || listReport.isEmpty()) {
            return 0L;
        }
        List<String> exists = queryDSL.select(taskReport.reportTime).from(taskReport)
            .where(taskReport.reportTime.in(listReport.stream().map(TaskReport::getReportTime).collect(Collectors.toSet())))
            .fetch();
        SQLUpdateClause update = queryDSL.update(taskReport);
        SQLInsertClause insert = queryDSL.insert(taskReport);
        for (TaskReport report : listReport) {
            if (exists.contains(report.getReportTime())) {
                update.set(taskReport.jobCount, report.getJobCount())
                    .set(taskReport.jobErrCount, report.getJobErrCount())
                    .set(taskReport.triggerCount, report.getTriggerCount())
                    .set(taskReport.misfireCount, report.getMisfireCount())
                    .where(taskReport.reportTime.eq(report.getReportTime()))
                    .addBatch();
            } else {
                insert.populate(report).addBatch();
            }
        }
        long count = 0;
        if (update.getBatchCount() > 0) {
            count = count + update.execute();
        }
        if (insert.getBatchCount() > 0) {
            count = count + insert.execute();
        }
        return count;
    }

    public String dataCheck(String namespace) {
        final String line = "\n";
        final String tab = "\t";
        final StringBuilder errMsg = new StringBuilder();
        final Consumer<List<?>> appendData = list -> {
            if (list.isEmpty()) {
                return;
            }
            for (Object row : list) {
                errMsg.append(tab).append(row).append(line);
            }
            errMsg.append(line);
        };
        // 1.task_http_job、task_java_job、task_js_job、task_shell_job 与 task_job 是否是一对一的关系
        // -- task_java_job 一对一的关系
        // select
        //     a.job_id
        // from task_http_job a
        // group by a.job_id having count(a.id) > 1
        final Function<String, TupleTwo<String, List<Map<String, Object>>>> query1 = tableName -> {
            String sql = String.format("select a.job_id from %s a group by a.job_id having count(a.id) > 1", tableName);
            List<Map<String, Object>> res = beginReadOnlyTX(status -> jdbc.queryMany(sql));
            return TupleTwo.creat(sql, res);
        };
        final String[] tables = new String[]{taskHttpJob.getTableName(), taskJavaJob.getTableName(), taskJsJob.getTableName(), taskShellJob.getTableName()};
        for (String table : tables) {
            TupleTwo<String, List<Map<String, Object>>> tuple = query1.apply(table);
            if (tuple.getValue2().isEmpty()) {
                continue;
            }
            errMsg.append(String.format("-- %s 一对一的关系", table)).append(line);
            errMsg.append(tab).append(tuple.getValue1()).append(line);
            appendData.accept(tuple.getValue2());
        }
        // -- 无效的 task_job 数据(任务类型错误或者缺少任务子表数据)
        // select
        //     a.id, a.namespace, a.name
        // from task_job a
        //     left join task_http_job b on (a.id=b.job_id)
        //     left join task_java_job c on (a.id=c.job_id)
        //     left join task_js_job d on (a.id=d.job_id)
        //     left join task_shell_job e on (a.id=e.job_id)
        // where a.type not in (1, 2, 3, 4)
        //     or (a.type=1 and b.id is null )
        //     or (a.type=2 and c.id is null )
        //     or (a.type=3 and d.id is null )
        //     or (a.type=4 and e.id is null )
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery1 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskJob.id, taskJob.namespace, taskJob.name
            ))
            .from(taskJob)
            .leftJoin(taskHttpJob).on(taskJob.id.eq(taskHttpJob.jobId))
            .leftJoin(taskJavaJob).on(taskJob.id.eq(taskJavaJob.jobId))
            .leftJoin(taskJsJob).on(taskJob.id.eq(taskJsJob.jobId))
            .leftJoin(taskShellJob).on(taskJob.id.eq(taskShellJob.jobId))
            .where(
                taskJob.type.notIn(EnumConstant.JOB_TYPE_1, EnumConstant.JOB_TYPE_2, EnumConstant.JOB_TYPE_3, EnumConstant.JOB_TYPE_4)
                    .or(taskJob.type.eq(EnumConstant.JOB_TYPE_1).and(taskHttpJob.id.isNull()))
                    .or(taskJob.type.eq(EnumConstant.JOB_TYPE_2).and(taskJavaJob.id.isNull()))
                    .or(taskJob.type.eq(EnumConstant.JOB_TYPE_3).and(taskJsJob.id.isNull()))
                    .or(taskJob.type.eq(EnumConstant.JOB_TYPE_4).and(taskShellJob.id.isNull()))
            );
        List<LinkedHashMap<String, ?>> list1 = beginReadOnlyTX(status -> sqlQuery1.fetch());
        if (!list1.isEmpty()) {
            errMsg.append("-- 无效的 task_job 数据(任务类型错误或者缺少任务子表数据)").append(line);
            errMsg.append(sqlQuery1.getSQL().getSQL()).append(line);
            appendData.accept(list1);
        }
        // -- 无效的 task_http_job 数据(namespace不一致或者缺少父表数据)
        // select
        //     a.id, a.namespace, a.job_id
        // from task_http_job a left join task_job b on (a.job_id=b.id)
        // where a.namespace!=b.namespace or b.id is null
        final Function<String, TupleTwo<String, List<Map<String, Object>>>> query2 = tableName -> {
            String sql = String.format(
                "select a.id, a.namespace, a.job_id from %s a left join %s b on (a.job_id=b.id) where a.namespace!=b.namespace or b.id is null",
                tableName, taskJob.getTableName()
            );
            List<Map<String, Object>> res = beginReadOnlyTX(status -> jdbc.queryMany(sql));
            return TupleTwo.creat(sql, res);
        };
        for (String table : tables) {
            TupleTwo<String, List<Map<String, Object>>> tuple = query2.apply(table);
            if (tuple.getValue2().isEmpty()) {
                continue;
            }
            errMsg.append(String.format("-- 无效的 %s 数据(namespace不一致或者缺少父表数据)", table)).append(line);
            errMsg.append(tuple.getValue1()).append(line);
            appendData.accept(tuple.getValue2());
        }
        // 2.task_job_trigger 与 task_job 是否是一对一的关系
        // -- task_job_trigger 一对一的关系
        // select
        //     a.job_id
        // from task_job_trigger a
        // group by a.job_id having count(a.id) > 1
        TupleTwo<String, List<Map<String, Object>>> tuple = query1.apply(taskJobTrigger.getTableName());
        if (!tuple.getValue2().isEmpty()) {
            errMsg.append(String.format("-- %s 一对一的关系", taskJobTrigger.getTableName())).append(line);
            errMsg.append(tab).append(tuple.getValue1()).append(line);
            appendData.accept(tuple.getValue2());
        }
        // -- 无效的 task_job_trigger 数据(触发类型错误或者与task_job匹配的数据)
        // select
        //     a.id, a.namespace, a.name
        // from task_job a
        //     left join task_job_trigger b on (a.id=b.job_id)
        // where b.type not in (1, 2) or b.id is null
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery2 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskJob.id, taskJob.namespace, taskJob.name
            ))
            .from(taskJob)
            .leftJoin(taskJobTrigger).on(taskJob.id.eq(taskJobTrigger.jobId))
            .where(
                taskJobTrigger.type.notIn(EnumConstant.JOB_TRIGGER_TYPE_1, EnumConstant.JOB_TRIGGER_TYPE_2)
                    .or(taskJobTrigger.id.isNull())
            );
        List<LinkedHashMap<String, ?>> list2 = beginReadOnlyTX(status -> sqlQuery2.fetch());
        if (!list2.isEmpty()) {
            errMsg.append("-- 无效的 task_job_trigger 数据(触发类型错误或者与task_job匹配的数据)").append(line);
            errMsg.append(sqlQuery2.getSQL().getSQL()).append(line);
            appendData.accept(list2);
        }
        // 3. task_job: type、route_strategy、first_scheduler、whitelist_scheduler、blacklist_scheduler、load_balance 字段值的有效性
        // -- task_job: type、route_strategy、load_balance 字段值的有效性
        // select
        //     a.id, a.namespace, a.name
        // from task_job a
        // where a.type not in (1, 2, 3, 4)
        //     or a.route_strategy not in (0, 1, 2, 3)
        //     or a.load_balance not in (1, 2, 3, 4)
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery3 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskJob.id, taskJob.namespace, taskJob.name
            ))
            .from(taskJob)
            .where(
                taskJob.type.notIn(EnumConstant.JOB_TYPE_1, EnumConstant.JOB_TYPE_2, EnumConstant.JOB_TYPE_3, EnumConstant.JOB_TYPE_4)
                    .or(taskJob.routeStrategy.notIn(EnumConstant.JOB_ROUTE_STRATEGY_0, EnumConstant.JOB_ROUTE_STRATEGY_1, EnumConstant.JOB_ROUTE_STRATEGY_2, EnumConstant.JOB_ROUTE_STRATEGY_3))
                    .or(taskJob.loadBalance.notIn(EnumConstant.JOB_LOAD_BALANCE_1, EnumConstant.JOB_LOAD_BALANCE_2, EnumConstant.JOB_LOAD_BALANCE_3, EnumConstant.JOB_LOAD_BALANCE_4))
            );
        List<LinkedHashMap<String, ?>> list3 = beginReadOnlyTX(status -> sqlQuery3.fetch());
        if (!list3.isEmpty()) {
            errMsg.append("-- task_job: type、route_strategy、load_balance 字段值的有效性").append(line);
            errMsg.append(sqlQuery3.getSQL().getSQL()).append(line);
            appendData.accept(list3);
        }
        // -- task_job: first_scheduler、whitelist_scheduler、blacklist_scheduler 字段值的有效性
        // select
        //     a.id, a.namespace, a.name, a.first_scheduler, a.whitelist_scheduler, a.blacklist_scheduler
        // from task_job a
        // where a.route_strategy not in (1, 2, 3)
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery4 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskJob.id, taskJob.namespace, taskJob.name,
                taskJob.firstScheduler, taskJob.whitelistScheduler, taskJob.blacklistScheduler
            ))
            .from(taskJob)
            .where(
                taskJob.type.notIn(EnumConstant.JOB_TYPE_1, EnumConstant.JOB_TYPE_2, EnumConstant.JOB_TYPE_3, EnumConstant.JOB_TYPE_4)
                    .or(taskJob.routeStrategy.notIn(EnumConstant.JOB_ROUTE_STRATEGY_0, EnumConstant.JOB_ROUTE_STRATEGY_1, EnumConstant.JOB_ROUTE_STRATEGY_2, EnumConstant.JOB_ROUTE_STRATEGY_3))
                    .or(taskJob.loadBalance.notIn(EnumConstant.JOB_LOAD_BALANCE_1, EnumConstant.JOB_LOAD_BALANCE_2, EnumConstant.JOB_LOAD_BALANCE_3, EnumConstant.JOB_LOAD_BALANCE_4))
            );
        List<LinkedHashMap<String, ?>> list4 = beginReadOnlyTX(status -> sqlQuery4.fetch());
        Iterator<LinkedHashMap<String, ?>> iterator4 = list4.iterator();
        while (iterator4.hasNext()) {
            LinkedHashMap<String, ?> row = iterator4.next();
            try {
                String firstScheduler = Conv.asString(row.get(taskJob.firstScheduler.getMetadata().getName()));
                String whitelistScheduler = Conv.asString(row.get(taskJob.whitelistScheduler.getMetadata().getName()));
                String blacklistScheduler = Conv.asString(row.get(taskJob.blacklistScheduler.getMetadata().getName()));
                if (StringUtils.isNotBlank(firstScheduler)) {
                    if (!Objects.equals(firstScheduler, EnumConstant.JOB_FIRST_SCHEDULER_1)) {
                        JacksonMapper.getInstance().fromJson(firstScheduler, List.class);
                    }
                }
                if (StringUtils.isNotBlank(whitelistScheduler)) {
                    JacksonMapper.getInstance().fromJson(whitelistScheduler, List.class);
                }
                if (StringUtils.isNotBlank(blacklistScheduler)) {
                    JacksonMapper.getInstance().fromJson(blacklistScheduler, List.class);
                }
                iterator4.remove();
            } catch (Throwable ignored) {
            }
        }
        if (!list4.isEmpty()) {
            errMsg.append("-- task_job: first_scheduler、whitelist_scheduler、blacklist_scheduler 字段值的有效性").append(line);
            errMsg.append(sqlQuery4.getSQL().getSQL()).append(line);
            appendData.accept(list4);
        }
        // 4.task_http_job: request_method 字段值的有效性
        // select
        //     a.id, a.namespace, a.job_id
        // from task_http_job a
        // where a.request_method not in ('GET','HEAD','POST','PUT','DELETE','CONNECT','OPTIONS','TRACE','PATCH')
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery5 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskHttpJob.id, taskHttpJob.namespace, taskHttpJob.jobId
            ))
            .from(taskHttpJob)
            .where(taskHttpJob.requestMethod.notIn(
                "GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH"
            ));
        List<LinkedHashMap<String, ?>> list5 = beginReadOnlyTX(status -> sqlQuery5.fetch());
        if (!list5.isEmpty()) {
            errMsg.append("-- task_http_job: request_method 字段值的有效性)").append(line);
            errMsg.append(sqlQuery5.getSQL().getSQL()).append(line);
            appendData.accept(list5);
        }
        // 5.task_java_job: class_name、class_method 字段值的有效性
        // select
        //     a.id, a.namespace, a.job_id, a.class_name, a.class_method
        // from task_java_job a
        // where a.namespace=''
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery6 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskJavaJob.id, taskJavaJob.namespace, taskJavaJob.jobId,
                taskJavaJob.className, taskJavaJob.classMethod
            ))
            .from(taskJavaJob)
            .where(taskJavaJob.namespace.eq(namespace));
        List<LinkedHashMap<String, ?>> list6 = beginReadOnlyTX(status -> sqlQuery6.fetch());
        Iterator<LinkedHashMap<String, ?>> iterator6 = list6.iterator();
        while (iterator6.hasNext()) {
            LinkedHashMap<String, ?> row = iterator6.next();
            try {
                String className = Conv.asString(row.get(taskJavaJob.className.getMetadata().getName()));
                String classMethod = Conv.asString(row.get(taskJavaJob.classMethod.getMetadata().getName()));
                TupleTwo<Class<?>, Method> exists = ClassMethodLoader.getMethod(className, classMethod);
                if (exists != null) {
                    iterator6.remove();
                }
            } catch (Throwable ignored) {
            }
        }
        if (!list6.isEmpty()) {
            errMsg.append("-- task_java_job: class_name、class_method 字段值的有效性").append(line);
            errMsg.append(sqlQuery6.getSQL().getSQL()).append(line);
            appendData.accept(list6);
        }
        // 6.task_shell_job: shell_type 字段值的有效性
        // select
        //     a.id, a.namespace, a.job_id
        // from task_shell_job a
        // where a.shell_type not in ('bash','sh','ash','powershell','cmd','python','node','deno','php')
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery7 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskShellJob.id, taskShellJob.namespace, taskShellJob.jobId
            ))
            .from(taskShellJob)
            .where(taskShellJob.shellType.notIn(EnumConstant.SHELL_TYPE_FILE_SUFFIX_MAPPING.keySet()));
        List<LinkedHashMap<String, ?>> list7 = beginReadOnlyTX(status -> sqlQuery7.fetch());
        if (!list7.isEmpty()) {
            errMsg.append("-- task_shell_job: shell_type 字段值的有效性").append(line);
            errMsg.append(sqlQuery7.getSQL().getSQL()).append(line);
            appendData.accept(list7);
        }
        // task_shell_job: shell_charset 字段值的有效性
        // select
        //     a.id, a.namespace, a.job_id, a.shell_charset
        // from task_shell_job a
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery8 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskShellJob.id, taskShellJob.namespace, taskShellJob.jobId, taskShellJob.shellCharset
            ))
            .from(taskShellJob)
            .where(taskShellJob.shellType.notIn(EnumConstant.SHELL_TYPE_FILE_SUFFIX_MAPPING.keySet()));
        List<LinkedHashMap<String, ?>> list8 = beginReadOnlyTX(status -> sqlQuery8.fetch());
        Iterator<LinkedHashMap<String, ?>> iterator8 = list8.iterator();
        while (iterator8.hasNext()) {
            LinkedHashMap<String, ?> row = iterator8.next();
            try {
                String shellCharset = Conv.asString(row.get(taskShellJob.shellCharset.getMetadata().getName()));
                Charset.forName(shellCharset);
                iterator8.remove();
            } catch (Throwable ignored) {
            }
        }
        if (!list8.isEmpty()) {
            errMsg.append("-- task_shell_job: shell_charset 字段值的有效性").append(line);
            errMsg.append(sqlQuery8.getSQL().getSQL()).append(line);
            appendData.accept(list8);
        }
        // 7.task_job_trigger: misfire_strategy、allow_concurrent、type、cron、fixed_interval 字段值的有效性
        // task_job_trigger: misfire_strategy、allow_concurrent、type 字段值的有效性
        // select
        //     a.id, a.namespace, a.job_id
        // from task_job_trigger a
        // where a.misfire_strategy not in (1, 2)
        //     or a.allow_concurrent not in (0, 1)
        //     or a.type not in (1, 2)
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery9 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskJobTrigger.id, taskJobTrigger.namespace, taskJobTrigger.jobId
            ))
            .from(taskJobTrigger)
            .where(
                taskJobTrigger.misfireStrategy.notIn(EnumConstant.JOB_TRIGGER_MISFIRE_STRATEGY_1, EnumConstant.JOB_TRIGGER_MISFIRE_STRATEGY_2)
                    .or(taskJobTrigger.allowConcurrent.notIn(EnumConstant.JOB_ALLOW_CONCURRENT_0, EnumConstant.JOB_ALLOW_CONCURRENT_1))
                    .or(taskJobTrigger.type.notIn(EnumConstant.JOB_TRIGGER_TYPE_1, EnumConstant.JOB_TRIGGER_TYPE_2))
            );
        List<LinkedHashMap<String, ?>> list9 = beginReadOnlyTX(status -> sqlQuery9.fetch());
        if (!list9.isEmpty()) {
            errMsg.append("-- task_job_trigger: misfire_strategy、allow_concurrent、type 字段值的有效性").append(line);
            errMsg.append(sqlQuery9.getSQL().getSQL()).append(line);
            appendData.accept(list9);
        }
        // task_job_trigger: cron、fixed_interval 字段值的有效性
        // select
        //     a.id, a.namespace, a.job_id, a.type, a.cron, a.fixed_interval
        // from task_job_trigger a
        // where a.type in (1, 2)
        SQLQuery<LinkedHashMap<String, ?>> sqlQuery10 = queryDSL.select(QueryDslUtils.linkedMap(
                RenameStrategy.None,
                taskJobTrigger.id, taskJobTrigger.namespace, taskJobTrigger.jobId,
                taskJobTrigger.type, taskJobTrigger.cron, taskJobTrigger.fixedInterval
            ))
            .from(taskJobTrigger)
            .where(taskJobTrigger.type.in(EnumConstant.JOB_TRIGGER_TYPE_1, EnumConstant.JOB_TRIGGER_TYPE_2));
        List<LinkedHashMap<String, ?>> list10 = beginReadOnlyTX(status -> sqlQuery10.fetch());
        Iterator<LinkedHashMap<String, ?>> iterator10 = list10.iterator();
        while (iterator10.hasNext()) {
            LinkedHashMap<String, ?> row = iterator10.next();
            try {
                Integer type = Conv.asInteger(row.get(taskJobTrigger.type.getMetadata().getName()));
                String cron = Conv.asString(row.get(taskJobTrigger.cron.getMetadata().getName()));
                Long fixedInterval = Conv.asLong(row.get(taskJobTrigger.fixedInterval.getMetadata().getName()));
                boolean success = false;
                if (Objects.equals(type, EnumConstant.JOB_TRIGGER_TYPE_1)) {
                    success = CronExpressionUtil.isValidExpression(cron);
                } else if (Objects.equals(type, EnumConstant.JOB_TRIGGER_TYPE_2)) {
                    success = (fixedInterval != null && fixedInterval > 0);
                }
                if (success) {
                    iterator10.remove();
                }
            } catch (Throwable ignored) {
            }
        }
        if (!list10.isEmpty()) {
            errMsg.append("-- task_job_trigger: cron、fixed_interval 字段值的有效性").append(line);
            errMsg.append(sqlQuery10.getSQL().getSQL()).append(line);
            appendData.accept(list10);
        }
        return errMsg.toString();
    }

    public Date lastEventNameDate(String namespace, String eventName) {
        return queryDSL.select(taskSchedulerLog.createAt.max())
            .from(taskSchedulerLog)
            .where(taskSchedulerLog.namespace.eq(namespace))
            .where(taskSchedulerLog.eventName.eq(eventName))
            .fetchOne();
    }

    public long saveJobConsoleLog(TaskJobConsoleLog jobConsoleLog) {
        if (jobConsoleLog == null) {
            return 0;
        }
        if (jobConsoleLog.getId() == null) {
            jobConsoleLog.setId(snowFlake.nextId());
        }
        return queryDSL.insert(taskJobConsoleLog).populate(jobConsoleLog).execute();
    }

    public List<TaskSchedulerCmd> getNextSchedulerCmd(String namespace, String instanceName) {
        Date minDate = new Date(currentTimeMillis() - (GlobalConstant.EXEC_SCHEDULER_CMD_INTERVAL + 5_000));
        return queryDSL.select(taskSchedulerCmd)
            .from(taskSchedulerCmd)
            .where(taskSchedulerCmd.namespace.eq(namespace))
            .where(
                taskSchedulerCmd.instanceName.eq(instanceName)
                    .or(taskSchedulerCmd.instanceName.isNull())
                    .or(taskSchedulerCmd.instanceName.trim().length().loe(0))
            )
            .where(taskSchedulerCmd.createAt.goe(minDate))
            .where(taskSchedulerCmd.state.eq(EnumConstant.SCHEDULER_CMD_STATE_0))
            .where(taskSchedulerCmd.cmdInfo.isNotNull())
            .orderBy(taskSchedulerCmd.createAt.asc())
            .fetch();
    }

    public boolean updateSchedulerCmdState(long id, int state, Integer oldState) {
        SQLUpdateClause update = queryDSL.update(taskSchedulerCmd)
            .set(taskSchedulerCmd.state, state)
            .set(taskSchedulerCmd.updateAt, currentDate())
            .where(taskSchedulerCmd.id.eq(id));
        if (oldState != null) {
            update.where(taskSchedulerCmd.state.eq(oldState));
        }
        return update.execute() > 0;
    }

    public boolean updateSchedulerCmdState(long id, int state) {
        return updateSchedulerCmdState(id, state, null);
    }

    public boolean saveSchedulerCmd(TaskSchedulerCmd schedulerCmd) {
        if (schedulerCmd == null) {
            return false;
        }
        if (schedulerCmd.getId() == null) {
            schedulerCmd.setId(snowFlake.nextId());
        }
        if (StringUtils.isBlank(schedulerCmd.getInstanceName())) {
            schedulerCmd.setInstanceName(null);
        }
        schedulerCmd.setState(EnumConstant.SCHEDULER_CMD_STATE_0);
        schedulerCmd.setCreateAt(currentDate());
        return queryDSL.insert(taskSchedulerCmd).populate(schedulerCmd).execute() > 0;
    }

    public TaskSchedulerCmd getSchedulerCmd(long id) {
        return queryDSL.selectFrom(taskSchedulerCmd).where(taskSchedulerCmd.id.eq(id)).fetchOne();
    }

    public boolean delSchedulerCmd(long id) {
        return queryDSL.delete(taskSchedulerCmd).where(taskSchedulerCmd.id.eq(id)).execute() > 0;
    }

    /**
     * 更新由于调度器节点关闭导致的任务日志状态为空的数据
     */
    public long updateZombieJobLog(String namespace, String instanceName) {
        return queryDSL.update(taskJobLog)
            .set(taskJobLog.status, EnumConstant.JOB_LOG_STATUS_2)
            .where(taskJobLog.namespace.eq(namespace))
            .where(taskJobLog.instanceName.eq(instanceName))
            .where(taskJobLog.status.isNull())
            .execute();
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
     * 开启一个新事务，在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     */
    public <T> T newBeginTX(TransactionCallback<T> action) {
        return jdbc.beginTX(action, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
