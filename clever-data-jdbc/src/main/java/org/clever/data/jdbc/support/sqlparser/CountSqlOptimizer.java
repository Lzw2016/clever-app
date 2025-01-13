package org.clever.data.jdbc.support.sqlparser;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/06/12 11:08 <br/>
 */
public interface CountSqlOptimizer {
    /**
     * 默认的获取 count sql 选项
     */
    CountSqlOptions DEFAULT_OPTIONS = new CountSqlOptions();

    /**
     * 返回基于原生sql的简单包装的count sql语句
     */
    static String getRawCountSql(String rawSql) {
        return String.format("SELECT COUNT(1) AS TOTAL FROM (\n %s \n) TOTAL", rawSql);
    }

    /**
     * 返回优化后的count sql语句
     */
    default String getCountSql(String rawSql) {
        return getCountSql(rawSql, DEFAULT_OPTIONS);
    }

    /**
     * 返回优化后的count sql语句
     */
    String getCountSql(String rawSql, CountSqlOptions options);
}
