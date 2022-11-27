//package org.clever.data.jdbc;
//
//import org.clever.core.watch.FileSystemWatcher;
//import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
//import com.zaxxer.hikari.HikariConfig;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.io.IOCase;
//
//import java.util.HashSet;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/06/12 21:39 <br/>
// */
//@Slf4j
//public class BaseTest {
//    public static HikariConfig newHikariConfig() {
//        HikariConfig hikariConfig = new HikariConfig();
//        hikariConfig.setDriverClassName("oracle.jdbc.OracleDriver");
//        hikariConfig.setJdbcUrl("jdbc:oracle:thin:@122.9.140.63:1521:YXT");
//        hikariConfig.setUsername("lmis");
//        hikariConfig.setPassword("lmis9system");
//        hikariConfig.setAutoCommit(false);
//        hikariConfig.setMinimumIdle(1);
//        hikariConfig.setMaximumPoolSize(10);
//        return hikariConfig;
//    }
//
//    public static Jdbc newJdbcDataSource() {
//        return new Jdbc(newHikariConfig());
//    }
//
////    public static MyBatis newMyBatisJdbcDataSource(String absolutePath) {
////        FileSystemMyBatisMapperSql mapperSql = new FileSystemMyBatisMapperSql(absolutePath);
////        FileSystemWatcher watcher = new FileSystemWatcher(
////                absolutePath,
////                file -> {
////                    final String absPath = file.getAbsolutePath();
////                    try {
////                        mapperSql.reloadFile(absPath, false);
////                    } catch (Exception e) {
////                        String error = e.getMessage();
////                        if (e.getCause() instanceof SAXParseException) {
////                            SAXParseException saxParseException = (SAXParseException) e.getCause();
////                            error = String.format(
////                                    "#第%d行，第%d列存在错误: %s",
////                                    saxParseException.getLineNumber(),
////                                    saxParseException.getColumnNumber(),
////                                    saxParseException.getMessage()
////                            );
////                        }
////                        log.error("#重新加载Mapper.xml文件失败 | path={} | error={}", absPath, error);
////                    }
////                },
////                new HashSet<>(),
////                new HashSet<String>() {{
////                    add("*.xml");
////                }},
////                IOCase.SYSTEM,
////                3000
////        );
////        watcher.start();
////        Runtime.getRuntime().addShutdownHook(new Thread(watcher::stop));
////        return new MyBatis(newJdbcDataSource(), mapperSql);
////    }
//
//    public static FileSystemMyBatisMapperSql newFileSystemMyBatisMapperSql(String absolutePath) {
//        // String absolutePath = "./src/test/resources/mybatis";
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
//        return mapperSql;
//    }
//}
//
