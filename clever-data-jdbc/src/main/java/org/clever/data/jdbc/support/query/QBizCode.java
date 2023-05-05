package org.clever.data.jdbc.support.query;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.clever.data.jdbc.support.entity.BizCode;

import java.sql.Types;
import java.util.Date;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * 业务编码表(biz_code)
 */
@SuppressWarnings("ALL")
public class QBizCode extends RelationalPathBase<BizCode> {
    /** biz_code表 */
    public static final QBizCode bizCode = new QBizCode("biz_code");

    /** 主键id */
    public final NumberPath<Long> id = createNumber("id", Long.class);
    /** 编码名称 */
    public final StringPath codeName = createString("codeName");
    /** 编码规则表达式 */
    public final StringPath pattern = createString("pattern");
    /** 序列值 */
    public final NumberPath<Long> sequence = createNumber("sequence", Long.class);
    /** 重置sequence值的表达式，使用Java日期格式化字符串 */
    public final StringPath resetPattern = createString("resetPattern");
    /** 重置sequence值标识，此字段值变化后则需要重置 */
    public final StringPath resetFlag = createString("resetFlag");
    /** 说明 */
    public final StringPath description = createString("description");
    /** 创建时间 */
    public final DateTimePath<Date> createAt = createDateTime("createAt", Date.class);
    /** 更新时间 */
    public final DateTimePath<Date> updateAt = createDateTime("updateAt", Date.class);

    public QBizCode(String variable) {
        super(BizCode.class, forVariable(variable), "", "biz_code");
        addMetadata();
    }

    public QBizCode(String variable, String schema, String table) {
        super(BizCode.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QBizCode(String variable, String schema) {
        super(BizCode.class, forVariable(variable), schema, "biz_code");
        addMetadata();
    }

    public QBizCode(Path<? extends BizCode> path) {
        super(path.getType(), path.getMetadata(), "", "biz_code");
        addMetadata();
    }

    public QBizCode(PathMetadata metadata) {
        super(BizCode.class, metadata, "", "biz_code");
        addMetadata();
    }

    private void addMetadata() {
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19));
        addMetadata(codeName, ColumnMetadata.named("code_name").withIndex(2).ofType(Types.VARCHAR).withSize(127));
        addMetadata(pattern, ColumnMetadata.named("pattern").withIndex(3).ofType(Types.VARCHAR).withSize(127));
        addMetadata(sequence, ColumnMetadata.named("sequence").withIndex(4).ofType(Types.BIGINT).withSize(19));
        addMetadata(resetPattern, ColumnMetadata.named("reset_pattern").withIndex(5).ofType(Types.VARCHAR).withSize(127));
        addMetadata(resetFlag, ColumnMetadata.named("reset_flag").withIndex(6).ofType(Types.VARCHAR).withSize(127));
        addMetadata(description, ColumnMetadata.named("description").withIndex(7).ofType(Types.VARCHAR).withSize(511));
        addMetadata(createAt, ColumnMetadata.named("create_at").withIndex(8).ofType(Types.TIMESTAMP).withSize(3));
        addMetadata(updateAt, ColumnMetadata.named("update_at").withIndex(9).ofType(Types.TIMESTAMP).withSize(3));
    }
}
