package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;

import java.util.Map;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-03 13:04 <br/>
 */
public class SQLiteDialect extends AbstractDialect {
    @Override
    public String doBuildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap, String firstMark, String secondMark) {
        return originalSql + " limit " + (COLON + secondMark) + " offset " + (COLON + firstMark);
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long limit) {
        return originalSql + " limit " + limit + " offset " + offset;
    }

    @Override
    public TupleTwo<String, Map<String, Object>> nextPKSql(String primaryKeyName) {
        throw new UnsupportedOperationException("不支持当前操作");
    }
}
