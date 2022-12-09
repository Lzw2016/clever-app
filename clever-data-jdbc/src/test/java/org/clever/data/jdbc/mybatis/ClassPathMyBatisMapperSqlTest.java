package org.clever.data.jdbc.mybatis;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.clever.core.io.ClassPathResource;
import org.clever.core.io.Resource;
import org.clever.core.io.support.PathMatchingResourcePatternResolver;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/07 21:48 <br/>
 */
@Slf4j
public class ClassPathMyBatisMapperSqlTest {
    @SneakyThrows
    @Test
    public void t01() {
        // “classpath”： 用于加载类路径（包括jar包）中的一个且仅一个资源；对于多个匹配的也只返回一个，所以如果需要多个匹配的请考虑“classpath*:”前缀；
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:**/*.xml");
        for (Resource resource : resources) {
            if (resource.isFile()) {
                log.info("--> {} | lastModified={}", resource.getFile().getAbsolutePath(), resource.lastModified());
            } else {
                log.info("--> {} | lastModified={}", resource.getURL().toExternalForm(), resource.lastModified());
            }
        }
    }

    @SneakyThrows
    @Test
    public void t02() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("org/clever/jdbc/support/sql-error-codes.xml");
        log.info("--> {} | lastModified={}", resource.getURL(), resource.lastModified());
        // HikariCP-4.0.3.jar!/META-INF/maven/com.zaxxer/HikariCP/pom.xml
        resource = resolver.getResource("META-INF/maven/com.zaxxer/HikariCP/pom.xml");
        log.info("--> {} | lastModified={}", resource.getURL(), resource.lastModified());
    }

    @SneakyThrows
    @Test
    public void t03() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("org/clever/jdbc/support/sql-error-codes.xml");
        log.info("--> {}", ((ClassPathResource) resource).getPath());

        Resource[] resources = resolver.getResources("classpath*:**/*.xml");
        for (Resource res : resources) {
            String[] flagArr = {
                    // jar包
                    ".jar!/",
                    // IDEA 自带编译器
                    "/out/production/classes/",
                    "/out/production/resources/",
                    "/out/test/classes/",
                    "/out/test/resources/",
                    // gradle编译器
                    "/build/classes/java/main/",
                    "/build/classes/java/test/",
                    "/build/classes/kotlin/main/",
                    "/build/classes/kotlin/test/",
                    "/build/classes/groovy/main/",
                    "/build/classes/groovy/test/",
                    "/build/resources/main/",
                    "/build/resources/test/",
                    // maven编译器
                    "/target/classes/",
                    "/target/test-classes/",
            };
            String url = res.getURL().toExternalForm();
            for (String flag : flagArr) {
                int idx = url.indexOf(flag);
                if (idx >= 0) {
                    url = url.substring(idx + flag.length());
                    break;
                }
            }
            url = FilenameUtils.normalize(url, true);
            log.info("--> {}", url);
        }
    }

    @SneakyThrows
    @Test
    public void t04() {
        ClassPathMyBatisMapperSql myBatisMapperSql = new ClassPathMyBatisMapperSql("classpath:dao/*.xml");
        myBatisMapperSql.reloadAll();
        log.info("加载完成");
        SqlSource sqlSource = myBatisMapperSql.getSqlSource("t01", "dao/UserDao.xml", DbType.MYSQL);
        log.info("SQL = {}", sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql());
        myBatisMapperSql.startWatch(100);
        Thread.sleep(10_000);
        sqlSource = myBatisMapperSql.getSqlSource("t01", "dao/UserDao.xml", DbType.MYSQL);
        log.info("SQL = {}", sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql());
    }

    @Test
    public void t05() {
        final long startTime = System.currentTimeMillis();
        final int count = 10;
        final String locationPattern = "classpath:performance_test/**/*.xml";
        long firstTime = 0;
        for (int i = 0; i < count; i++) {
            ClassPathMyBatisMapperSql myBatisMapperSql = new ClassPathMyBatisMapperSql(locationPattern);
            myBatisMapperSql.reloadAll();
            // ### SqlSourceCount=654
            log.info("### SqlSourceCount={}", myBatisMapperSql.getSqlSourceCount());
            if (firstTime == 0) {
                firstTime = System.currentTimeMillis() - startTime;
            }
        }
        final long endTime = System.currentTimeMillis();
        ClassPathMyBatisMapperSql myBatisMapperSql = new ClassPathMyBatisMapperSql(locationPattern);
        // 198ms/次 | 第一次:434ms | 总时间:1989ms | sql.xml文件数量:157
        log.info("{}ms/次 | 第一次:{}ms | 总时间:{}ms | sql.xml文件数量:{}",
                (endTime - startTime) / count,
                firstTime,
                (endTime - startTime),
                myBatisMapperSql.getAllLastModified().size()
        );
    }

    @Test
    public void t06() {
        ClassPathMyBatisMapperSql myBatisMapperSql = new ClassPathMyBatisMapperSql("classpath:dao/*.xml");
        SqlSource sqlSource = myBatisMapperSql.getSqlSource("t01", "dao/UserDao.xml", DbType.MYSQL);
        log.info("SQL = {}", sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql());
    }

    @SneakyThrows
    @Test
    public void t07() {
        final String locationPattern = "classpath:performance_test/**/*.xml";
        ClassPathMyBatisMapperSql myBatisMapperSql = new ClassPathMyBatisMapperSql(locationPattern);
        myBatisMapperSql.startWatch(100);
        Thread.sleep(2_000);
        log.info("### SqlSourceCount={}", myBatisMapperSql.getSqlSourceCount());
    }
}
