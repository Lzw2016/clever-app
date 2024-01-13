package org.clever.data.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.tuples.TupleTwo;
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
            log.info("t01 -> {}", mapper01.t01(2L));
            log.info("t02 -> {}", mapper01.t02(3L));
        } finally {
            DataSourceAdmin.closeAllDataSource();
        }
    }
}
