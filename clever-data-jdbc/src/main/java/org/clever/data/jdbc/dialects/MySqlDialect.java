package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-03 12:16 <br/>
 */
public class MySqlDialect extends AbstractDialect {
    @Override
    public String doBuildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap, String firstMark, String secondMark) {
        return originalSql + "\n LIMIT " + (COLON + firstMark) + COMMA + (COLON + secondMark);
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long limit) {
        return originalSql + "\n LIMIT " + offset + COMMA + limit;
    }

    @Override
    public TupleTwo<String, Map<String, Object>> currentDateTimeSql() {
        return TupleTwo.creat("select now(3) from dual", new HashMap<>());
    }
}
