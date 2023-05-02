package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskShellJob;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * js脚本任务(task_shell_job)
 */
@SuppressWarnings("ALL")
public class QTaskShellJob extends RelationalPathBase<TaskShellJob> {
    /** task_shell_job表 */
    public static final QTaskShellJob taskShellJob = new QTaskShellJob("task_shell_job");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** shell脚本类型：bash|sh|ash|powershell|cmd|python|node|deno|php */
    public final StringPath shellType = createString("shellType");
    /** 文件内容 */
    public final StringPath content = createString("content");
    /** 读写权限：0-可读可写，1-只读 */
    public final NumberPath<Integer> readOnly = createNumber("readOnly", Integer.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskShellJob(String variable) {
        super(TaskShellJob.class, forVariable(variable), "test", "task_shell_job");
        addMetadata();
    }

    public QTaskShellJob(String variable, String schema, String table) {
        super(TaskShellJob.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskShellJob(String variable, String schema) {
        super(TaskShellJob.class, forVariable(variable), schema, "task_shell_job");
        addMetadata();
    }

    public QTaskShellJob(Path<? extends TaskShellJob> path) {
        super(path.getType(), path.getMetadata(), "test", "task_shell_job");
        addMetadata();
    }

    public QTaskShellJob(PathMetadata metadata) {
        super(TaskShellJob.class, metadata, "test", "task_shell_job");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(shellType, ColumnMetadata.named("shell_type").withIndex(4).ofType(Types.VARCHAR).withSize(15));
        addMetadata(content, ColumnMetadata.named("content").withIndex(5).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(readOnly, ColumnMetadata.named("read_only").withIndex(6).ofType(Types.TINYINT).withSize(3));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(7).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
    }
}
