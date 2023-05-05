package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskSchedulerLog;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 调度器事件日志(task_scheduler_log)
 */
@SuppressWarnings("ALL")
public class QTaskSchedulerLog extends RelationalPathBase<TaskSchedulerLog> {
    /** task_scheduler_log表 */
    public static final QTaskSchedulerLog taskSchedulerLog = new QTaskSchedulerLog("task_scheduler_log");

    /** 编号 */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 调度器实例名称 */
    public final StringPath instanceName = createString("instanceName");
    /** 事件名称 */
    public final StringPath eventName = createString("eventName");
    /** 事件日志数据 */
    public final StringPath logData = createString("logData");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);

    public QTaskSchedulerLog(String variable) {
        super(TaskSchedulerLog.class, forVariable(variable), "", "task_scheduler_log");
        addMetadata();
    }

    public QTaskSchedulerLog(String variable, String schema, String table) {
        super(TaskSchedulerLog.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskSchedulerLog(String variable, String schema) {
        super(TaskSchedulerLog.class, forVariable(variable), schema, "task_scheduler_log");
        addMetadata();
    }

    public QTaskSchedulerLog(Path<? extends TaskSchedulerLog> path) {
        super(path.getType(), path.getMetadata(), "", "task_scheduler_log");
        addMetadata();
    }

    public QTaskSchedulerLog(PathMetadata metadata) {
        super(TaskSchedulerLog.class, metadata, "", "task_scheduler_log");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(instanceName, ColumnMetadata.named("instance_name").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(eventName, ColumnMetadata.named("event_name").withIndex(4).ofType(Types.VARCHAR).withSize(63));
        addMetadata(logData, ColumnMetadata.named("log_data").withIndex(5).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(6).ofType(Types.TIMESTAMP).withSize(3));
    }
}
