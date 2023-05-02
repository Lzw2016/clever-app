package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 16:08 <br/>
 */
public class Oracle12cDialect extends AbstractDialect {
    @Override
    public String doBuildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap, String firstMark, String secondMark) {
        return originalSql + " OFFSET " + (COLON + firstMark) + " ROWS FETCH NEXT " + (COLON + secondMark) + " ROWS ONLY";
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long limit) {
        return originalSql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public TupleTwo<String, Map<String, Object>> currentDateTimeSql() {
        return TupleTwo.creat("select current_timestamp from dual", new HashMap<>());
    }

    @Override
    public TupleTwo<String, Map<String, Object>> nextSeqSql(String seqName) {
        return TupleTwo.creat("SELECT :seqName FROM DUAL", new HashMap<String, Object>() {{
            put("seqName", String.format("%s.NEXTVAL", seqName));
        }});
    }
}
