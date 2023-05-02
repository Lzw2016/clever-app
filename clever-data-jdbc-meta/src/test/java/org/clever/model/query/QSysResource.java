package org.clever.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.model.entity.SysResource;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 资源表(sys_resource)
 */
@SuppressWarnings("ALL")
public class QSysResource extends RelationalPathBase<SysResource> {
    /** sys_resource表 */
    public static final QSysResource sysResource = new QSysResource("sys_resource");

    /** 资源id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 权限编码 */
    public final StringPath permission = createString("permission");
    /** 资源类型: 1:API权限，2:菜单权限，3:UI权限(如:按钮、表单、表格) */
    public final NumberPath<Integer> resourceType = createNumber("resourceType", Integer.class);
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

    public QSysResource(String variable) {
        super(SysResource.class, forVariable(variable), "test", "sys_resource");
        addMetadata();
    }

    public QSysResource(String variable, String schema, String table) {
        super(SysResource.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QSysResource(String variable, String schema) {
        super(SysResource.class, forVariable(variable), schema, "sys_resource");
        addMetadata();
    }

    public QSysResource(Path<? extends SysResource> path) {
        super(path.getType(), path.getMetadata(), "test", "sys_resource");
        addMetadata();
    }

    public QSysResource(PathMetadata metadata) {
        super(SysResource.class, metadata, "test", "sys_resource");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(permission, ColumnMetadata.named("permission").withIndex(2).ofType(Types.VARCHAR).withSize(63));
        addMetadata(resourceType, ColumnMetadata.named("resource_type").withIndex(3).ofType(Types.INTEGER).withSize(10));
        addMetadata(isEnable, ColumnMetadata.named("is_enable").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(createBy, ColumnMetadata.named("create_by").withIndex(5).ofType(Types.BIGINT).withSize(19));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(6).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateBy, ColumnMetadata.named("update_by").withIndex(7).ofType(Types.BIGINT).withSize(19));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
    }
}
