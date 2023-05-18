package org.clever.core.timer;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/18 18:20 <br/>
 */
@Slf4j
public class HashedWheelTimerTest {
    @Test
    public void t01() throws Exception {
        HashedWheelTimer timer = new HashedWheelTimer();
        for (int i = 100; i < 130; i++) {
            int ser = i;
            timer.newTimeout(timeout -> {
                Thread.sleep(ser * 5);
                log.info("--> {}", ser);
            }, i * 5, TimeUnit.MILLISECONDS);
            log.info("pendingTimeouts--> {}", timer.pendingTimeouts());
        }
        Thread.sleep(1000 * 10);
        Set<Timeout> res = timer.stop();
        log.info("res--> {}", res.size());
        Thread.sleep(1000 * 2);
        log.info("pendingTimeouts--> {}", timer.pendingTimeouts());
        log.info("#end");
    }
}
