package org.clever.data.jdbc.dialects;

import org.clever.core.tuples.TupleTwo;

import java.util.Map;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-03 12:14 <br/>
 */
public interface IDialect {
    String COLON = ":";
    String COMMA = ",";
    String FIRST_MARK = "first_mark";
    String SECOND_MARK = "second_mark";

    /**
     * 组装分页语句(使用分页参数)
     *
     * @param originalSql 原始 SQL
     * @param offset      数据偏移量
     * @param limit       数据量
     * @param paramMap    Sql参数
     */
    String buildPaginationSql(String originalSql, long offset, long limit, Map<String, Object> paramMap);

    /**
     * 组装分页语句(不使用分页参数)
     *
     * @param originalSql 原始 SQL
     * @param offset      数据偏移量
     * @param limit       数据量
     */
    String buildPaginationSql(String originalSql, long offset, long limit);

    /**
     * 获取查询数据库当前时间的sql和参数
     */
    default TupleTwo<String, Map<String, Object>> currentDateTimeSql() {
        throw new UnsupportedOperationException("不支持当前操作");
    }

    /**
     * 获取查询当前序列值的sql和参数
     *
     * @param seqName 序列名称
     */
    default TupleTwo<String, Map<String, Object>> nextSeqSql(String seqName) {
        throw new UnsupportedOperationException("不支持当前操作");
    }
}
