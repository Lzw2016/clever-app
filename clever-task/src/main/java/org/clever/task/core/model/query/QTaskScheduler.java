package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskScheduler;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 调度器(task_scheduler)
 */
@SuppressWarnings("ALL")
public class QTaskScheduler extends RelationalPathBase<TaskScheduler> {
    /** task_scheduler表 */
    public static final QTaskScheduler taskScheduler = new QTaskScheduler("task_scheduler");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间(同一个namespace的不同调度器属于同一个集群) */
    public final StringPath namespace = createString("namespace");
    /** 调度器实例名称 */
    public final StringPath instanceName = createString("instanceName");
    /** 最后心跳时间 */
    public final DateTimePath<Date> lastHeartbeatTime = createDateTime("lastHeartbeatTime", Date.class);
    /** 心跳频率(单位：毫秒) */
    public final NumberPath<Long> heartbeatInterval = createNumber("heartbeatInterval", Long.class);
    /** 调度器配置，线程池大小、负载权重、最大并发任务数... */
    public final StringPath config = createString("config");
    /** 描述 */
    public final StringPath description = createString("description");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskScheduler(String variable) {
        super(TaskScheduler.class, forVariable(variable), "test", "task_scheduler");
        addMetadata();
    }

    public QTaskScheduler(String variable, String schema, String table) {
        super(TaskScheduler.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskScheduler(String variable, String schema) {
        super(TaskScheduler.class, forVariable(variable), schema, "task_scheduler");
        addMetadata();
    }

    public QTaskScheduler(Path<? extends TaskScheduler> path) {
        super(path.getType(), path.getMetadata(), "test", "task_scheduler");
        addMetadata();
    }

    public QTaskScheduler(PathMetadata metadata) {
        super(TaskScheduler.class, metadata, "test", "task_scheduler");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(instanceName, ColumnMetadata.named("instance_name").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(lastHeartbeatTime, ColumnMetadata.named("last_heartbeat_time").withIndex(4).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(heartbeatInterval, ColumnMetadata.named("heartbeat_interval").withIndex(5).ofType(Types.BIGINT).withSize(19));
        addMetadata(config, ColumnMetadata.named("config").withIndex(6).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(description, ColumnMetadata.named("description").withIndex(7).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(9).ofType(Types.TIMESTAMP).withSize(3));
    }
}
