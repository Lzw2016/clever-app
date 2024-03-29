package org.clever.security.impl.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.security.impl.model.entity.SysRole;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 角色表(sys_role)
 */
@SuppressWarnings("ALL")
public class QSysRole extends RelationalPathBase<SysRole> {
    /** 角色表(sys_role) */
    public static final QSysRole sysRole = new QSysRole("sys_role");

    /** 角色id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 角色编号 */
    public final StringPath roleCode = createString("roleCode");
    /** 角色名称 */
    public final StringPath roleName = createString("roleName");
    /** 是否启用: 0:禁用，1:启用 */
    public final NumberPath<Integer> isEnable = createNumber("isEnable", Integer.class);
    /** 创建人(用户id) */
    public final NumberPath<Long> createBy = createNumber("createBy", Long.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新人(用户id) */
    public final NumberPath<Long> updateBy = createNumber("updateBy", Long.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    QSysRole(String variable) {
        super(SysRole.class, forVariable(variable), "public", "sys_role");
        addMetadata();
    }

    QSysRole(String variable, String schema, String table) {
        super(SysRole.class, forVariable(variable), schema, table);
        addMetadata();
    }

    QSysRole(String variable, String schema) {
        super(SysRole.class, forVariable(variable), schema, "sys_role");
        addMetadata();
    }

    QSysRole(Path<? extends SysRole> path) {
        super(path.getType(), path.getMetadata(), "public", "sys_role");
        addMetadata();
    }

    QSysRole(PathMetadata metadata) {
        super(SysRole.class, metadata, "public", "sys_role");
        addMetadata();
    }

    void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT));
        addMetadata(roleCode, ColumnMetadata.named("role_code").withIndex(2).ofType(Types.VARCHAR));
        addMetadata(roleName, ColumnMetadata.named("role_name").withIndex(3).ofType(Types.VARCHAR));
        addMetadata(isEnable, ColumnMetadata.named("is_enable").withIndex(4).ofType(Types.INTEGER));
        addMetadata(createBy, ColumnMetadata.named("create_by").withIndex(5).ofType(Types.BIGINT));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(6).ofType(Types.TIMESTAMP));
        addMetadata(updateBy, ColumnMetadata.named("update_by").withIndex(7).ofType(Types.BIGINT));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(8).ofType(Types.TIMESTAMP));
    }
}
