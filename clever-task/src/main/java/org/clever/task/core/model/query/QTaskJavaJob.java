package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskJavaJob;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * js脚本任务(task_java_job)
 */
@SuppressWarnings("ALL")
public class QTaskJavaJob extends RelationalPathBase<TaskJavaJob> {
    /** task_java_job表 */
    public static final QTaskJavaJob taskJavaJob = new QTaskJavaJob("task_java_job");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** 是否是静态方法(函数)，0：非静态，1：静态 */
    public final NumberPath<Integer> isStatic = createNumber("isStatic", Integer.class);
    /** java class全路径 */
    public final StringPath className = createString("className");
    /** java class method */
    public final StringPath classMethod = createString("classMethod");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskJavaJob(String variable) {
        super(TaskJavaJob.class, forVariable(variable), "", "task_java_job");
        addMetadata();
    }

    public QTaskJavaJob(String variable, String schema, String table) {
        super(TaskJavaJob.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskJavaJob(String variable, String schema) {
        super(TaskJavaJob.class, forVariable(variable), schema, "task_java_job");
        addMetadata();
    }

    public QTaskJavaJob(Path<? extends TaskJavaJob> path) {
        super(path.getType(), path.getMetadata(), "", "task_java_job");
        addMetadata();
    }

    public QTaskJavaJob(PathMetadata metadata) {
        super(TaskJavaJob.class, metadata, "", "task_java_job");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(isStatic, ColumnMetadata.named("is_static").withIndex(4).ofType(Types.TINYINT).withSize(3));
        addMetadata(className, ColumnMetadata.named("class_name").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(classMethod, ColumnMetadata.named("class_method").withIndex(6).ofType(Types.VARCHAR).withSize(63));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(7).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
    }
}
