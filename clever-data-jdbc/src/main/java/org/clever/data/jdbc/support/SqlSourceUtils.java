package org.clever.data.jdbc.support;

import org.apache.commons.lang3.StringUtils;
import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.DynamicSqlParser;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/07/30 22:58 <br/>
 */
public class SqlSourceUtils {
    public static final SqlSourceUtils Instance = new SqlSourceUtils();

    private SqlSourceUtils() {
    }

    /**
     * 获取 SqlSource
     *
     * @param xmlSql MyBatis XML sql
     */
    public SqlSource getSqlSource(String xmlSql) {
        if (StringUtils.isBlank(xmlSql)) {
            return null;
        }
        xmlSql = StringUtils.trim(xmlSql);
        if (!xmlSql.startsWith("<script>")) {
            xmlSql = String.format("<script>\n%s\n</script>", xmlSql);
        }
        return DynamicSqlParser.parserSql(xmlSql);
    }

    /**
     * 获取 BoundSql
     *
     * @param xmlSql    MyBatis XML sql
     * @param parameter SQL参数
     */
    public BoundSql getBoundSql(DbType dbType, String xmlSql, Object parameter) {
        return getSqlSource(xmlSql).getBoundSql(dbType, parameter);
    }
}
