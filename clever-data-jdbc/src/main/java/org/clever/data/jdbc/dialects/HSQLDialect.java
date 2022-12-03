package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;

import java.util.Map;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-03 13:03 <br/>
 */
public class HSQLDialect extends AbstractDialect {
    @Override
    public String doBuildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap, String firstMark, String secondMark) {
        return originalSql + " limit " + (COLON + firstMark) + COMMA + (COLON + secondMark);
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long limit) {
        return originalSql + " limit " + offset + COMMA + limit;
    }

    @Override
    public TupleTwo<String, Map<String, Object>> nextPKSql(String primaryKeyName) {
        throw new UnsupportedOperationException("不支持当前操作");
    }
}
