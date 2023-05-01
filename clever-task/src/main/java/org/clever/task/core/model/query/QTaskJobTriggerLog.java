package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskJobTriggerLog;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 任务触发器日志(task_job_trigger_log)
 */
@SuppressWarnings("ALL")
public class QTaskJobTriggerLog extends RelationalPathBase<TaskJobTriggerLog> {
    /** task_job_trigger_log表 */
    public static final QTaskJobTriggerLog taskJobTriggerLog = new QTaskJobTriggerLog("task_job_trigger_log");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 调度器实例名称 */
    public final StringPath instanceName = createString("instanceName");
    /** 任务触发器ID */
    public final NumberPath<Long> jobTriggerId = createNumber("jobTriggerId", Long.class);
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** 触发器名称 */
    public final StringPath triggerName = createString("triggerName");
    /** 触发时间 */
    public final DateTimePath<Date> fireTime = createDateTime("fireTime", Date.class);
    /** 是否是手动触发，0：系统自动触发，1：用户手动触发 */
    public final NumberPath<Byte> isManual = createNumber("isManual", Byte.class);
    /** 触发耗时(单位：毫秒) */
    public final NumberPath<Integer> triggerTime = createNumber("triggerTime", Integer.class);
    /** 上一次触发时间 */
    public final DateTimePath<Date> lastFireTime = createDateTime("lastFireTime", Date.class);
    /** 下一次触发时间 */
    public final DateTimePath<Date> nextFireTime = createDateTime("nextFireTime", Date.class);
    /** 触发次数 */
    public final NumberPath<Long> fireCount = createNumber("fireCount", Long.class);
    /** 是否错过了触发，0：否，1：是 */
    public final NumberPath<Byte> misFired = createNumber("misFired", Byte.class);
    /** 触发器消息 */
    public final StringPath triggerMsg = createString("triggerMsg");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);

    public QTaskJobTriggerLog(String variable) {
        super(TaskJobTriggerLog.class, forVariable(variable), "test", "task_job_trigger_log");
        addMetadata();
    }

    public QTaskJobTriggerLog(String variable, String schema, String table) {
        super(TaskJobTriggerLog.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskJobTriggerLog(String variable, String schema) {
        super(TaskJobTriggerLog.class, forVariable(variable), schema, "task_job_trigger_log");
        addMetadata();
    }

    public QTaskJobTriggerLog(Path<? extends TaskJobTriggerLog> path) {
        super(path.getType(), path.getMetadata(), "test", "task_job_trigger_log");
        addMetadata();
    }

    public QTaskJobTriggerLog(PathMetadata metadata) {
        super(TaskJobTriggerLog.class, metadata, "test", "task_job_trigger_log");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(instanceName, ColumnMetadata.named("instance_name").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(jobTriggerId, ColumnMetadata.named("job_trigger_id").withIndex(4).ofType(Types.BIGINT).withSize(19));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(5).ofType(Types.BIGINT).withSize(19));
        addMetadata(triggerName, ColumnMetadata.named("trigger_name").withIndex(6).ofType(Types.VARCHAR).withSize(127));
        addMetadata(fireTime, ColumnMetadata.named("fire_time").withIndex(7).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(isManual, ColumnMetadata.named("is_manual").withIndex(8).ofType(Types.TINYINT).withSize(3));
        addMetadata(triggerTime, ColumnMetadata.named("trigger_time").withIndex(9).ofType(Types.INTEGER).withSize(10));
        addMetadata(lastFireTime, ColumnMetadata.named("last_fire_time").withIndex(10).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(nextFireTime, ColumnMetadata.named("next_fire_time").withIndex(11).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(fireCount, ColumnMetadata.named("fire_count").withIndex(12).ofType(Types.BIGINT).withSize(19));
        addMetadata(misFired, ColumnMetadata.named("mis_fired").withIndex(13).ofType(Types.TINYINT).withSize(3));
        addMetadata(triggerMsg, ColumnMetadata.named("trigger_msg").withIndex(14).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(15).ofType(Types.TIMESTAMP).withSize(3));
    }
}
