package org.clever.data.jdbc.support.sqlparser;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 23:15 <br/>
 */
@Slf4j
public class JSqlParserCountSqlOptimizerTest {
    @Test
    public void test01() {
        final String absolutePath = new File("./src/test/resources/performance_test").getAbsolutePath();
        FileSystemMyBatisMapperSql mybatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
        // mybatisMapperSql.reloadAll();
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        SqlSource sqlSource = mybatisMapperSql.getSqlSource("queryAllArea", "bas/area/Area.xml", DbType.MYSQL);
        String sql = sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql();
        log.info("sql=\n{}", sql);
        sql = countSqlOptimizer.getCountSql(sql);
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test02() {
        // 支持各种传参形式
        String sql = "select a, a1, a2, b, c " +
            "from ta left join tb on (ta.id=tb.id) " +
            "where a=1 and a1=? and b=:p_b and b=:p_b.value " +
            "      and c=power(1) and d=power(?) and e=power(:p_e) and e=power(:p_e.value) " +
            "order by a, a1 asc, a2 desc ";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, false));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test03() {
        // union select
        String sql = "select * from ta_1 where a=1 and a1=? and b=:p_b and b=:p_b.value " +
            "union all " +
            "select * from ta_2 where a=1 and a1=? and b=:p_b and b=:p_b.value ";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, false));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test04() {
        // select 子句中含有参数
        String sql = "select a, b, ?, :p_b, :p_b.value " +
            "from ta " +
            "where a=1 and a1=? and b=:p_b and b=:p_b.value " +
            "      and c=power(1) and d=power(?) and e=power(:p_e) and e=power(:p_e.value) " +
            "order by a, a1 asc, a2 desc ";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, false));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test05() {
        // order by 带有参数
        String sql = "select a, a1, a2, b, c " +
            "from ta left join tb on (ta.id=tb.id) " +
            "where a=1 and a1=? and b=:p_b and b=:p_b.value " +
            "      and c=power(1) and d=power(?) and e=power(:p_e) and e=power(:p_e.value) " +
            "order by ?, a1 :p_b, :p_b.value desc ";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, false));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test06() {
        // distinct
        String sql = "select distinct a, a1, a2, b, c " +
            "from ta left join tb on (ta.id=tb.id) " +
            "where a=1 and a1=? and b=:p_b and b=:p_b.value " +
            "      and c=power(1) and d=power(?) and e=power(:p_e) and e=power(:p_e.value) " +
            "order by a, a1 asc, a2 desc ";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, false));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test07() {
        // group by
        String sql = "select a, a1, a2 " +
            "from ta left join tb on (ta.id=tb.id) " +
            "where a=1 and a1=? and b=:p_b and b=:p_b.value " +
            "      and c=power(1) and d=power(?) and e=power(:p_e) and e=power(:p_e.value) " +
            "group by a, a1, a2 ";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, false));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test08() {
        // group by + distinct
        String sql = "select distinct a, a1, a2 " +
            "from ta left join tb on (ta.id=tb.id) " +
            "where a=1 and a1=? and b=:p_b and b=:p_b.value " +
            "      and c=power(1) and d=power(?) and e=power(:p_e) and e=power(:p_e.value) " +
            "group by a, a1, a2 ";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, false));
        log.info("sql=\n{}", sql);
    }
}
