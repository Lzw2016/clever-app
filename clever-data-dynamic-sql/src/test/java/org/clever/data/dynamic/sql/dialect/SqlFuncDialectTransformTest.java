package org.clever.data.dynamic.sql.dialect;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.dynamic.sql.dialect.func.JoinFuncTransform;
import org.clever.data.dynamic.sql.dialect.func.ToDateFuncTransform;
import org.clever.data.dynamic.sql.dialect.utils.SqlFuncTransformUtils;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/10 17:13 <br/>
 */
@Slf4j
public class SqlFuncDialectTransformTest {

    public SqlFuncDialectTransformTest() {
        SqlFuncTransformUtils.register(new ToDateFuncTransform());
        SqlFuncTransformUtils.register(new JoinFuncTransform());
    }

    public SqlFuncDialectTransform parser(String code, Map<String, Object> ognlRoot) {
        final long startTime = System.currentTimeMillis();
        SqlFuncDialectTransform transform = new SqlFuncDialectTransform(code, ognlRoot);
        final long endTime = System.currentTimeMillis();
        log.info("耗时：{}ms", endTime - startTime);
        return transform;
    }

    @Test
    public void t01() {
        // sql_func_1(jObject.fieldA.java_func(), jObject.fieldB, "YYYY-DD-MM", jVar2, jVar3)
        final String code = "sql_func_1(jObject.fieldA.java_func(), jObject.fieldB, \"YYYY-DD-MM\", jVar2, jVar3)";
        SqlFuncDialectTransform transform = parser(code, new HashMap<>());
        log.info("--> {} | {}", transform.toSql(DbType.MYSQL), transform.getSqlVariable(DbType.MYSQL));
    }

    @Test
    public void t02() {
        // sql_func_1(jObject.fieldA.java_func(jObject.fieldC, "abc"), jObject.fieldB, "YYYY-DD-MM", jVar2, jVar3)
        final String code = "sql_func_1(jObject.fieldA.java_func(jObject.fieldC, \"abc\"), jObject.fieldB, \"YYYY-DD-MM\", jVar2, jVar3)";
        SqlFuncDialectTransform transform = parser(code, new HashMap<>());
        log.info("--> {} | {}", transform.toSql(DbType.MYSQL), transform.getSqlVariable(DbType.MYSQL));
    }

    @Test
    public void t03() {
        // sql_func_1(jObject.java_func_1(jVar2, "ABC"), jVar3, sql_func_2(jVar3, jObject.java_func_2(jVar2, "ABC")))
        final String code = "sql_func_1(jObject.java_func_1(jVar2, \"ABC\"), jVar3, sql_func_2(jVar3, jObject.java_func_2(jVar2, \"ABC\")))";
        SqlFuncDialectTransform transform = parser(code, new HashMap<>());
        log.info("--> {} | {}", transform.toSql(DbType.MYSQL), transform.getSqlVariable(DbType.MYSQL));
    }

    @Test
    public void t04() {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("today", new Date());

        // to_date(today)
        final String code = "to_date(today)";

        SqlFuncDialectTransform transform = parser(code, bindings);
        log.info("MYSQL        --> {} | {}", transform.toSql(DbType.MYSQL), transform.getSqlVariable(DbType.MYSQL));

        transform = parser(code, bindings);
        log.info("ORACLE       --> {} | {}", transform.toSql(DbType.ORACLE), transform.getSqlVariable(DbType.ORACLE));

        transform = parser(code, bindings);
        log.info("SQL_SERVER   --> {} | {}", transform.toSql(DbType.SQL_SERVER), transform.getSqlVariable(DbType.SQL_SERVER));
    }

    @Test
    public void t05() {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("java_arr", Arrays.asList(1, 2, 3, 4, 5));
        final String code = "join(java_arr)";
        SqlFuncDialectTransform transform = parser(code, bindings);
        log.info("--> {} | {}", transform.toSql(DbType.MYSQL), transform.getSqlVariable(DbType.MYSQL));
    }

    @Test
    public void t99() {
        final LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("today1", new Date());
        bindings.put("today2", new Date());
        String sql = "select * from entity_a where a='@{to_date(today1)}' and b='@{to_date(today2)}' and c=#{abc} limit 10";
        Pattern pattern = Pattern.compile("'@\\{.+?}'");
        Matcher matcher = pattern.matcher(sql);
        StringBuilder newSql = new StringBuilder();
        int start = 0;
        while (matcher.find()) {
            newSql.append(sql, start, matcher.start());
            String code = matcher.group();
            code = code.substring(3, code.length() - 2);
            start = matcher.end();
            SqlFuncDialectTransform transform = parser(code, bindings);
            newSql.append(transform.toSql(DbType.SQL_SERVER));
            params.putAll(transform.getSqlVariable(DbType.SQL_SERVER));
        }
        newSql.append(sql, start, sql.length());
        log.info("newSql   --> {} | {}", newSql, params);
    }
}
