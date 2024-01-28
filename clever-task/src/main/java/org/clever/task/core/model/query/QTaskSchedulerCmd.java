package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskSchedulerCmd;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 调度器指令(task_scheduler_cmd)
 */
@SuppressWarnings("ALL")
public class QTaskSchedulerCmd extends RelationalPathBase<TaskSchedulerCmd> {
    /** task_scheduler_cmd表 */
    public static final QTaskSchedulerCmd taskSchedulerCmd = new QTaskSchedulerCmd("task_scheduler_cmd");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 指定的调度器实例名称，为空表示不指定 */
    public final StringPath instanceName = createString("instanceName");
    /** 指令信息 */
    public final StringPath cmdInfo = createString("cmdInfo");
    /** 指令执行状态，0：未执行，1：执行中，2：执行完成 */
    public final NumberPath<Integer> state = createNumber("state", Integer.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskSchedulerCmd(String variable) {
        super(TaskSchedulerCmd.class, forVariable(variable), "test", "task_scheduler_cmd");
        addMetadata();
    }

    public QTaskSchedulerCmd(String variable, String schema, String table) {
        super(TaskSchedulerCmd.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskSchedulerCmd(String variable, String schema) {
        super(TaskSchedulerCmd.class, forVariable(variable), schema, "task_scheduler_cmd");
        addMetadata();
    }

    public QTaskSchedulerCmd(Path<? extends TaskSchedulerCmd> path) {
        super(path.getType(), path.getMetadata(), "test", "task_scheduler_cmd");
        addMetadata();
    }

    public QTaskSchedulerCmd(PathMetadata metadata) {
        super(TaskSchedulerCmd.class, metadata, "test", "task_scheduler_cmd");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(instanceName, ColumnMetadata.named("instance_name").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(cmdInfo, ColumnMetadata.named("cmd_info").withIndex(4).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(state, ColumnMetadata.named("state").withIndex(5).ofType(Types.TINYINT).withSize(3));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(6).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(7).ofType(Types.TIMESTAMP).withSize(3));
    }
}
