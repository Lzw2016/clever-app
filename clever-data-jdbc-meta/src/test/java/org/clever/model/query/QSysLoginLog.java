package org.clever.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.model.entity.SysLoginLog;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 登录日志(sys_login_log)
 */
@SuppressWarnings("ALL")
public class QSysLoginLog extends RelationalPathBase<SysLoginLog> {
    /** sys_login_log表 */
    public static final QSysLoginLog sysLoginLog = new QSysLoginLog("sys_login_log");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 用户id */
    public final NumberPath<Long> userId = createNumber("userId", Long.class);
    /** 登录时间 */
    public final DateTimePath<Date> loginTime = createDateTime("loginTime", Date.class);
    /** 登录ip */
    public final StringPath loginIp = createString("loginIp");
    /** 登录方式 */
    public final NumberPath<Integer> loginType = createNumber("loginType", Integer.class);
    /** 登录渠道 */
    public final NumberPath<Integer> loginChannel = createNumber("loginChannel", Integer.class);
    /** 登录状态: 0:登录失败，1:登录成功 */
    public final NumberPath<Integer> loginState = createNumber("loginState", Integer.class);
    /** 登录请求数据 */
    public final StringPath requestData = createString("requestData");
    /** token id */
    public final NumberPath<Long> jwtTokenId = createNumber("jwtTokenId", Long.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);

    public QSysLoginLog(String variable) {
        super(SysLoginLog.class, forVariable(variable), "test", "sys_login_log");
        addMetadata();
    }

    public QSysLoginLog(String variable, String schema, String table) {
        super(SysLoginLog.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QSysLoginLog(String variable, String schema) {
        super(SysLoginLog.class, forVariable(variable), schema, "sys_login_log");
        addMetadata();
    }

    public QSysLoginLog(Path<? extends SysLoginLog> path) {
        super(path.getType(), path.getMetadata(), "test", "sys_login_log");
        addMetadata();
    }

    public QSysLoginLog(PathMetadata metadata) {
        super(SysLoginLog.class, metadata, "test", "sys_login_log");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(userId, ColumnMetadata.named("user_id").withIndex(2).ofType(Types.BIGINT).withSize(19));
        addMetadata(loginTime, ColumnMetadata.named("login_time").withIndex(3).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(loginIp, ColumnMetadata.named("login_ip").withIndex(4).ofType(Types.VARCHAR).withSize(31));
        addMetadata(loginType, ColumnMetadata.named("login_type").withIndex(5).ofType(Types.INTEGER).withSize(10));
        addMetadata(loginChannel, ColumnMetadata.named("login_channel").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(loginState, ColumnMetadata.named("login_state").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(requestData, ColumnMetadata.named("request_data").withIndex(8).ofType(Types.VARCHAR).withSize(4095));
        addMetadata(jwtTokenId, ColumnMetadata.named("jwt_token_id").withIndex(9).ofType(Types.BIGINT).withSize(19));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(10).ofType(Types.TIMESTAMP).withSize(3));
    }
}
