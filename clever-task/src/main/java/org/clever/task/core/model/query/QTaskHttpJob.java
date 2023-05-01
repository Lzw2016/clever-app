package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskHttpJob;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * Http任务(task_http_job)
 */
@SuppressWarnings("ALL")
public class QTaskHttpJob extends RelationalPathBase<TaskHttpJob> {
    /** task_http_job表 */
    public static final QTaskHttpJob taskHttpJob = new QTaskHttpJob("task_http_job");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 任务ID */
    public final NumberPath<Long> jobId = createNumber("jobId", Long.class);
    /** http请求method，ALL GET HEAD POST PUT DELETE CONNECT OPTIONS TRACE PATCH */
    public final StringPath requestMethod = createString("requestMethod");
    /** Http请求地址 */
    public final StringPath requestUrl = createString("requestUrl");
    /** Http请求数据json格式，包含：params、headers、body */
    public final StringPath requestData = createString("requestData");
    /** Http请求是否成功校验(js脚本) */
    public final StringPath successCheck = createString("successCheck");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskHttpJob(String variable) {
        super(TaskHttpJob.class, forVariable(variable), "test", "task_http_job");
        addMetadata();
    }

    public QTaskHttpJob(String variable, String schema, String table) {
        super(TaskHttpJob.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskHttpJob(String variable, String schema) {
        super(TaskHttpJob.class, forVariable(variable), schema, "task_http_job");
        addMetadata();
    }

    public QTaskHttpJob(Path<? extends TaskHttpJob> path) {
        super(path.getType(), path.getMetadata(), "test", "task_http_job");
        addMetadata();
    }

    public QTaskHttpJob(PathMetadata metadata) {
        super(TaskHttpJob.class, metadata, "test", "task_http_job");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(jobId, ColumnMetadata.named("job_id").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(requestMethod, ColumnMetadata.named("request_method").withIndex(4).ofType(Types.VARCHAR).withSize(15));
        addMetadata(requestUrl, ColumnMetadata.named("request_url").withIndex(5).ofType(Types.VARCHAR).withSize(511));
        addMetadata(requestData, ColumnMetadata.named("request_data").withIndex(6).ofType(Types.LONGVARCHAR).withSize(16777215));
        addMetadata(successCheck, ColumnMetadata.named("success_check").withIndex(7).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(9).ofType(Types.TIMESTAMP).withSize(3));
    }
}
