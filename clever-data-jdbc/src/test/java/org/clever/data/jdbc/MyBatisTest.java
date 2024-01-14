package org.clever.data.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.id.SnowFlake;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.jdbc.entity.EntityData;
import org.clever.data.jdbc.mapper.LocationClass;
import org.clever.data.jdbc.mapper.Mapper01;
import org.clever.data.jdbc.mybatis.ClassPathMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/06 17:02 <br/>
 */
@Slf4j
public class MyBatisTest {
    @Test
    public void t01() {
        DataSourceAdmin.setDefaultDataSourceName("def");
        DataSourceAdmin.addDataSource("def", BaseTest.mysqlConfig());
        FileSystemMyBatisMapperSql mapperSql = BaseTest.newFileSystemMyBatisMapperSql("./src/test/java", "");
        DataSourceAdmin.setMyBatisMapperSql(mapperSql);
        MyBatis myBatis = DaoFactory.getMyBatis(LocationClass.class);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("DANJ_NO", "CKD11500000026");
        // paramMap.put("HANGHAO", 2);
        TupleTwo<String, Map<String, Object>> sqlInfo = myBatis.getSql("t01", paramMap);
        log.info(" #1 ---> {} | {}", SqlLoggerUtils.deleteWhitespace(sqlInfo.getValue1()), sqlInfo.getValue2());
        DataSourceAdmin.closeAllDataSource();
    }

    @Test
    public void t02() {
        DataSourceAdmin.setDefaultDataSourceName("def");
        DataSourceAdmin.addDataSource("def", BaseTest.mysqlConfig());
        FileSystemMyBatisMapperSql mapperSql = BaseTest.newFileSystemMyBatisMapperSql("./src/test/resources", "dao/**/*.xml");
        DataSourceAdmin.setMyBatisMapperSql(mapperSql);
        MyBatis myBatis = DaoFactory.getMyBatis(LocationClass.class);
        DataSourceAdmin.closeAllDataSource();
    }

    @SneakyThrows
    @Test
    public void t03() {
        ClassPathMyBatisMapperSql mapperSql = BaseTest.newClassPathMyBatisMapperSql("classpath*:org/clever/data/jdbc/mapper/**/*.xml");
        DataSourceAdmin.setDefaultDataSourceName("def");
        DataSourceAdmin.addDataSource("def", BaseTest.mysqlConfig());
        DataSourceAdmin.setMyBatisMapperSql(mapperSql);
        try {
            final Mapper01 mapper01 = DaoFactory.getMapper(Mapper01.class);
            // 排序参数
            final QueryBySort sort = new QueryBySort();
            sort.addOrderField("createAt", QueryBySort.DESC);
            sort.addOrderFieldMapping("createAt", "create_at");
            // 分页参数
            final QueryByPage page = new QueryByPage(5, 1);
            page.addOrderField("createAt", QueryBySort.DESC);
            page.addOrderFieldMapping("createAt", "create_at");
            // 测试
            log.info("t01 -> {}", mapper01.t01(1L));
            log.info("t02 -> {}", mapper01.t02(2L));
            log.info("t03 -> {}", mapper01.t03(3L));
            log.info("t04 -> {}", mapper01.t04(4L));
            LinkedList<EntityData> t05 = mapper01.t05(5L);
            log.info("t05 -> {} | {}", t05.getClass().getName(), t05);
            HashMap<String, Object> t06 = mapper01.t06(6L);
            log.info("t06 -> {} | {}", t06.getClass().getName(), t06);
            log.info("t07 -> {}", mapper01.t07(1L));
            log.info("t08 -> {}", mapper01.t08(1L));
            log.info("t09 -> {}", mapper01.t09(1L));
            log.info("t10 -> {}", mapper01.t10("a"));
            log.info("t11 -> {}", mapper01.t11(1L));
            EntityData[] t12 = mapper01.t12(1L);
            log.info("t12 -> {} | {}", t12.getClass().getName(), t12);
            Map<String, Object>[] t13 = mapper01.t13(1L);
            log.info("t13 -> {} | {}", t13.getClass().getName(), t13);
            log.info("t14 -> {}", mapper01.t14(1L, sort));
            log.info("t15 -> {}", mapper01.t15(1L, page));
            log.info("t16 -> {}", mapper01.t16(1L, page));
            log.info("t17 -> {}", mapper01.t17(1L, page));
            mapper01.t18(1L, batchData -> {
                log.info("t18 -> {}", batchData);
                return true;
            });
            mapper01.t19(1L, rowData -> {
                log.info("t19 -> {}", rowData);
                return true;
            });
            mapper01.t20(1L, batchData -> log.info("t20 -> {}", batchData));
            mapper01.t21(1L, rowData -> log.info("t21 -> {}", rowData));
            log.info("t22 -> {}", mapper01.t22(1L, SnowFlake.SNOW_FLAKE.nextId()));
            log.info("t23 -> {}", mapper01.t23(1L, SnowFlake.SNOW_FLAKE.nextId()));
            log.info("t24 -> {}", mapper01.t24("t24"));
            log.info("t25 -> {}", mapper01.t25("t25"));
            Map<String, Object> t26_1 = new HashMap<String, Object>() {{
                put("str", "t26_1");
            }};
            Map<String, Object> t26_2 = new HashMap<String, Object>() {{
                put("str", "t26_2");
            }};
            Map<String, Object> t26_3 = new HashMap<String, Object>() {{
                put("str", "t26_3");
            }};
            log.info("t26 -> {}", mapper01.t26(Arrays.asList(t26_1, t26_2, t26_3)));
            log.info("t27 -> {}", mapper01.t27(Arrays.asList(t26_1, t26_2, t26_3)));
            Long[] t28 = mapper01.t28(Arrays.asList(t26_1, t26_2, t26_3));
            log.info("t28 -> {} | {}", t28.getClass(), Arrays.asList(t28));
            log.info("t29 -> {}", mapper01.t29(Arrays.asList(EntityData.create("t29_1"), EntityData.create("t29_2"), EntityData.create("t29_3"))));
            mapper01.t30("mapper");
            log.info("t31 -> {}", mapper01.t31("mapper", -1L));
        } finally {
            DataSourceAdmin.closeAllDataSource();
        }
    }

    @SneakyThrows
    @Test
    public void t04() {
        ClassPathMyBatisMapperSql mapperSql = BaseTest.newClassPathMyBatisMapperSql("classpath*:org/clever/data/jdbc/mapper/**/*.xml");
        DataSourceAdmin.setDefaultDataSourceName("def");
        DataSourceAdmin.addDataSource("def", BaseTest.mysqlConfig());
        DataSourceAdmin.setMyBatisMapperSql(mapperSql);
        try {
            final Mapper01 mapper01 = DaoFactory.getMapper(Mapper01.class);
            log.info(" -> {}", mapper01);
            log.info("t01 -> {}", mapper01.t01(1L));
            log.info("t01 -> {}", mapper01.t01(1L));
        } finally {
            DataSourceAdmin.closeAllDataSource();
        }
    }
}
