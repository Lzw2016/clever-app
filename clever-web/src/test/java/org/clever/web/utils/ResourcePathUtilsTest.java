package org.clever.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.ResourcePathUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 21:25 <br/>
 */
@Slf4j
public class ResourcePathUtilsTest {
    @Test
    public void t01() {
        final String basePath = "./src/test/resources/static";
        Resource resource = ResourcePathUtils.getResource(basePath, "index.html");
        log.info("-> {}", ResourcePathUtils.getAbsolutePath(resource));
    }

    @Test
    public void t02() {
        final String basePath = "classpath:static";
        Resource resource = ResourcePathUtils.getResource(basePath, "index.html");
        log.info("-> {}", ResourcePathUtils.getAbsolutePath(resource));
    }
}
