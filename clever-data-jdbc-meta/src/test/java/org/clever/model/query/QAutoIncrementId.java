package org.clever.model.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.model.entity.AutoIncrementId;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 自增长id表(auto_increment_id)
 */
@SuppressWarnings("ALL")
public class QAutoIncrementId extends RelationalPathBase<AutoIncrementId> {
    /** auto_increment_id表 */
    public static final QAutoIncrementId autoIncrementId = new QAutoIncrementId("auto_increment_id");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 序列名称 */
    public final StringPath sequenceName = createString("sequenceName");
    /** 当前值 */
    public final NumberPath<Long> currentValue = createNumber("currentValue", Long.class);
    /** 说明 */
    public final StringPath description = createString("description");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QAutoIncrementId(String variable) {
        super(AutoIncrementId.class, forVariable(variable), "test", "auto_increment_id");
        addMetadata();
    }

    public QAutoIncrementId(String variable, String schema, String table) {
        super(AutoIncrementId.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QAutoIncrementId(String variable, String schema) {
        super(AutoIncrementId.class, forVariable(variable), schema, "auto_increment_id");
        addMetadata();
    }

    public QAutoIncrementId(Path<? extends AutoIncrementId> path) {
        super(path.getType(), path.getMetadata(), "test", "auto_increment_id");
        addMetadata();
    }

    public QAutoIncrementId(PathMetadata metadata) {
        super(AutoIncrementId.class, metadata, "test", "auto_increment_id");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(sequenceName, ColumnMetadata.named("sequence_name").withIndex(2).ofType(Types.VARCHAR).withSize(127));
        addMetadata(currentValue, ColumnMetadata.named("current_value").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(description, ColumnMetadata.named("description").withIndex(4).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(5).ofType(Types.TIMESTAMP));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(6).ofType(Types.TIMESTAMP));
    }
}
