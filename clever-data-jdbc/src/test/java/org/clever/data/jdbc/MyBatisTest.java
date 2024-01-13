package org.clever.data.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.jdbc.entity.EntityData;
import org.clever.data.jdbc.mapper.LocationClass;
import org.clever.data.jdbc.mapper.Mapper01;
import org.clever.data.jdbc.mybatis.ClassPathMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
            Mapper01 mapper01 = DaoFactory.getMapper(Mapper01.class);
//            log.info("t01 -> {}", mapper01.t01(1L));
//            log.info("t02 -> {}", mapper01.t02(2L));
//            log.info("t03 -> {}", mapper01.t03(3L));
//            log.info("t04 -> {}", mapper01.t04(4L));
//            LinkedList<EntityData> t05 = mapper01.t05(5L);
//            log.info("t05 -> {} | {}", t05.getClass().getName(), t05);
//            HashMap<String, Object> t06 = mapper01.t06(6L);
//            log.info("t06 -> {} | {}", t06.getClass().getName(), t06);
//            log.info("t07 -> {}", mapper01.t07(1L));
//            log.info("t08 -> {}", mapper01.t08(1L));
//            log.info("t09 -> {}", mapper01.t09(1L));
//            log.info("t10 -> {}", mapper01.t10("a"));
//            log.info("t11 -> {}", mapper01.t11(1L));
            EntityData[] t12 = mapper01.t12(1L);
            log.info("t12 -> {} | {}", t12.getClass().getName(), t12);
        } finally {
            DataSourceAdmin.closeAllDataSource();
        }
    }
}
