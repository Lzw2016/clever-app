package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;
import org.clever.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 16:35 <br/>
 */
public class SybaseDialect extends AbstractDialect {
    /**
     * sybase 12.5.4以前，不支持select top
     */
    private final boolean hasTop;

    public SybaseDialect() {
        this(false);
    }

    public SybaseDialect(boolean hasTop) {
        this.hasTop = hasTop;
    }

    @Override
    public String doBuildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap, String firstMark, String secondMark) {
        limit = (offset >= 1) ? (offset + limit) : limit;
        paramMap.put(secondMark, limit);
        originalSql = StringUtils.trimWhitespace(originalSql);
        int index = originalSql.toUpperCase().indexOf(" FROM ");
        StringBuilder sql = new StringBuilder("select");
        if (hasTop) {
            sql.append(" top ").append(COLON).append(secondMark);
        }
        sql.append(" rownum=identity(12),").append(originalSql, 6, index).append(" into #t ").append(originalSql.substring(index));
        sql.append(" select * from #t where rownum > ").append(COLON).append(firstMark).append(" and rownum <= ").append(COLON).append(secondMark);
        sql.append(" drop table #t ");
        return sql.toString();
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long limit) {
        limit = (offset >= 1) ? (offset + limit) : limit;
        originalSql = StringUtils.trimWhitespace(originalSql);
        int index = originalSql.toUpperCase().indexOf(" FROM ");
        StringBuilder sql = new StringBuilder("select");
        if (hasTop) {
            sql.append(" top ").append(limit);
        }
        sql.append(" rownum=identity(12),").append(originalSql, 6, index).append(" into #t ").append(originalSql.substring(index));
        sql.append(" select * from #t where rownum > ").append(offset).append(" and rownum <= ").append(limit);
        sql.append(" drop table #t ");
        return sql.toString();
    }

    @Override
    public TupleTwo<String, Map<String, Object>> currentDateTimeSql() {
        return TupleTwo.creat("select sysdatetime()", new HashMap<>());
    }
}
