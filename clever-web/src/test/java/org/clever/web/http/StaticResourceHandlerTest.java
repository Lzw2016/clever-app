package org.clever.web.http;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.io.DefaultResourceLoader;
import org.clever.core.io.FileSystemResourceLoader;
import org.clever.core.io.Resource;
import org.clever.util.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 15:32 <br/>
 */
@Slf4j
public class StaticResourceHandlerTest {
    @SneakyThrows
    @Test
    public void t01() {
        final String path = "./src/test/resources/static/";
        final String absolutePath = new File(path).getAbsolutePath();
        log.info("-> {}", absolutePath);
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("file:" + path);
        log.info("-> {}", resource.getFile().getAbsolutePath());
        Resource file = resource.createRelative("index.html");
        log.info("-> {}", file.getFile().getAbsolutePath());
    }

    @SneakyThrows
    @Test
    public void t02() {
        final String path = "./src/test/resources/static";
        final String absolutePath = new File(path).getAbsolutePath();
        log.info("-> {}", absolutePath);
        FileSystemResourceLoader resourceLoader = new FileSystemResourceLoader();
        Resource resource = resourceLoader.getResource(path);
        log.info("-> {}", resource.getFile().getAbsolutePath());
        Resource file = resource.createRelative("static/index.html");
        log.info("-> {}", file.getFile().getAbsolutePath());
    }

    @SneakyThrows
    @Test
    public void t03() {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:static/");
        log.info("-> {}", resource.getFile().getAbsolutePath());
        Resource file = resource.createRelative("index.html");
        log.info("-> {}", file.getFile().getAbsolutePath());
    }

    @SneakyThrows
    @Test
    public void t04() {
        FileSystemResourceLoader resourceLoader = new FileSystemResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:static/");
        log.info("-> {}", resource.getFile().getAbsolutePath());
        Resource file = resource.createRelative("index.html");
        log.info("-> {}", file.getFile().getAbsolutePath());
    }

    @SneakyThrows
    @Test
    public void t05() {
        final String path = "./src/test/resources/static";
        final String absolutePath = new File(path).getAbsolutePath();
        log.info("-> {}", absolutePath);
        File resource = ResourceUtils.getFile(path);
        log.info("-> {}", resource.getAbsolutePath());
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource res = resourceLoader.getResource("classpath:META-INF/maven/org.apache.commons/commons-lang3");
        log.info("-> {}", res.getURL().toExternalForm());
    }
}
