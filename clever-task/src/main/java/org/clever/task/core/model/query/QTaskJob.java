package org.clever.task.core.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.task.core.model.entity.TaskJob;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 定时任务(task_job)
 */
@SuppressWarnings("ALL")
public class QTaskJob extends RelationalPathBase<TaskJob> {
    /** task_job表 */
    public static final QTaskJob taskJob = new QTaskJob("task_job");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 命名空间 */
    public final StringPath namespace = createString("namespace");
    /** 任务名称 */
    public final StringPath name = createString("name");
    /** 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本 */
    public final NumberPath<Integer> type = createNumber("type", Integer.class);
    /** 最大重入执行数量(对于单个节点当前任务未执行完成就触发了下一次执行导致任务重入执行)，小于等于0：表示禁止重入执行 */
    public final NumberPath<Integer> maxReentry = createNumber("maxReentry", Integer.class);
    /** 是否允许多节点并发执行，使用分布式锁实现，不建议禁止，0：禁止，1：允许 */
    public final NumberPath<Integer> allowConcurrent = createNumber("allowConcurrent", Integer.class);
    /** 执行失败时的最大重试次数 */
    public final NumberPath<Integer> maxRetryCount = createNumber("maxRetryCount", Integer.class);
    /** 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单 */
    public final NumberPath<Integer> routeStrategy = createNumber("routeStrategy", Integer.class);
    /** 路由策略，1-指定节点优先，调度器名称集合 */
    public final StringPath firstScheduler = createString("firstScheduler");
    /** 路由策略，2-固定节点白名单，调度器名称集合 */
    public final StringPath whitelistScheduler = createString("whitelistScheduler");
    /** 路由策略，3-固定节点黑名单，调度器名称集合 */
    public final StringPath blacklistScheduler = createString("blacklistScheduler");
    /** 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH */
    public final NumberPath<Integer> loadBalance = createNumber("loadBalance", Integer.class);
    /** 是否更新任务数据，0：不更新，1：更新 */
    public final NumberPath<Integer> isUpdateData = createNumber("isUpdateData", Integer.class);
    /** 任务数据(json格式) */
    public final StringPath jobData = createString("jobData");
    /** 运行次数 */
    public final NumberPath<Long> runCount = createNumber("runCount", Long.class);
    /** 是否禁用：0-启用，1-禁用 */
    public final NumberPath<Integer> disable = createNumber("disable", Integer.class);
    /** 描述 */
    public final StringPath description = createString("description");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QTaskJob(String variable) {
        super(TaskJob.class, forVariable(variable), "", "task_job");
        addMetadata();
    }

    public QTaskJob(String variable, String schema, String table) {
        super(TaskJob.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTaskJob(String variable, String schema) {
        super(TaskJob.class, forVariable(variable), schema, "task_job");
        addMetadata();
    }

    public QTaskJob(Path<? extends TaskJob> path) {
        super(path.getType(), path.getMetadata(), "", "task_job");
        addMetadata();
    }

    public QTaskJob(PathMetadata metadata) {
        super(TaskJob.class, metadata, "", "task_job");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(namespace, ColumnMetadata.named("namespace").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(name, ColumnMetadata.named("name").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(type, ColumnMetadata.named("type").withIndex(4).ofType(Types.TINYINT).withSize(3));
        addMetadata(maxReentry, ColumnMetadata.named("max_reentry").withIndex(5).ofType(Types.TINYINT).withSize(3));
        addMetadata(allowConcurrent, ColumnMetadata.named("allow_concurrent").withIndex(6).ofType(Types.TINYINT).withSize(3));
        addMetadata(maxRetryCount, ColumnMetadata.named("max_retry_count").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(routeStrategy, ColumnMetadata.named("route_strategy").withIndex(8).ofType(Types.TINYINT).withSize(3));
        addMetadata(firstScheduler, ColumnMetadata.named("first_scheduler").withIndex(9).ofType(Types.VARCHAR).withSize(2047));
        addMetadata(whitelistScheduler, ColumnMetadata.named("whitelist_scheduler").withIndex(10).ofType(Types.VARCHAR).withSize(2047));
        addMetadata(blacklistScheduler, ColumnMetadata.named("blacklist_scheduler").withIndex(11).ofType(Types.VARCHAR).withSize(2047));
        addMetadata(loadBalance, ColumnMetadata.named("load_balance").withIndex(12).ofType(Types.TINYINT).withSize(3));
        addMetadata(isUpdateData, ColumnMetadata.named("is_update_data").withIndex(13).ofType(Types.TINYINT).withSize(3));
        addMetadata(jobData, ColumnMetadata.named("job_data").withIndex(14).ofType(Types.LONGVARCHAR).withSize(65535));
        addMetadata(runCount, ColumnMetadata.named("run_count").withIndex(15).ofType(Types.BIGINT).withSize(19));
        addMetadata(disable, ColumnMetadata.named("disable").withIndex(16).ofType(Types.TINYINT).withSize(3));
        addMetadata(description, ColumnMetadata.named("description").withIndex(17).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(18).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(19).ofType(Types.TIMESTAMP).withSize(3));
    }
}
