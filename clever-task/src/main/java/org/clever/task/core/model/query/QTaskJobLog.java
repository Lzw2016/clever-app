package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskJobLog;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 任务执行日志(task_job_log)
 */
@SuppressWarnings("ALL")
public class QTaskJobLog extends RelationalPathBase<TaskJobLog> {
    /** task_job_log表 */
    public static final QTaskJobLog taskJobLog = new QTaskJobLog("task_job_log");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 调度器实例名称 */
    public final StringPath instanceName = createString("instanceName");
    /** 对应的触发器日志ID */
    public final NumberPath<Long> jobTriggerLogId = createNumber("jobTriggerLogId", Long.class);
    /** 任务触发器ID */
    public final NumberPath<Long> jobTriggerId = createNumber("jobTriggerId", Long.class);
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** 触发时间 */
    public final DateTimePath<Date> fireTime = createDateTime("fireTime", Date.class);
    /** 开始执行时间 */
    public final DateTimePath<Date> startTime = createDateTime("startTime", Date.class);
    /** 执行结束时间 */
    public final DateTimePath<Date> endTime = createDateTime("endTime", Date.class);
    /** 执行耗时(单位：毫秒) */
    public final NumberPath<Integer> runTime = createNumber("runTime", Integer.class);
    /** 任务执行结果，0：成功，1：失败，2：取消 */
    public final NumberPath<Integer> status = createNumber("status", Integer.class);
    /** 重试次数 */
    public final NumberPath<Integer> retryCount = createNumber("retryCount", Integer.class);
    /** 异常信息 */
    public final StringPath exceptionInfo = createString("exceptionInfo");
    /** 执行次数 */
    public final NumberPath<Long> runCount = createNumber("runCount", Long.class);
    /** 执行前的任务数据 */
    public final StringPath beforeJobData = createString("beforeJobData");
    /** 执行后的任务数据 */
    public final StringPath afterJobData = createString("afterJobData");

    public QTaskJobLog(String variable) {
        super(TaskJobLog.class, forVariable(variable), "", "task_job_log");
        addMetadata();
    }

    public QTaskJobLog(String variable, String schema, String table) {
        super(TaskJobLog.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskJobLog(String variable, String schema) {
        super(TaskJobLog.class, forVariable(variable), schema, "task_job_log");
        addMetadata();
    }

    public QTaskJobLog(Path<? extends TaskJobLog> path) {
        super(path.getType(), path.getMetadata(), "", "task_job_log");
        addMetadata();
    }

    public QTaskJobLog(PathMetadata metadata) {
        super(TaskJobLog.class, metadata, "", "task_job_log");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(instanceName, ColumnMetadata.named("instance_name").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(jobTriggerLogId, ColumnMetadata.named("job_trigger_log_id").withIndex(4).ofType(Types.BIGINT).withSize(19));
        addMetadata(jobTriggerId, ColumnMetadata.named("job_trigger_id").withIndex(5).ofType(Types.BIGINT).withSize(19));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(6).ofType(Types.BIGINT).withSize(19));
        addMetadata(fireTime, ColumnMetadata.named("fire_time").withIndex(7).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(startTime, ColumnMetadata.named("start_time").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(endTime, ColumnMetadata.named("end_time").withIndex(9).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(runTime, ColumnMetadata.named("run_time").withIndex(10).ofType(Types.INTEGER).withSize(10));
        addMetadata(status, ColumnMetadata.named("status").withIndex(11).ofType(Types.TINYINT).withSize(3));
        addMetadata(retryCount, ColumnMetadata.named("retry_count").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(exceptionInfo, ColumnMetadata.named("exception_info").withIndex(13).ofType(Types.VARCHAR).withSize(2047));
        addMetadata(runCount, ColumnMetadata.named("run_count").withIndex(14).ofType(Types.BIGINT).withSize(19));
        addMetadata(beforeJobData, ColumnMetadata.named("before_job_data").withIndex(15).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(afterJobData, ColumnMetadata.named("after_job_data").withIndex(16).ofType(Types.LONGVARCHAR).withSize(65535));
    }
}
