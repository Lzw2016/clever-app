package org.clever.security.impl.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.security.impl.model.entity.SysSecurityContext;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 用户security context(缓存表)(sys_security_context)
 */
@SuppressWarnings("ALL")
public class QSysSecurityContext extends RelationalPathBase<SysSecurityContext> {
    /** 用户security context(缓存表)(sys_security_context) */
    public static final QSysSecurityContext sysSecurityContext = new QSysSecurityContext("sys_security_context");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 用户id */
    public final NumberPath<Long> userId = createNumber("userId", Long.class);
    /** 用户security context */
    public final StringPath securityContext = createString("securityContext");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    QSysSecurityContext(String variable) {
        super(SysSecurityContext.class, forVariable(variable), "public", "sys_security_context");
        addMetadata();
    }

    QSysSecurityContext(String variable, String schema, String table) {
        super(SysSecurityContext.class, forVariable(variable), schema, table);
        addMetadata();
    }

    QSysSecurityContext(String variable, String schema) {
        super(SysSecurityContext.class, forVariable(variable), schema, "sys_security_context");
        addMetadata();
    }

    QSysSecurityContext(Path<? extends SysSecurityContext> path) {
        super(path.getType(), path.getMetadata(), "public", "sys_security_context");
        addMetadata();
    }

    QSysSecurityContext(PathMetadata metadata) {
        super(SysSecurityContext.class, metadata, "public", "sys_security_context");
        addMetadata();
    }

    void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT));
        addMetadata(userId, ColumnMetadata.named("user_id").withIndex(2).ofType(Types.BIGINT));
        addMetadata(securityContext, ColumnMetadata.named("security_context").withIndex(3).ofType(Types.VARCHAR));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(4).ofType(Types.TIMESTAMP));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(5).ofType(Types.TIMESTAMP));
    }
}
