package org.clever.task.manage.dao;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.SQLQuery;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.DateUtils;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.Page;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.jdbc.querydsl.utils.QueryDslUtils;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.JobInfo;
import org.clever.task.core.model.entity.*;
import org.clever.task.manage.model.JobLogInfo;
import org.clever.task.manage.model.request.*;
import org.clever.task.manage.model.response.*;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.clever.task.core.model.query.QTaskJob.taskJob;
import static org.clever.task.core.model.query.QTaskJobLog.taskJobLog;
import static org.clever.task.core.model.query.QTaskJobTrigger.taskJobTrigger;
import static org.clever.task.core.model.query.QTaskJobTriggerLog.taskJobTriggerLog;
import static org.clever.task.core.model.query.QTaskScheduler.taskScheduler;
import static org.clever.task.core.model.query.QTaskSchedulerLog.taskSchedulerLog;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/22 20:32 <br/>
 */
@SuppressWarnings("DuplicatedCode")
@Slf4j
public class TaskManageStore {
    private final Jdbc jdbc;
    @Getter
    private final QueryDSL queryDSL;

    public TaskManageStore(QueryDSL queryDSL) {
        Assert.notNull(queryDSL, "参数 queryDSL 不能为null");
        this.queryDSL = queryDSL;
        this.jdbc = queryDSL.getJdbc();
    }

    public List<String> allInstance(AllInstanceReq req) {
        SQLQuery<String> sqlQuery = queryDSL.select(taskScheduler.instanceName).distinct()
            .from(taskScheduler)
            .orderBy(taskScheduler.instanceName.asc());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskScheduler.namespace.eq(req.getNamespace()));
        }
        return sqlQuery.fetch();
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
            sqlQuery.where(taskJob.name.like(jdbc.likePrefix(query.getName())));
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
        if (query.getJobStatus() != null) {
            sqlQuery.where(taskJobLog.status.eq(query.getJobStatus()));
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

    public Page<JobLogInfo> queryJobLogInfo(JobLogInfoReq query) {
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
        if (StringUtils.isNotBlank(query.getJobName())) {
            sqlQuery.where(taskJob.name.like(jdbc.likePrefix(query.getJobName())));
        }
        if (query.getJobType() != null) {
            sqlQuery.where(taskJob.type.eq(query.getJobType()));
        }
        if (query.getJobStatus() != null) {
            sqlQuery.where(taskJobLog.status.eq(query.getJobStatus()));
        }
        if (query.getIsManual() != null) {
            sqlQuery.where(taskJobTriggerLog.isManual.eq(query.getIsManual()));
        }
        // if (query.getMisFired() != null) {
        //     sqlQuery.where(taskJobTriggerLog.misFired.eq(query.getMisFired()));
        // }
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
            sqlQuery.where(taskJobTrigger.name.like(jdbc.likePrefix(query.getName())));
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
        if (query.getIsManual() != null) {
            sqlQuery.where(taskJobTriggerLog.isManual.eq(query.getIsManual()));
        }
        if (query.getMisFired() != null) {
            sqlQuery.where(taskJobTriggerLog.misFired.eq(query.getMisFired()));
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

    public CartLineDataRes getCartLineDataRes(CartLineDataReq req) {
        if (req.getEnd() == null) {
            req.setEnd(new Date());
        }
        if (req.getStart() == null) {
            req.setStart(DateUtils.addMonths(req.getEnd(), -1));
        }
        if (req.getLimit() == null || req.getLimit() <= 0) {
            req.setLimit(30);
        }
        if (req.getLimit() > QueryByPage.PAGE_SIZE_MAX) {
            req.setLimit(QueryByPage.PAGE_SIZE_MAX);
        }
        Function<DateTimePath<Date>, StringExpression> getDateFormatField = field -> {
            // mysql        date_format(now(), '%Y-%m-%d %H:%i:%s')
            // oracle       to_char(sysdate, 'YYYY-MM-DD HH24:MI:SS')
            // postgre_sql  to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
            StringExpression fieldExpr;
            switch (queryDSL.getJdbc().getDbType()) {
                case MYSQL:
                    // noinspection DuplicateBranchesInSwitch
                    fieldExpr = Expressions.stringTemplate("date_format({0}, '%Y-%m-%d')", field);
                    break;
                case ORACLE:
                case ORACLE_12C:
                case POSTGRE_SQL:
                    fieldExpr = Expressions.stringTemplate("to_char({0}, 'YYYY-MM-DD')", field);
                    break;
                default:
                    fieldExpr = Expressions.stringTemplate("date_format({0}, '%Y-%m-%d')", field);
            }
            return fieldExpr;
        };
        CartLineDataRes res = new CartLineDataRes();
        // 任务运行统计数据
        StringExpression jobFireTime = getDateFormatField.apply(taskJobLog.fireTime);
        NumberTemplate<Integer> jobErrCountField = Expressions.numberTemplate(Integer.class, "sum(case when {0}=1 then 1 else 0 end)", taskJobLog.status);
        SQLQuery<Tuple> sqlQuery = queryDSL.select(jobFireTime, taskJobLog.fireTime.count(), jobErrCountField)
            .from(taskJobLog)
            .where(taskJobLog.fireTime.goe(req.getStart()))
            .where(taskJobLog.fireTime.loe(req.getEnd()))
            .groupBy(jobFireTime)
            .orderBy(jobFireTime.desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobLog.namespace.eq(req.getNamespace()));
        }
        if (StringUtils.isNotBlank(req.getInstanceName())) {
            sqlQuery.where(taskJobLog.instanceName.eq(req.getInstanceName()));
        }
        List<Tuple> tuples = sqlQuery.fetch();
        Map<String, Integer> jobCountMap = new HashMap<>(tuples.size());
        tuples.forEach(tuple -> jobCountMap.put(tuple.get(jobFireTime), Conv.asInteger(tuple.get(taskJobLog.fireTime.count()))));
        Map<String, Integer> jobErrCountMap = new HashMap<>(tuples.size());
        tuples.forEach(tuple -> jobErrCountMap.put(tuple.get(jobFireTime), Conv.asInteger(tuple.get(jobErrCountField))));
        StringExpression triggerFireTime = getDateFormatField.apply(taskJobTriggerLog.fireTime);
        sqlQuery = queryDSL.select(triggerFireTime, taskJobTriggerLog.fireTime.count())
            .from(taskJobTriggerLog)
            .where(taskJobTriggerLog.fireTime.goe(req.getStart()))
            .where(taskJobTriggerLog.fireTime.loe(req.getEnd()))
            .groupBy(triggerFireTime)
            .orderBy(triggerFireTime.desc())
            .limit(req.getLimit());
        if (StringUtils.isNotBlank(req.getNamespace())) {
            sqlQuery.where(taskJobTriggerLog.namespace.eq(req.getNamespace()));
        }
        if (StringUtils.isNotBlank(req.getInstanceName())) {
            sqlQuery.where(taskJobTriggerLog.instanceName.eq(req.getInstanceName()));
        }
        tuples = sqlQuery.fetch();
        Map<String, Integer> triggerCountMap = new HashMap<>(tuples.size());
        tuples.forEach(tuple -> triggerCountMap.put(tuple.get(triggerFireTime), Conv.asInteger(tuple.get(taskJobTriggerLog.fireTime.count()))));
        // 设置返回值
        Date startTime = req.getStart();
        String currentTimeStr = DateUtils.formatToString(startTime, DateUtils.yyyy_MM_dd);
        final String endTimeStr = DateUtils.formatToString(req.getEnd(), DateUtils.yyyy_MM_dd);
        while (currentTimeStr.compareTo(endTimeStr) <= 0) {
            Integer jobCount = jobCountMap.get(currentTimeStr);
            if (jobCount == null) {
                jobCount = 0;
            }
            Integer jobErrCount = jobErrCountMap.get(currentTimeStr);
            if (jobErrCount == null) {
                jobErrCount = 0;
            }
            Integer triggerCount = triggerCountMap.get(currentTimeStr);
            if (triggerCount == null) {
                triggerCount = 0;
            }
            res.getJob().add(new CartLineDataRes.CartLineItem(currentTimeStr, jobCount, jobErrCount, triggerCount));
            startTime = DateUtils.addDays(startTime, 1);
            currentTimeStr = DateUtils.formatToString(startTime, DateUtils.yyyy_MM_dd);
        }
        return res;
    }

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
            .orderBy(taskJobTrigger.nextFireTime.asc())
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
            .leftJoin(taskJob).on(taskJobTriggerLog.jobId.eq(taskJob.id))
            .where(taskJob.id.isNotNull())
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
                .findFirst().orElse(null);
            res.getMisfireJobs().add(jobEntity);
        }
        return res;
    }

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
            .leftJoin(taskJob).on(taskJobLog.jobId.eq(taskJob.id))
            .where(taskJob.id.isNotNull())
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
                .findFirst().orElse(null);
            res.getFailJobs().add(jobEntity);
        }
        return res;
    }

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
            .leftJoin(taskJob).on(taskJobLog.jobId.eq(taskJob.id))
            .where(taskJob.id.isNotNull())
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
                .findFirst().orElse(null);
            res.getMaxRunTimeJobs().add(jobEntity);
        }
        return res;
    }

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
            .leftJoin(taskJob).on(taskJobLog.jobId.eq(taskJob.id))
            .where(taskJob.id.isNotNull())
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
                .findFirst().orElse(null);
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
