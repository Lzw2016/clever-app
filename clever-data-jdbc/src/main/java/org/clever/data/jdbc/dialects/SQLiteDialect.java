package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-03 13:04 <br/>
 */
public class SQLiteDialect extends AbstractDialect {
    @Override
    public String doBuildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap, String firstMark, String secondMark) {
        return originalSql + "\n limit " + (COLON + secondMark) + " offset " + (COLON + firstMark);
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long limit) {
        return originalSql + "\n limit " + limit + " offset " + offset;
    }

    @Override
    public TupleTwo<String, Map<String, Object>> currentDateTimeSql() {
        return TupleTwo.creat("select strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')", new HashMap<>());
    }
}
