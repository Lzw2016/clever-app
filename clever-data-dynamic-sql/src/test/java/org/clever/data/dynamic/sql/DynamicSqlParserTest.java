package org.clever.data.dynamic.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.dynamic.sql.domain.Author;
import org.clever.data.dynamic.sql.domain.Section;
import org.clever.data.dynamic.sql.ognl.OgnlCache;
import org.clever.data.dynamic.sql.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/09/01 17:14 <br/>
 */
@Slf4j
public class DynamicSqlParserTest {
    private static String[] sqlArray;

    @BeforeAll
    public static void init() throws Exception {
        File xml = new File("src/test/resources/sql.txt");
        String sqlText = FileUtils.readFileToString(xml, "utf-8");
        sqlArray = StringUtils.splitByWholeSeparator(sqlText, "# -------------------------------------------------------------------------------------");
    }

    // 简单脚本
    @Test
    public void t01() {
        String sql = sqlArray[0];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Map<String, Object> params = new HashMap<>();
        params.put("id", 2);
        params.put("name", "%queryAll%");
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    // 复杂脚本
    @Test
    public void t02() {
        String sql = sqlArray[1];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Map<String, Object> params = new HashMap<>();
        params.put("id", "value111111111111111111");
        params.put("name", "lzw");
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        params.put("list", list);
        List<String> names = new ArrayList<>();
        names.add("name1");
        names.add("name2");
        names.add("name3");
        names.add("name4");
        names.add("name5");
        names.add("name6");
        params.put("names", names);
        params.put("orderBy", "a.aaa DESC, a.bbb ASC");
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
        log.info("#---------------------------------------------------------------------------------------------------------");
        boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
        log.info("#---------------------------------------------------------------------------------------------------------");
        params.clear();
        boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    // 参数是对象
    @Test
    public void t03() {
        String sql = sqlArray[2];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Author author = new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS);
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, author);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    // 混合参数
    @Test
    public void t04() {
        String sql = sqlArray[3];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Author author = new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS);
        Map<String, Object> params = new HashMap<>();
        params.put("author", author);
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    // 性能测试
    @Test
    public void t05() {
        String sql = sqlArray[4];
        Map<String, Object> params = new HashMap<>();
        params.put("id", "value111111111111111111");
        params.put("name", "lzw");
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        params.put("list", list);
        List<String> names = new ArrayList<>();
        names.add("name1");
        names.add("name2");
        names.add("name3");
        names.add("name4");
        names.add("name5");
        names.add("name6");
        params.put("names", names);
        params.put("orderBy", "a.aaa DESC, a.bbb ASC");
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        final int count = 100000;
        final long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
            boundSql.getParameterValueList();
        }
        final long end = System.currentTimeMillis();
        log.info("--> 耗时 {}s, 速度： {}次/ms", (end - start) / 1000.0, count * 1.0 / (end - start));
    }

    @Test
    public void t06() {
        String sql = sqlArray[5];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Map<String, Object> params = new HashMap<>();
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    @Test
    public void t07() {
        String sql = sqlArray[6];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Map<String, Object> params = new HashMap<>();
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    // mybatis 兼容性全面测试
    @Test
    public void t08() {
        String sql = sqlArray[7];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Map<String, Object> params = new HashMap<>();
        params.put("f1", "f1-val");
        params.put("f2", "f2-val");
        params.put("f3", "f3-val");
        params.put("f4List", Arrays.asList("f4-111", "f4-222", "f4-333"));
        params.put("f5", "f5-val");
        params.put("f6", "f6-val");
        params.put("f7", "f7-val");
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    // mybatis 兼容性全面测试
    @Test
    public void t09() {
        String sql = sqlArray[8];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Map<String, Object> params = new HashMap<>();
        params.put("f1", "f1-val");
        params.put("f2", "f2-val");
        params.put("f3", "f3-val");
        params.put("f4", "f4-val");
        params.put("id", "id-val");
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    // mybatis 扩展函数
    @Test
    public void t10() {
        String sql = sqlArray[9];
        SqlSource sqlSource = DynamicSqlParser.parserSql(sql);
        Map<String, Object> params = new HashMap<>();
        params.put("f1", "123");
        params.put("f2", "   ");
        params.put("f3", "bbb");
        params.put("f4", new int[]{1, 2, 3});
        params.put("f5", new ArrayList<String>() {{
            add("f5-aaa");
            add("f5-bbb");
            add("f5-ccc");
        }});
        params.put("f6", new BigDecimal("999"));
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
        OgnlCache.Default_Context.put("demo", "自定义");
    }
}
