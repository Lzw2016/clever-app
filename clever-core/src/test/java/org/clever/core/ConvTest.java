package org.clever.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 14:12 <br/>
 */
@Slf4j
public class ConvTest {
    @Test
    public void test01() {
        log.info("-> {}", Conv.asInteger(1));
    }
}
