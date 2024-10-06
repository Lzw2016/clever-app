package org.clever.boot;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/05/09 22:16 <br/>
 */
@Slf4j
public class ClassUtilsTest {
    @Test
    public void t01() {
        log.info("getQualifiedName --> {}", ClassUtils.getQualifiedName(ClassUtilsTest.class));
        log.info("getShortName     --> {}", ClassUtils.getShortName(ClassUtilsTest.class));
    }
}
