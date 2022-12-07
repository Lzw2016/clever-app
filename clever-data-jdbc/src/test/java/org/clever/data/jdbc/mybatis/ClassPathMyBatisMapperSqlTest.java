package org.clever.data.jdbc.mybatis;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.io.Resource;
import org.clever.core.io.support.PathMatchingResourcePatternResolver;
import org.junit.jupiter.api.Test;

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
        Resource[] resources = resolver.getResources("classpath*:**/*.xml");
        for (Resource resource : resources) {
            log.info("--> {}", resource.getClass());
        }

        Resource resource = resolver.getResource("org/clever/jdbc/support/sql-error-codes.xml");
        log.info("--> {} | lastModified={}", resource.getURL(), resource.lastModified());
    }
}
