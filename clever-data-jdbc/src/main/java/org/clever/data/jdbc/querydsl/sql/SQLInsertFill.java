package org.clever.data.jdbc.querydsl.sql;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLInsertBatch;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/02/04 16:32 <br/>
 */
public interface SQLInsertFill {
    /**
     * 填充更新字段值
     */
    void fill(RelationalPath<?> entity, QueryMetadata metadata, List<Path<?>> columns, List<Expression<?>> values, List<SQLInsertBatch> batches, boolean isBatch);

    /**
     * 执行顺序
     *
     * @see org.clever.core.Ordered
     */
    default int getOrder() {
        return 0;
    }
}
