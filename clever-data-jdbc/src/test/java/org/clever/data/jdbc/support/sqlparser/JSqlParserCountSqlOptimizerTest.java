package org.clever.data.jdbc.support.sqlparser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.SqlSourceGroup;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

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
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, true));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test09() {
        // TODO bug a._high -> a.high
        String sql = "select count(1) as contnum " +
            "from bas_package_items a " +
            "where a.item_id = ? " +
            "and ((a.length <= 0 or a.length is null) or (a.width <= 0 or a.width is null) or (a._high <= 0 or a._high is null) or (a.volume <= 0 or a.volume is null) or (a.package_meas <= 0 or a.package_meas is null))";
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        sql = countSqlOptimizer.getCountSql(sql, new CountSqlOptions(true, true));
        log.info("sql=\n{}", sql);
    }

    @Test
    public void test_performance_01() {
        final String absolutePath = new File("./src/test/resources/performance_test").getAbsolutePath();
        FileSystemMyBatisMapperSql mybatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
        mybatisMapperSql.reloadAll();
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        int count = 0;
        long cost = 0;
        ConcurrentMap<String, SqlSourceGroup> allSqlSourceGroupMap = ReflectionsUtils.getFieldValue(mybatisMapperSql, "allSqlSourceGroupMap");
        for (Map.Entry<String, SqlSourceGroup> entry : allSqlSourceGroupMap.entrySet()) {
            SqlSourceGroup sqlSourceGroup = entry.getValue();
            ConcurrentMap<String, SqlSource> stdSqlSource = ReflectionsUtils.getFieldValue(sqlSourceGroup, "stdSqlSource");
            for (SqlSource sqlSource : stdSqlSource.values()) {
                String sql = sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql().toLowerCase();
                if (StringUtils.isBlank(sql)
                    || sql.contains("insert into")
                    || sql.contains("update ")
                    || sql.contains("delete ")
                    || sql.contains(" in")
                    || sql.contains("where 1 = 1 and")
                    || sql.contains("where ci.wh_id = ? and")
                    || sql.contains("left join bas_package_items e on a.item_id = e.item_id and e.package_level = 4")
                    || sql.contains("where --@")
                    || sql.contains("'库存不足挂起',")
                    || sql.contains("where 1 = 1 and ")
                    || sql.contains("a.high <= 0 or a.high is null")
                    || sql.contains("package_level != 4 and")
                    || sql.contains("select zhiy_no 职员编号,yans_zz 验收组长")
                    || sql.contains("where a.order_out_id in")
                    || sql.contains("where t.english_name in")) {
                    continue;
                }
                final long startTime = System.currentTimeMillis();
                sql = countSqlOptimizer.getCountSql(sql);
                final long endTime = System.currentTimeMillis();
                cost += Math.max((endTime - startTime), 1);
                count++;
            }
        }
        // 耗时: 775ms | 数量: 252 | 速率: 3.0753968253968256ms/个
        log.info("耗时: {}ms | 数量: {} | 速率: {}ms/个", cost, count, cost * 1.0 / count);
    }
}
