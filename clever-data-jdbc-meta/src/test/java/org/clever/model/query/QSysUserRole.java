package org.clever.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.model.entity.SysUserRole;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 用户角色关联表(sys_user_role)
 */
@SuppressWarnings("ALL")
public class QSysUserRole extends RelationalPathBase<SysUserRole> {
    /** sys_user_role表 */
    public static final QSysUserRole sysUserRole = new QSysUserRole("sys_user_role");

    /** 用户id */
    public final NumberPath<Long> userId = createNumber("userId", Long.class);
    /** 角色id */
    public final NumberPath<Long> roleId = createNumber("roleId", Long.class);
    /** 创建人(用户id) */
    public final NumberPath<Long> createBy = createNumber("createBy", Long.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新人(用户id) */
    public final NumberPath<Long> updateBy = createNumber("updateBy", Long.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QSysUserRole(String variable) {
        super(SysUserRole.class, forVariable(variable), "test", "sys_user_role");
        addMetadata();
    }

    public QSysUserRole(String variable, String schema, String table) {
        super(SysUserRole.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QSysUserRole(String variable, String schema) {
        super(SysUserRole.class, forVariable(variable), schema, "sys_user_role");
        addMetadata();
    }

    public QSysUserRole(Path<? extends SysUserRole> path) {
        super(path.getType(), path.getMetadata(), "test", "sys_user_role");
        addMetadata();
    }

    public QSysUserRole(PathMetadata metadata) {
        super(SysUserRole.class, metadata, "test", "sys_user_role");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(userId, ColumnMetadata.named("user_id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(roleId, ColumnMetadata.named("role_id").withIndex(2).ofType(Types.BIGINT).withSize(19));
        addMetadata(createBy, ColumnMetadata.named("create_by").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(4).ofType(Types.TIMESTAMP));
        addMetadata(updateBy, ColumnMetadata.named("update_by").withIndex(5).ofType(Types.BIGINT).withSize(19));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(6).ofType(Types.TIMESTAMP));
    }
}
