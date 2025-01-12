package org.clever.data.jdbc.mybatis;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/03 14:46 <br/>
 */
@Slf4j
public class FileSystemMyBatisMapperSqlTest {
    @SneakyThrows
    @Test
    public void t01() {
        final String absolutePath = new File("./src/test/resources/dao").getAbsolutePath();
        FileSystemMyBatisMapperSql myBatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
        myBatisMapperSql.reloadAll();
        log.info("加载完成");
        SqlSource sqlSource = myBatisMapperSql.getSqlSource("t01", "UserDao.xml", DbType.MYSQL);
        log.info("SQL = {}", sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql());
        myBatisMapperSql.startWatch(100);
        Thread.sleep(10_000);
        sqlSource = myBatisMapperSql.getSqlSource("t01", "UserDao.xml", DbType.MYSQL);
        log.info("SQL = {}", sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql());
    }

    @Test
    public void t02() {
        final String absolutePath = new File("./src/test/resources/performance_test").getAbsolutePath();
        final long startTime = System.currentTimeMillis();
        final int count = 10;
        long firstTime = 0;
        for (int i = 0; i < count; i++) {
            FileSystemMyBatisMapperSql myBatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
            myBatisMapperSql.reloadAll();
            // ### SqlSourceCount=654
            log.info("### SqlSourceCount={}", myBatisMapperSql.getSqlSourceCount());
            if (firstTime == 0) {
                firstTime = System.currentTimeMillis() - startTime;
            }
        }
        final long endTime = System.currentTimeMillis();
        FileSystemMyBatisMapperSql myBatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
        //  176ms/次 | 第一次:415ms | 总时间:1767ms | sql.xml文件数量:157
        log.info("{}ms/次 | 第一次:{}ms | 总时间:{}ms | sql.xml文件数量:{}",
            (endTime - startTime) / count,
            firstTime,
            (endTime - startTime),
            myBatisMapperSql.getAllLastModified().size()
        );
    }

    @Test
    public void t03() {
        final String absolutePath = new File("./src/test/resources/dao").getAbsolutePath();
        FileSystemMyBatisMapperSql myBatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
        SqlSource sqlSource = myBatisMapperSql.getSqlSource("t01", "UserDao.xml", DbType.MYSQL);
        log.info("SQL = {}", sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql());
    }

    @SneakyThrows
    @Test
    public void t04() {
        final String absolutePath = new File("./src/test/resources/performance_test").getAbsolutePath();
        FileSystemMyBatisMapperSql myBatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
        myBatisMapperSql.startWatch(100);
        Thread.sleep(2_000);
        log.info("### SqlSourceCount={}", myBatisMapperSql.getSqlSourceCount());
    }
}
