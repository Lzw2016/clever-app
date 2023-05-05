package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskJobTrigger;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 任务触发器(task_job_trigger)
 */
@SuppressWarnings("ALL")
public class QTaskJobTrigger extends RelationalPathBase<TaskJobTrigger> {
    /** task_job_trigger表 */
    public static final QTaskJobTrigger taskJobTrigger = new QTaskJobTrigger("task_job_trigger");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** 触发器名称 */
    public final StringPath name = createString("name");
    /** 触发开始时间 */
    public final DateTimePath<Date> startTime = createDateTime("startTime", Date.class);
    /** 触发结束时间 */
    public final DateTimePath<Date> endTime = createDateTime("endTime", Date.class);
    /** 上一次触发时间 */
    public final DateTimePath<Date> lastFireTime = createDateTime("lastFireTime", Date.class);
    /** 下一次触发时间 */
    public final DateTimePath<Date> nextFireTime = createDateTime("nextFireTime", Date.class);
    /** 错过触发策略，1：忽略，2：立即补偿触发一次 */
    public final NumberPath<Integer> misfireStrategy = createNumber("misfireStrategy", Integer.class);
    /** 是否允许多节点并行触发，使用分布式锁实现，不建议允许，0：禁止，1：允许 */
    public final NumberPath<Integer> allowConcurrent = createNumber("allowConcurrent", Integer.class);
    /** 任务类型，1：cron触发，2：固定间隔触发 */
    public final NumberPath<Integer> type = createNumber("type", Integer.class);
    /** cron表达式 */
    public final StringPath cron = createString("cron");
    /** 固定间隔触发，间隔时间(单位：秒) */
    public final NumberPath<Long> fixedInterval = createNumber("fixedInterval", Long.class);
    /** 是否禁用：0-启用，1-禁用 */
    public final NumberPath<Integer> disable = createNumber("disable", Integer.class);
    /** 描述 */
    public final StringPath description = createString("description");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskJobTrigger(String variable) {
        super(TaskJobTrigger.class, forVariable(variable), "", "task_job_trigger");
        addMetadata();
    }

    public QTaskJobTrigger(String variable, String schema, String table) {
        super(TaskJobTrigger.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskJobTrigger(String variable, String schema) {
        super(TaskJobTrigger.class, forVariable(variable), schema, "task_job_trigger");
        addMetadata();
    }

    public QTaskJobTrigger(Path<? extends TaskJobTrigger> path) {
        super(path.getType(), path.getMetadata(), "", "task_job_trigger");
        addMetadata();
    }

    public QTaskJobTrigger(PathMetadata metadata) {
        super(TaskJobTrigger.class, metadata, "", "task_job_trigger");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(name, ColumnMetadata.named("name").withIndex(4).ofType(Types.VARCHAR).withSize(127));
        addMetadata(startTime, ColumnMetadata.named("start_time").withIndex(5).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(endTime, ColumnMetadata.named("end_time").withIndex(6).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(lastFireTime, ColumnMetadata.named("last_fire_time").withIndex(7).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(nextFireTime, ColumnMetadata.named("next_fire_time").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(misfireStrategy, ColumnMetadata.named("misfire_strategy").withIndex(9).ofType(Types.TINYINT).withSize(3));
        addMetadata(allowConcurrent, ColumnMetadata.named("allow_concurrent").withIndex(10).ofType(Types.TINYINT).withSize(3));
        addMetadata(type, ColumnMetadata.named("type").withIndex(11).ofType(Types.TINYINT).withSize(3));
        addMetadata(cron, ColumnMetadata.named("cron").withIndex(12).ofType(Types.VARCHAR).withSize(511));
        addMetadata(fixedInterval, ColumnMetadata.named("fixed_interval").withIndex(13).ofType(Types.BIGINT).withSize(19));
        addMetadata(disable, ColumnMetadata.named("disable").withIndex(14).ofType(Types.TINYINT).withSize(3));
        addMetadata(description, ColumnMetadata.named("description").withIndex(15).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(16).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(17).ofType(Types.TIMESTAMP).withSize(3));
    }
}
