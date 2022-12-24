package org.clever.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.io.Resource;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 21:25 <br/>
 */
@Slf4j
public class PathUtilsTest {
    @Test
    public void t01() {
        final String basePath = "./src/test/resources/static";
        Resource resource = PathUtils.getResource(basePath, "index.html");
        log.info("-> {}", PathUtils.getAbsolutePath(resource));
    }

    @Test
    public void t02() {
        final String basePath = "classpath:static";
        Resource resource = PathUtils.getResource(basePath, "index.html");
        log.info("-> {}", PathUtils.getAbsolutePath(resource));
    }
}
