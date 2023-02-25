package org.clever.security.impl.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.security.impl.model.entity.SysUser;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 用户表(sys_user)
 */
@SuppressWarnings("ALL")
public class QSysUser extends RelationalPathBase<SysUser> {
    /** 用户表 */
    public static final QSysUser sysUser = new QSysUser("sys_user");

    /** 用户id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 用户登录名(允许修改) */
    public final StringPath loginName = createString("loginName");
    /** 登录密码 */
    public final StringPath password = createString("password");
    /** 登录名 */
    public final StringPath userName = createString("userName");
    /** 是否启用: 0:禁用，1:启用 */
    public final NumberPath<Integer> isEnable = createNumber("isEnable", Integer.class);
    /** 创建人 */
    public final StringPath createBy = createString("createBy");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新人 */
    public final StringPath updateBy = createString("updateBy");
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    QSysUser(String variable) {
        super(SysUser.class, forVariable(variable), "public", "sys_user");
        addMetadata();
    }

    QSysUser(String variable, String schema, String table) {
        super(SysUser.class, forVariable(variable), schema, table);
        addMetadata();
    }

    QSysUser(String variable, String schema) {
        super(SysUser.class, forVariable(variable), schema, "sys_user");
        addMetadata();
    }

    QSysUser(Path<? extends SysUser> path) {
        super(path.getType(), path.getMetadata(), "public", "sys_user");
        addMetadata();
    }

    QSysUser(PathMetadata metadata) {
        super(SysUser.class, metadata, "public", "sys_user");
        addMetadata();
    }

    void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT));
        addMetadata(loginName, ColumnMetadata.named("login_name").withIndex(2).ofType(Types.VARCHAR));
        addMetadata(password, ColumnMetadata.named("password").withIndex(3).ofType(Types.VARCHAR));
        addMetadata(userName, ColumnMetadata.named("user_name").withIndex(4).ofType(Types.VARCHAR));
        addMetadata(isEnable, ColumnMetadata.named("is_enable").withIndex(5).ofType(Types.INTEGER));
        addMetadata(createBy, ColumnMetadata.named("create_by").withIndex(6).ofType(Types.BIGINT));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(7).ofType(Types.TIMESTAMP));
        addMetadata(updateBy, ColumnMetadata.named("update_by").withIndex(8).ofType(Types.BIGINT));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(9).ofType(Types.TIMESTAMP));
    }
}
