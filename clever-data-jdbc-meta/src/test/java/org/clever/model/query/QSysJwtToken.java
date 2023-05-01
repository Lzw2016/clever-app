package org.clever.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.model.entity.SysJwtToken;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 登录JWT Token(缓存表)(sys_jwt_token)
 */
@SuppressWarnings("ALL")
public class QSysJwtToken extends RelationalPathBase<SysJwtToken> {
    /** sys_jwt_token表 */
    public static final QSysJwtToken sysJwtToken = new QSysJwtToken("sys_jwt_token");

    /** token id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 用户id */
    public final NumberPath<Long> userId = createNumber("userId", Long.class);
    /** token数据 */
    public final StringPath token = createString("token");
    /** token过期时间(空表示永不过期) */
    public final DateTimePath<Date> expiredTime = createDateTime("expiredTime", Date.class);
    /** token是否禁用: 0:未禁用；1:已禁用 */
    public final NumberPath<Integer> disable = createNumber("disable", Integer.class);
    /** token禁用原因: 0:使用RefreshToken；1:管理员手动禁用；2:并发登录被挤下线；3:用户主动登出 */
    public final NumberPath<Integer> disableReason = createNumber("disableReason", Integer.class);
    /** token数据 */
    public final StringPath refreshToken = createString("refreshToken");
    /** 刷新token过期时间 */
    public final DateTimePath<Date> rtExpiredTime = createDateTime("rtExpiredTime", Date.class);
    /** 刷新token状态: 0:无效(已使用)；1:有效(未使用) */
    public final NumberPath<Integer> rtState = createNumber("rtState", Integer.class);
    /** 刷新token使用时间 */
    public final DateTimePath<Date> rtUseTime = createDateTime("rtUseTime", Date.class);
    /** 刷新token创建的token id */
    public final NumberPath<Long> rtCreateTokenId = createNumber("rtCreateTokenId", Long.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QSysJwtToken(String variable) {
        super(SysJwtToken.class, forVariable(variable), "test", "sys_jwt_token");
        addMetadata();
    }

    public QSysJwtToken(String variable, String schema, String table) {
        super(SysJwtToken.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QSysJwtToken(String variable, String schema) {
        super(SysJwtToken.class, forVariable(variable), schema, "sys_jwt_token");
        addMetadata();
    }

    public QSysJwtToken(Path<? extends SysJwtToken> path) {
        super(path.getType(), path.getMetadata(), "test", "sys_jwt_token");
        addMetadata();
    }

    public QSysJwtToken(PathMetadata metadata) {
        super(SysJwtToken.class, metadata, "test", "sys_jwt_token");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(userId, ColumnMetadata.named("user_id").withIndex(2).ofType(Types.BIGINT).withSize(19));
        addMetadata(token, ColumnMetadata.named("token").withIndex(3).ofType(Types.VARCHAR).withSize(4095));
        addMetadata(expiredTime, ColumnMetadata.named("expired_time").withIndex(4).ofType(Types.TIMESTAMP));
        addMetadata(disable, ColumnMetadata.named("disable").withIndex(5).ofType(Types.INTEGER).withSize(10));
        addMetadata(disableReason, ColumnMetadata.named("disable_reason").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(refreshToken, ColumnMetadata.named("refresh_token").withIndex(7).ofType(Types.VARCHAR).withSize(127));
        addMetadata(rtExpiredTime, ColumnMetadata.named("rt_expired_time").withIndex(8).ofType(Types.TIMESTAMP));
        addMetadata(rtState, ColumnMetadata.named("rt_state").withIndex(9).ofType(Types.INTEGER).withSize(10));
        addMetadata(rtUseTime, ColumnMetadata.named("rt_use_time").withIndex(10).ofType(Types.TIMESTAMP));
        addMetadata(rtCreateTokenId, ColumnMetadata.named("rt_create_token_id").withIndex(11).ofType(Types.BIGINT).withSize(19));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(12).ofType(Types.TIMESTAMP));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(13).ofType(Types.TIMESTAMP));
    }
}
