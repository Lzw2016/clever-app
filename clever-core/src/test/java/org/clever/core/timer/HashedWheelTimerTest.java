package org.clever.core.timer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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

    @Test
    public void t02() throws Exception {
        HashedWheelTimer timer = new HashedWheelTimer();
        for (int i = 100; i < 130; i++) {
            int ser = i;
            Timeout handler = timer.newTimeout(timeout -> {
                Thread.sleep(ser * 5);
                log.info("--> {}", ser);
            }, i * 5, TimeUnit.MILLISECONDS);
            log.info("pendingTimeouts--> {}", timer.pendingTimeouts());
            handler.cancel();
        }
        Thread.sleep(1000 * 2);
        log.info("pendingTimeouts--> {}", timer.pendingTimeouts());
        Set<Timeout> res = timer.stop();
        log.info("res--> {}", res.size());
        Thread.sleep(1000 * 2);
        log.info("pendingTimeouts--> {}", timer.pendingTimeouts());
        log.info("#end");
    }

    @Test
    public void t03() throws Exception {
        ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor(
            new BasicThreadFactory.Builder()
                .namingPattern("test-%s")
                .daemon(true)
                .build()
        );
        ScheduledFuture<?> future_1 = scheduled.scheduleAtFixedRate(() -> log.info("任务1"), 0, 500, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> future_2 = scheduled.scheduleAtFixedRate(() -> log.info("任务2"), 0, 1500, TimeUnit.MILLISECONDS);
        Thread.sleep(1000 * 3);
        future_1.cancel(true);
        Thread.sleep(1000 * 6);
        future_2.cancel(true);
        Thread.sleep(1000 * 3);
        scheduled.shutdown();
        log.info("#end");
    }

    @Test
    public void t04() throws Exception {
        HashedWheelTimer timer = new HashedWheelTimer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                log.info("@@@");
                if (!timer.isStop()) {
                    timer.newTimeout(this, 300, TimeUnit.MILLISECONDS);
                }
            }
        };
        timer.newTimeout(task, 300, TimeUnit.MILLISECONDS);
        Thread.sleep(1000 * 10);
        Set<Timeout> res = timer.stop();
        log.info("res--> {}", res.size());
        Thread.sleep(1000 * 2);
        log.info("pendingTimeouts--> {}", timer.pendingTimeouts());
        log.info("#end");
    }
}
