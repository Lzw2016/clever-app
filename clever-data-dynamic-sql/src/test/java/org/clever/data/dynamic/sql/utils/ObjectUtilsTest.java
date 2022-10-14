package org.clever.data.dynamic.sql.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/03/30 15:41 <br/>
 */
@Slf4j
public class ObjectUtilsTest {
    @Test
    public void t01() {
        log.info("-> {}", ObjectUtils.Instance.isIn("", "a", "b"));
        log.info("-> {}", ObjectUtils.Instance.isIn("a", "a", "b"));
        log.info("-> {}", ObjectUtils.Instance.isIn("a", 'a', "b"));
        log.info("-> {}", ObjectUtils.Instance.notIn("", "a", "b"));
        log.info("-> {}", ObjectUtils.Instance.notIn("a", "a", "b"));
        log.info("-> {}", ObjectUtils.Instance.notIn("a", 'a', "b"));
    }
}
