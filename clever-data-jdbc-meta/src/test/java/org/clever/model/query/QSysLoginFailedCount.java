package org.clever.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.model.entity.SysLoginFailedCount;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 连续登录失败次数(缓存表)(sys_login_failed_count)
 */
@SuppressWarnings("ALL")
public class QSysLoginFailedCount extends RelationalPathBase<SysLoginFailedCount> {
    /** sys_login_failed_count表 */
    public static final QSysLoginFailedCount sysLoginFailedCount = new QSysLoginFailedCount("sys_login_failed_count");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 用户id */
    public final NumberPath<Long> userId = createNumber("userId", Long.class);
    /** 登录方式 */
    public final NumberPath<Integer> loginType = createNumber("loginType", Integer.class);
    /** 登录失败次数 */
    public final NumberPath<Integer> failedCount = createNumber("failedCount", Integer.class);
    /** 最后登录失败时间 */
    public final DateTimePath<Date> lastLoginTime = createDateTime("lastLoginTime", Date.class);
    /** 数据删除标志: 0:未删除，1:已删除 */
    public final NumberPath<Integer> deleteFlag = createNumber("deleteFlag", Integer.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QSysLoginFailedCount(String variable) {
        super(SysLoginFailedCount.class, forVariable(variable), "test", "sys_login_failed_count");
        addMetadata();
    }

    public QSysLoginFailedCount(String variable, String schema, String table) {
        super(SysLoginFailedCount.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QSysLoginFailedCount(String variable, String schema) {
        super(SysLoginFailedCount.class, forVariable(variable), schema, "sys_login_failed_count");
        addMetadata();
    }

    public QSysLoginFailedCount(Path<? extends SysLoginFailedCount> path) {
        super(path.getType(), path.getMetadata(), "test", "sys_login_failed_count");
        addMetadata();
    }

    public QSysLoginFailedCount(PathMetadata metadata) {
        super(SysLoginFailedCount.class, metadata, "test", "sys_login_failed_count");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(userId, ColumnMetadata.named("user_id").withIndex(2).ofType(Types.BIGINT).withSize(19));
        addMetadata(loginType, ColumnMetadata.named("login_type").withIndex(3).ofType(Types.INTEGER).withSize(10));
        addMetadata(failedCount, ColumnMetadata.named("failed_count").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(lastLoginTime, ColumnMetadata.named("last_login_time").withIndex(5).ofType(Types.TIMESTAMP));
        addMetadata(deleteFlag, ColumnMetadata.named("delete_flag").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(7).ofType(Types.TIMESTAMP));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(8).ofType(Types.TIMESTAMP));
    }
}
