package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-03 12:49 <br/>
 */
public class OracleDialect extends AbstractDialect {
    @Override
    public String doBuildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap, String firstMark, String secondMark) {
        limit = (offset >= 1) ? (offset + limit) : limit;
        paramMap.put(secondMark, limit);
        return "SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( \n" + originalSql + "\n ) TMP WHERE ROWNUM <=" + (COLON + secondMark) + ") WHERE ROW_ID > " + (COLON + firstMark);
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long limit) {
        limit = (offset >= 1) ? (offset + limit) : limit;
        return "SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( \n" + originalSql + "\n ) TMP WHERE ROWNUM <=" + limit + ") WHERE ROW_ID > " + offset;
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
