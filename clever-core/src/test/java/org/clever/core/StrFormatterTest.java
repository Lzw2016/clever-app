package org.clever.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/10/02 19:18 <br/>
 */
@Slf4j
public class StrFormatterTest {
    @Test
    public void t01() {
        log.info("--> {}", StrFormatter.toPlainString(Duration.ofDays(3).plus(Duration.ofSeconds(45))));
        log.info("--> {}", StrFormatter.toPlainString(Duration.ofMillis(10L * 1000L)));
    }
}
