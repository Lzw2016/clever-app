package org.clever.data.jdbc.querydsl.sql;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLUpdateBatch;

import java.util.List;
import java.util.Map;

/**
 * 在querydsl执行update之前的字段填充逻辑<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/02/04 16:16 <br/>
 */
public interface SQLUpdateFill {
    /**
     * 填充更新字段值
     */
    void fill(RelationalPath<?> entity, QueryMetadata metadata, Map<Path<?>, Expression<?>> updates, List<SQLUpdateBatch> batches, boolean isBatch);

    /**
     * 执行顺序
     *
     * @see org.clever.core.Ordered
     */
    default int getOrder() {
        return 0;
    }
}
