package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskJobConsoleLog;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 任务控制台日志(task_job_console_log)
 */
@SuppressWarnings("ALL")
public class QTaskJobConsoleLog extends RelationalPathBase<TaskJobConsoleLog> {
    /** task_job_console_log表 */
    public static final QTaskJobConsoleLog taskJobConsoleLog = new QTaskJobConsoleLog("task_job_console_log");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 调度器实例名称 */
    public final StringPath instanceName = createString("instanceName");
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** 任务执行日志ID */
    public final NumberPath<Long> jobLogId = createNumber("jobLogId", Long.class);
    /** 日志行号 */
    public final NumberPath<Integer> lineNum = createNumber("lineNum", Integer.class);
    /** 日志内容 */
    public final StringPath log = createString("log");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);

    public QTaskJobConsoleLog(String variable) {
        super(TaskJobConsoleLog.class, forVariable(variable), "test", "task_job_console_log");
        addMetadata();
    }

    public QTaskJobConsoleLog(String variable, String schema, String table) {
        super(TaskJobConsoleLog.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskJobConsoleLog(String variable, String schema) {
        super(TaskJobConsoleLog.class, forVariable(variable), schema, "task_job_console_log");
        addMetadata();
    }

    public QTaskJobConsoleLog(Path<? extends TaskJobConsoleLog> path) {
        super(path.getType(), path.getMetadata(), "test", "task_job_console_log");
        addMetadata();
    }

    public QTaskJobConsoleLog(PathMetadata metadata) {
        super(TaskJobConsoleLog.class, metadata, "test", "task_job_console_log");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(instanceName, ColumnMetadata.named("instance_name").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(4).ofType(Types.BIGINT).withSize(19));
        addMetadata(jobLogId, ColumnMetadata.named("job_log_id").withIndex(5).ofType(Types.BIGINT).withSize(19));
        addMetadata(lineNum, ColumnMetadata.named("line_num").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(log, ColumnMetadata.named("log").withIndex(7).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
    }
}
