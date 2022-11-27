//package org.clever.data.jdbc.mybatis;
//
//import org.clever.core.watch.FileSystemWatcher;
//import org.clever.data.dynamic.sql.builder.SqlSource;
//import org.clever.data.dynamic.sql.dialect.DbType;
//import org.clever.data.jdbc.support.SqlLoggerUtils;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.io.IOCase;
//import org.junit.jupiter.api.Test;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/12/03 14:46 <br/>
// */
//@Slf4j
//public class FileSystemMyBatisMapperSqlTest {
//
//    @SneakyThrows
//    @Test
//    public void t01() {
//        String absolutePath = "./src/test/resources/mybatis";
//        FileSystemMyBatisMapperSql mapperSql = new FileSystemMyBatisMapperSql(absolutePath);
//        FileSystemWatcher watcher = new FileSystemWatcher(
//                absolutePath,
//                file -> {
//                    final String absPath = FilenameUtils.normalize(file.getAbsolutePath(), true);
//                    String xmlPath = absPath.substring(mapperSql.getRootPathAbsolutePath().length());
//                    if (xmlPath.startsWith("/") || xmlPath.startsWith("\\")) {
//                        xmlPath = xmlPath.substring(1);
//                    }
//                    try {
//                        mapperSql.reloadFile(xmlPath, false);
//                    } catch (Exception e) {
//                        log.error("# 重新加载Mapper.xml文件失败 | path={}", absPath, e);
//                    }
//                },
//                new HashSet<String>() {{
//                    add("*.xml");
//                }},
//                new HashSet<>(),
//                IOCase.SYSTEM,
//                300
//        );
//        watcher.start();
//        Runtime.getRuntime().addShutdownHook(new Thread(watcher::stop));
//
//        String sqlId = "t01";
//        String stdXmlPath = "org/clever/biz/dao/UserDao.xml";
//        DbType dbType = DbType.ORACLE;
//        Map<String, Object> params = new HashMap<>();
//        params.put("a", "abc");
//        SqlSource sqlSource = mapperSql.getSqlSource(sqlId, stdXmlPath, DbType.SQL_SERVER);
//        log.info("-> {}", SqlLoggerUtils.deleteWhitespace(sqlSource.getBoundSql(dbType, params).getNamedParameterSql()));
//
//        sqlSource = mapperSql.getSqlSource(sqlId, stdXmlPath, dbType);
//        log.info("-> {}", SqlLoggerUtils.deleteWhitespace(sqlSource.getBoundSql(dbType, params).getNamedParameterSql()));
//
//        sqlSource = mapperSql.getSqlSource(sqlId, stdXmlPath, dbType, "yxt");
//        log.info("-> {}", SqlLoggerUtils.deleteWhitespace(sqlSource.getBoundSql(dbType, params).getNamedParameterSql()));
//
//        sqlSource = mapperSql.getSqlSource(sqlId, stdXmlPath, dbType, "yxt_report", "yxt");
//        log.info("-> {}", SqlLoggerUtils.deleteWhitespace(sqlSource.getBoundSql(dbType, params).getNamedParameterSql()));
//
//        log.info("\n\n");
//
//        for (int i = 0; i < 1; i++) {
//            Thread.sleep(3000);
//            sqlSource = mapperSql.getSqlSource(sqlId, stdXmlPath, dbType, "yxt");
//            log.info("-> {}", SqlLoggerUtils.deleteWhitespace(sqlSource.getBoundSql(dbType, params).getNamedParameterSql()));
//        }
//    }
//}
