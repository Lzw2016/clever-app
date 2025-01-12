package org.clever.data.dynamic.sql.utils;

import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.dynamic.sql.dialect.SqlFuncDialectTransform;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/24 20:09 <br/>
 */
public class ParseSqlFuncUtils {
    static final Pattern pattern = Pattern.compile("'@\\{.+?}'");

    public static boolean needParse(String originalSql) {
        return pattern.matcher(originalSql).find();
    }

    public static String parseSqlFunc(
        final DbType dbType,
        final String originalSql,
        final Object parameterObject,
        final LinkedHashMap<String, Object> sqlVariable) {
        Matcher matcher = pattern.matcher(originalSql);
        StringBuilder sqlDialect = new StringBuilder();
        int start = 0;
        while (matcher.find()) {
            sqlDialect.append(originalSql, start, matcher.start());
            String sqlFuc = matcher.group();
            sqlFuc = sqlFuc.substring(3, sqlFuc.length() - 2);
            start = matcher.end();
            SqlFuncDialectTransform transform = new SqlFuncDialectTransform(sqlFuc, parameterObject);
            sqlDialect.append(transform.toSql(dbType));
            LinkedHashMap<String, Object> tmpSqlVariable = transform.getSqlVariable(dbType);
            tmpSqlVariable.forEach((name, value) -> {
                name = SqlParameterNameStrategy.rename(name);
                // name = SqlParameterNameStrategy.uniqueName(name);
                sqlVariable.put(name, value);
            });
        }
        sqlDialect.append(originalSql, start, originalSql.length());
        return sqlDialect.toString();
    }
}
