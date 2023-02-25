package org.clever.security.impl.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.security.impl.model.entity.SysRoleResource;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 角色资源关联表(sys_role_resource)
 */
@SuppressWarnings("ALL")
public class QSysRoleResource extends RelationalPathBase<SysRoleResource> {
    /** 角色资源关联表(sys_role_resource) */
    public static final QSysRoleResource sysRoleResource = new QSysRoleResource("sys_role_resource");

    /** 角色id */
    public final NumberPath<Long> roleId = createNumber("roleId", Long.class);
    /** 资源ID */
    public final NumberPath<Long> resourceId = createNumber("resourceId", Long.class);
    /** 创建人(用户id) */
    public final NumberPath<Long> createBy = createNumber("createBy", Long.class);
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新人(用户id) */
    public final NumberPath<Long> updateBy = createNumber("updateBy", Long.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    QSysRoleResource(String variable) {
        super(SysRoleResource.class, forVariable(variable), "public", "sys_role_resource");
        addMetadata();
    }

    QSysRoleResource(String variable, String schema, String table) {
        super(SysRoleResource.class, forVariable(variable), schema, table);
        addMetadata();
    }

    QSysRoleResource(String variable, String schema) {
        super(SysRoleResource.class, forVariable(variable), schema, "sys_role_resource");
        addMetadata();
    }

    QSysRoleResource(Path<? extends SysRoleResource> path) {
        super(path.getType(), path.getMetadata(), "public", "sys_role_resource");
        addMetadata();
    }

    QSysRoleResource(PathMetadata metadata) {
        super(SysRoleResource.class, metadata, "public", "sys_role_resource");
        addMetadata();
    }

    void addMetadata() {
        addMetadata(roleId, ColumnMetadata.named("role_id").withIndex(1).ofType(Types.BIGINT));
        addMetadata(resourceId, ColumnMetadata.named("resource_id").withIndex(2).ofType(Types.BIGINT));
        addMetadata(createBy, ColumnMetadata.named("create_by").withIndex(3).ofType(Types.BIGINT));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(4).ofType(Types.TIMESTAMP));
        addMetadata(updateBy, ColumnMetadata.named("update_by").withIndex(5).ofType(Types.BIGINT));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(6).ofType(Types.TIMESTAMP));
    }
}
