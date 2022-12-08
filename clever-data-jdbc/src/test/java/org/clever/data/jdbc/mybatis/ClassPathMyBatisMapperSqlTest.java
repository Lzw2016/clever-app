package org.clever.data.jdbc.mybatis;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.clever.core.io.ClassPathResource;
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
}
