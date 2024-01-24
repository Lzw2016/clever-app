package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskReport;

import java.sql.Types;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 任务执行报表(task_report)
 */
@SuppressWarnings("ALL")
public class QTaskReport extends RelationalPathBase<TaskReport> {
    /** task_report表 */
    public static final QTaskReport taskReport = new QTaskReport("task_report");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 报表时间 */
    public final StringPath reportTime = createString("reportTime");
    /** job 运行总次数 */
    public final NumberPath<Long> jobCount = createNumber("jobCount", Long.class);
    /** job 运行错误次数 */
    public final NumberPath<Long> jobErrCount = createNumber("jobErrCount", Long.class);
    /** 触发总次数 */
    public final NumberPath<Long> triggerCount = createNumber("triggerCount", Long.class);
    /** 错过触发次数 */
    public final NumberPath<Long> misfireCount = createNumber("misfireCount", Long.class);

    public QTaskReport(String variable) {
        super(TaskReport.class, forVariable(variable), "test", "task_report");
        addMetadata();
    }

    public QTaskReport(String variable, String schema, String table) {
        super(TaskReport.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskReport(String variable, String schema) {
        super(TaskReport.class, forVariable(variable), schema, "task_report");
        addMetadata();
    }

    public QTaskReport(Path<? extends TaskReport> path) {
        super(path.getType(), path.getMetadata(), "test", "task_report");
        addMetadata();
    }

    public QTaskReport(PathMetadata metadata) {
        super(TaskReport.class, metadata, "test", "task_report");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(reportTime, ColumnMetadata.named("report_time").withIndex(3).ofType(Types.VARCHAR).withSize(31));
        addMetadata(jobCount, ColumnMetadata.named("job_count").withIndex(4).ofType(Types.BIGINT).withSize(19));
        addMetadata(jobErrCount, ColumnMetadata.named("job_err_count").withIndex(5).ofType(Types.BIGINT).withSize(19));
        addMetadata(triggerCount, ColumnMetadata.named("trigger_count").withIndex(6).ofType(Types.BIGINT).withSize(19));
        addMetadata(misfireCount, ColumnMetadata.named("misfire_count").withIndex(7).ofType(Types.BIGINT).withSize(19));
    }
}
