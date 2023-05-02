package org.clever.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.model.entity.SysLock;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 自增长id表(sys_lock)
 */
@SuppressWarnings("ALL")
public class QSysLock extends RelationalPathBase<SysLock> {
    /** sys_lock表 */
    public static final QSysLock sysLock = new QSysLock("sys_lock");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 锁名称 */
    public final StringPath lockName = createString("lockName");
    /** 锁次数 */
    public final NumberPath<Long> lockCount = createNumber("lockCount", Long.class);
    /** 说明 */
    public final StringPath description = createString("description");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QSysLock(String variable) {
        super(SysLock.class, forVariable(variable), "test", "sys_lock");
        addMetadata();
    }

    public QSysLock(String variable, String schema, String table) {
        super(SysLock.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QSysLock(String variable, String schema) {
        super(SysLock.class, forVariable(variable), schema, "sys_lock");
        addMetadata();
    }

    public QSysLock(Path<? extends SysLock> path) {
        super(path.getType(), path.getMetadata(), "test", "sys_lock");
        addMetadata();
    }

    public QSysLock(PathMetadata metadata) {
        super(SysLock.class, metadata, "test", "sys_lock");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(lockName, ColumnMetadata.named("lock_name").withIndex(2).ofType(Types.VARCHAR).withSize(127));
        addMetadata(lockCount, ColumnMetadata.named("lock_count").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(description, ColumnMetadata.named("description").withIndex(4).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(5).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(6).ofType(Types.TIMESTAMP).withSize(3));
    }
}
