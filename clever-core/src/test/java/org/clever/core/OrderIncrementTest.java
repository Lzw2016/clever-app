package org.clever.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/28 09:54 <br/>
 */
@Slf4j
public class OrderIncrementTest {
    @Test
    public void t01() {
        OrderIncrement orderIncrement = new OrderIncrement();
        int count = 19;
        log.info("### 1");
        for (int i = 0; i < count; i++) {
            log.info("#1--> {}", orderIncrement.incrL1());
        }
        log.info("### 2");
        for (int i2 = 0; i2 < count; i2++) {
            log.info("#2--> {}", orderIncrement.incrL2());
        }
        log.info("### 3");
        for (int i3 = 0; i3 < count; i3++) {
            log.info("#3--> {}", orderIncrement.incrL3());
        }
        log.info("### 4");
        for (int i4 = 0; i4 < count; i4++) {
            log.info("#4--> {}", orderIncrement.incrL4());
        }
        log.info("### 5");
        for (int i5 = 0; i5 < count; i5++) {
            log.info("#5--> {}", orderIncrement.incrL5());
        }
        log.info("### 6");
        for (int i6 = 0; i6 < count; i6++) {
            log.info("#6--> {}", orderIncrement.incrL6());
        }
    }
}
