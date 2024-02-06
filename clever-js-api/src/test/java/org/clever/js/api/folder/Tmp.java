package org.clever.js.api.folder;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.io.Resource;
import org.clever.core.io.support.PathMatchingResourcePatternResolver;
import org.clever.core.io.support.ResourcePatternResolver;
import org.clever.util.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/25 21:34 <br/>
 */
@Slf4j
public class Tmp {

    @Test
    public void t01() {
        log.info("## -> {}", new Date().toString());
    }

    @Test
    public void t02() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "file/**/*.txt");
        for (Resource resource : resources) {
            log.info(" -> {}", resource.getURL().toExternalForm());
            if (!resource.exists()) {
                log.debug("###不存在 -> {}", resource.getURL().toExternalForm());
            }
        }

        resources = resolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "**/*.txt");
        for (Resource resource : resources) {
            log.info(" -> {}", resource.getURL().toExternalForm());
            if (!resource.exists()) {
                log.debug("###不存在 -> {}", resource.getURL().toExternalForm());
            }
        }
    }

    @Test
    public void t03() {
        String pathUrl = "jar:file:D:\\ToolsSoftware\\Maven\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar!/LICENSE.txt";
        String path = FilenameUtils.normalize(pathUrl);
        log.info("path -> {}", path);
        path = FilenameUtils.concat(pathUrl, "aaa.js");
        log.info("path -> {}", path);
        path = FilenameUtils.getName(pathUrl);
        log.info("path -> {}", path);

        pathUrl = "jar:file:D:/ToolsSoftware/Maven/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar!/LICENSE.txt";
        path = FilenameUtils.normalize(pathUrl);
        log.info("path -> {}", path);
        path = FilenameUtils.concat(pathUrl, "aaa.js");
        log.info("path -> {}", path);
        path = FilenameUtils.getName(pathUrl);
        log.info("path -> {}", path);
    }

    @Test
    public void t05() {
        final String pathUrl = "file:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes/file/01a.txt";
        String path = FilenameUtils.normalize(pathUrl);
        log.info("path -> {}", path);

        path = FilenameUtils.concat(pathUrl, "aaa.js");
        log.info("path -> {}", path);

        path = FilenameUtils.getName(pathUrl);
        log.info("path -> {}", path);
    }

    @SneakyThrows
    @Test
    public void t06() {
        String pathUrl = "classpath*:D:\\ToolsSoftware\\Maven\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar!/LICENSE.txt";
        File file = ResourceUtils.getFile(pathUrl);
        log.info("file -> {}", file.getName());

        pathUrl = "classpath*:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes/file/01a.txt";
        file = ResourceUtils.getFile(pathUrl);
        log.info("file -> {}", file.getName());
    }

    @SneakyThrows
    @Test
    public void t07() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String pathUrl = "jar:file:D:/ToolsSoftware/Maven/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar!/LICENSE.txt";
        Resource resource = resolver.getResource(pathUrl);
        log.info("resource -> {}", resource.getURL().toExternalForm());

        pathUrl = "file:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes/file/01a.txt";
        resource = resolver.getResource(pathUrl);
        log.info("resource -> {}", resource.getURL().toExternalForm());
    }


    @SneakyThrows
    @Test
    public void t08() {
        URL url = ResourceUtils.getURL("classpath:");
        log.info("url -> {}", url.toExternalForm());
        // noinspection ConstantConditions
        log.info("url -> {}", this.getClass().getClassLoader().getResource("").toExternalForm());
    }

    @Test
    public void t09() {
        log.info("path -> {}", FilenameUtils.normalize("jar:file:/D:/SourceCode/clever/hinny-spring-example/hinny-spring-example-1.0.0-SNAPSHOT.jar!/BOOT-INF/classes!" + "/aaa.js"));
        log.info("path -> {}", FilenameUtils.normalize("file:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes" + "/aaa.js"));

        log.info("path -> {}", FilenameUtils.normalize("jar:file:/D:/SourceCode/clever/hinny-spring-example/hinny-spring-example-1.0.0-SNAPSHOT.jar!/BOOT-INF/classes!/" + "/aaa.js"));
        log.info("path -> {}", FilenameUtils.normalize("file:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes/" + "/aaa.js"));

        log.info("path -> {}", FilenameUtils.getName("jar:file:/D:/SourceCode/clever/hinny-spring-example/hinny-spring-example-1.0.0-SNAPSHOT.jar!/BOOT-INF/classes!/" + "/aaa.js"));
        log.info("path -> {}", FilenameUtils.getName("file:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes/" + "/aaa.js"));

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        log.info("url -> {}", resolver.getResource("jar:file:/D:/SourceCode/clever/hinny-spring-example/hinny-spring-example-1.0.0-SNAPSHOT.jar!/BOOT-INF/classes!/aaa.js").exists());
        log.info("url -> {}", resolver.getResource("file:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes/file/01a.txt").exists());
    }

    @Test
    public void t10() {
        String path = "jar:file:/D:/hinny-spring-example-1.0.0-SNAPSHOT.jar!/BOOT-INF/classes!/file/01a/aaa.js";
        String jarFullPath = "!/";
        String pathMatchResource = path.substring(0, path.lastIndexOf(jarFullPath) + jarFullPath.length());
        log.info("--> {}", pathMatchResource);

        pathMatchResource = "jar:file:/D:/hinny-spring-example-1.0.0-SNAPSHOT.jar!/BOOT-INF/classes!/file";
        String childPath = path.substring(pathMatchResource.length());
        log.info("--> {}", childPath);
        String[] childArr = StringUtils.split(childPath, Folder.PATH_SEPARATE);
        log.info("--> {}", Arrays.toString(childArr));
    }
}
