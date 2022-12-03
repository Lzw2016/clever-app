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
     * 返回能得到下一个主键值的SQL语句以及参数
     *
     * @param primaryKeyName 主键名称
     */
    TupleTwo<String, Map<String, Object>> nextPKSql(String primaryKeyName);
}