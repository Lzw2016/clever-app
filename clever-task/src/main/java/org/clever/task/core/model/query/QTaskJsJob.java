package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskJsJob;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * js脚本任务(task_js_job)
 */
@SuppressWarnings("ALL")
public class QTaskJsJob extends RelationalPathBase<TaskJsJob> {
    /** task_js_job表 */
    public static final QTaskJsJob taskJsJob = new QTaskJsJob("task_js_job");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** 文件内容 */
    public final StringPath content = createString("content");
    /** 读写权限：0-可读可写，1-只读 */
    public final NumberPath<Byte> readOnly = createNumber("readOnly", Byte.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskJsJob(String variable) {
        super(TaskJsJob.class, forVariable(variable), "test", "task_js_job");
        addMetadata();
    }

    public QTaskJsJob(String variable, String schema, String table) {
        super(TaskJsJob.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskJsJob(String variable, String schema) {
        super(TaskJsJob.class, forVariable(variable), schema, "task_js_job");
        addMetadata();
    }

    public QTaskJsJob(Path<? extends TaskJsJob> path) {
        super(path.getType(), path.getMetadata(), "test", "task_js_job");
        addMetadata();
    }

    public QTaskJsJob(PathMetadata metadata) {
        super(TaskJsJob.class, metadata, "test", "task_js_job");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(content, ColumnMetadata.named("content").withIndex(4).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(readOnly, ColumnMetadata.named("read_only").withIndex(5).ofType(Types.TINYINT).withSize(3));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(6).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(7).ofType(Types.TIMESTAMP).withSize(3));
    }
}
