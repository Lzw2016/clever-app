//package org.clever.data.jdbc;
//
//import org.clever.data.dynamic.sql.dialect.func.JoinFuncTransform;
//import org.clever.data.dynamic.sql.dialect.utils.SqlFuncTransformUtils;
//import org.clever.data.jdbc.mapper.LocationClass;
//import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/12/06 17:02 <br/>
// */
//@Slf4j
//public class MyBatisTest {
//    @SneakyThrows
//    @Test
//    public void t01() {
//        DataSourceAdmin.setDefaultDataSourceName("def");
//        DataSourceAdmin.addDataSource("def", BaseTest.newHikariConfig());
//        String absolutePath = "./src/test/java";
//        FileSystemMyBatisMapperSql mapperSql = BaseTest.newFileSystemMyBatisMapperSql(absolutePath);
//        DataSourceAdmin.setDefaultMapperSqlName(mapperSql.getRootPathAbsolutePath());
//        DataSourceAdmin.addMyBatisMapperSql(mapperSql.getRootPathAbsolutePath(), mapperSql);
//        MyBatis myBatis = DaoFactory.getMyBatis(LocationClass.class);
//
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("DANJ_NO", "CKD11500000026");
//        // paramMap.put("HANGHAO", 2);
//        List<?> list = myBatis.queryMany("t01", paramMap);
//        log.info(" #1 ---> {}", list);
//
//        paramMap.clear();
//        paramMap.put("LOT", "20180815173703");
//        list = myBatis.queryMany("t01", paramMap, JdbcTest.YwXjzl.class);
//        log.info(" #2 ---> {}", list);
//
//        SqlFuncTransformUtils.register(new JoinFuncTransform());
//        paramMap.clear();
//        paramMap.put("DANJ_NO_List", Arrays.asList("CKD11500000026", "CKD11500000009", "CKD11500000029"));
//        list = myBatis.queryMany("t01", paramMap);
//        log.info(" #3 ---> {}", list);
//
//        for (int i = 0; i < 1; i++) {
//            Thread.sleep(8000);
//            list = myBatis.queryMany("t01", paramMap);
//            log.info(" #4 ---> {}", list);
//        }
//        myBatis.close();
//    }
//}
