package org.clever.task.core;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.clever.core.DateUtils;
import org.clever.core.id.SnowFlake;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.model.entity.*;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.util.Date;
import java.util.List;
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
public class TaskStore {
    private final SnowFlake snowFlake;
    private final Jdbc jdbc;
    private final QueryDSL queryDSL;

    public TaskStore(SnowFlake snowFlake, QueryDSL queryDSL) {
        Assert.notNull(snowFlake, "参数 snowFlake 不能为null");
        Assert.notNull(queryDSL, "参数 queryDSL 不能为null");
        this.snowFlake = snowFlake;
        this.queryDSL = queryDSL;
        this.jdbc = queryDSL.getJdbc();
    }

    public TaskStore(SnowFlake snowFlake) {
        Assert.notNull(snowFlake, "参数 snowFlake 不能为null");
        this.snowFlake = snowFlake;
        jdbc = TaskDataSource.getJdbc();
        queryDSL = TaskDataSource.getQueryDSL();
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- dao

    /**
     * 获取数据库当前时间戳(毫秒)
     */
    public long currentTimeMillis() {
        return jdbc.currentDate().getTime();
    }

    /**
     * 获取数据库当前时间戳(精确到毫秒)
     */
    public Date currentDate() {
        return jdbc.currentDate();
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
                        Long.TYPE, Ops.DateTimeOps.DIFF_SECONDS, Expressions.currentTimestamp(), taskScheduler.lastHeartbeatTime
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
                        Long.TYPE, Ops.DateTimeOps.DIFF_SECONDS, Expressions.currentTimestamp(), taskScheduler.lastHeartbeatTime
                ).multiply(1000)
        ).as("available");
        List<Tuple> list = queryDSL.select(taskScheduler, available)
                .from(taskScheduler)
                .where(taskScheduler.namespace.eq(namespace))
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
     * 接下来N秒内需要触发的触发器列表
     *
     * @param nextTime N秒对应的毫秒时间
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
                .limit(1200)
                .fetch();
    }

    /**
     * 根据 namespace jobTriggerId 查询
     */
    public TaskJobTrigger getTrigger(String namespace, Long jobTriggerId) {
        return queryDSL.select(taskJobTrigger)
                .from(taskJobTrigger)
                .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
                .where(taskJobTrigger.namespace.eq(namespace))
                .where(taskJobTrigger.id.eq(jobTriggerId))
                .fetchOne();
    }

    /**
     * 获取定时任务悲观锁 TODO 乐观锁
     */
    public boolean getLockJob(String namespace, Long jobId, Long lockVersion) {
        return queryDSL.update(taskJob)
                .set(taskJob.lockVersion, taskJob.lockVersion.add(1))
                .where(taskJob.id.eq(jobId))
                .where(taskJob.namespace.eq(namespace))
                .where(taskJob.lockVersion.eq(lockVersion))
                .execute() > 0;
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
     * 查询当前集群所有定时任务信息
     */
    public List<TaskJob> queryAllJob(String namespace) {
        return queryDSL.select(taskJob)
                .from(taskJob)
                .where(taskJob.namespace.eq(namespace))
                .fetch();
    }

    public int updateDisableJob(String namespace, Integer disable, Long... jobIds) {
        return (int) queryDSL.update(taskJob)
                .set(taskJob.disable, disable)
                .where(taskJob.namespace.eq(namespace))
                .where(taskJob.id.in(jobIds))
                .execute();
    }

    public int updateDisableTrigger(String namespace, Integer disable, Long... triggerIds) {
        return (int) queryDSL.update(taskJobTrigger)
                .set(taskJobTrigger.disable, disable)
                .where(taskJobTrigger.namespace.eq(namespace))
                .where(taskJobTrigger.id.in(triggerIds))
                .execute();
    }

    /**
     * 获取HttpJob
     */
    public TaskHttpJob getHttpJob(String namespace, Long jobId) {
        return queryDSL.select(taskHttpJob)
                .from(taskHttpJob)
                .where(taskHttpJob.namespace.eq(namespace))
                .where(taskHttpJob.id.eq(jobId))
                .fetchOne();
    }

    /**
     * 获取JavaJob
     */
    public TaskJavaJob getJavaJob(String namespace, Long jobId) {
        return queryDSL.select(taskJavaJob)
                .from(taskJavaJob)
                .where(taskJavaJob.namespace.eq(namespace))
                .where(taskJavaJob.id.eq(jobId))
                .fetchOne();
    }

    /**
     * 获取JsJob
     */
    public TaskJsJob getJsJob(String namespace, Long jobId) {
        return queryDSL.select(taskJsJob)
                .from(taskJsJob)
                .where(taskJsJob.namespace.eq(namespace))
                .where(taskJsJob.id.eq(jobId))
                .fetchOne();
    }

    /**
     * 获取ShellJob
     */
    public TaskShellJob getShellJob(String namespace, Long jobId) {
        return queryDSL.select(taskShellJob)
                .from(taskShellJob)
                .where(taskShellJob.namespace.eq(namespace))
                .where(taskShellJob.id.eq(jobId))
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
    public void updateFireTime(TaskJobTrigger jobTrigger) {
        queryDSL.update(taskJobTrigger)
                .set(taskJobTrigger.lastFireTime, jobTrigger.getLastFireTime())
                .set(taskJobTrigger.nextFireTime, jobTrigger.getNextFireTime())
                .where(taskJobTrigger.id.eq(jobTrigger.getId()))
                .where(taskJobTrigger.namespace.eq(jobTrigger.getNamespace()))
                .execute();
    }

    /**
     * 获取触发器悲观锁 TODO 乐观锁
     */
    public boolean getLockTrigger(String namespace, Long jobTriggerId, Long lockVersion) {
        return queryDSL.update(taskJobTrigger)
                .set(taskJobTrigger.lockVersion, taskJobTrigger.lockVersion.add(1))
                .where(taskJobTrigger.id.eq(jobTriggerId))
                .where(taskJobTrigger.namespace.eq(namespace))
                .where(taskJobTrigger.lockVersion.eq(lockVersion))
                .execute() > 0;
    }

    public int addSchedulerLog(TaskSchedulerLog schedulerLog) {
        schedulerLog.setId(snowFlake.nextId());
        schedulerLog.setCreateAt(currentDate());
        return (int) queryDSL.insert(taskSchedulerLog).populate(schedulerLog).execute();
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

    // ---------------------------------------------------------------------------------------------------------------------------------------- manage

    public int addJob(TaskJob job) {
        job.setId(snowFlake.nextId());
        job.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJob).populate(job).execute();
    }

    public int addJobTrigger(TaskJobTrigger jobTrigger) {
        jobTrigger.setId(snowFlake.nextId());
        jobTrigger.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJobTrigger).populate(jobTrigger).execute();
    }

    public int addHttpJob(TaskHttpJob httpJob) {
        httpJob.setId(snowFlake.nextId());
        httpJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskHttpJob).populate(httpJob).execute();
    }

    public int addJavaJob(TaskJavaJob javaJob) {
        javaJob.setId(snowFlake.nextId());
        javaJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJavaJob).populate(javaJob).execute();
    }

    public int addJsJob(TaskJsJob jsJob) {
        jsJob.setId(snowFlake.nextId());
        jsJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskJsJob).populate(jsJob).execute();
    }

    public int addShellJob(TaskShellJob shellJob) {
        shellJob.setId(snowFlake.nextId());
        shellJob.setCreateAt(queryDSL.currentDate());
        return (int) queryDSL.insert(taskShellJob).populate(shellJob).execute();
    }

    public void delTriggerByJobId(String namespace, Long jobId) {
        queryDSL.delete(taskJobTrigger)
                .where(taskJobTrigger.namespace.eq(namespace))
                .where(taskJobTrigger.jobId.eq(jobId))
                .execute();
    }

    public void delJobByJobId(String namespace, Long jobId) {
        queryDSL.delete(taskJob)
                .where(taskJob.namespace.eq(namespace))
                .where(taskJob.id.eq(jobId))
                .execute();
    }

    public void delHttpJobByJobId(String namespace, Long jobId) {
        queryDSL.delete(taskHttpJob)
                .where(taskHttpJob.namespace.eq(namespace))
                .where(taskHttpJob.jobId.eq(jobId))
                .execute();
    }

    public void delJavaJobByJobId(String namespace, Long jobId) {
        queryDSL.delete(taskJavaJob)
                .where(taskJavaJob.namespace.eq(namespace))
                .where(taskJavaJob.jobId.eq(jobId))
                .execute();
    }

    public void delJsJobByJobId(String namespace, Long jobId) {
        queryDSL.delete(taskJsJob)
                .where(taskJsJob.namespace.eq(namespace))
                .where(taskJsJob.jobId.eq(jobId))
                .execute();
    }

    public void delShellJobByJobId(String namespace, Long jobId) {
        queryDSL.delete(taskShellJob)
                .where(taskShellJob.namespace.eq(namespace))
                .where(taskShellJob.jobId.eq(jobId))
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
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action) {
        return jdbc.beginReadOnlyTX(action);
    }
}
