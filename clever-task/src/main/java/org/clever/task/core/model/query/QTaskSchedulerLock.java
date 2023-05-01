package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskSchedulerLock;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 调度器集群锁(task_scheduler_lock)
 */
@SuppressWarnings("ALL")
public class QTaskSchedulerLock extends RelationalPathBase<TaskSchedulerLock> {
    /** task_scheduler_lock表 */
    public static final QTaskSchedulerLock taskSchedulerLock = new QTaskSchedulerLock("task_scheduler_lock");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 锁名称 */
    public final StringPath lockName = createString("lockName");
    /** 描述 */
    public final StringPath description = createString("description");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskSchedulerLock(String variable) {
        super(TaskSchedulerLock.class, forVariable(variable), "test", "task_scheduler_lock");
        addMetadata();
    }

    public QTaskSchedulerLock(String variable, String schema, String table) {
        super(TaskSchedulerLock.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskSchedulerLock(String variable, String schema) {
        super(TaskSchedulerLock.class, forVariable(variable), schema, "task_scheduler_lock");
        addMetadata();
    }

    public QTaskSchedulerLock(Path<? extends TaskSchedulerLock> path) {
        super(path.getType(), path.getMetadata(), "test", "task_scheduler_lock");
        addMetadata();
    }

    public QTaskSchedulerLock(PathMetadata metadata) {
        super(TaskSchedulerLock.class, metadata, "test", "task_scheduler_lock");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(lockName, ColumnMetadata.named("lock_name").withIndex(3).ofType(Types.VARCHAR).withSize(63));
        addMetadata(description, ColumnMetadata.named("description").withIndex(4).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(5).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(6).ofType(Types.TIMESTAMP).withSize(3));
    }
}
