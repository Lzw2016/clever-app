package org.clever.task.core.support;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.DateUtils;
import org.clever.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/23 22:19 <br/>
 */
@Slf4j
public class WheelTimerTest {
    @Test
    public void t01() throws Exception {
        WheelTimer.Clock clock = SystemClock::now;
        WheelTimer timer = new WheelTimer(Executors.defaultThreadFactory(), Executors.newSingleThreadExecutor(), clock, 10, TimeUnit.MILLISECONDS, 512);
        final WheelTimer.Task task = new WheelTimer.Task() {
            @Override
            public long getId() {
                return 0;
            }

            @Override
            public void run(WheelTimer.TaskInfo taskInfo) {
                log.info("@@@");
                if (!timer.isStop()) {
                    timer.addTask(this, 100, TimeUnit.MILLISECONDS);
                }
            }
        };
        WheelTimer.TaskInfo taskInfo = timer.addTask(task, 100, TimeUnit.MILLISECONDS);
        log.info("--> {}", taskInfo.getState());
        Thread.sleep(1000 * 10);
        timer.stop();
        Thread.sleep(1000 * 2);
        log.info("pendingTimeouts--> {}", timer.pendingTasks());
        log.info("#end");
    }

    @Test
    public void t02() throws Exception {
        WheelTimer.Clock clock = SystemClock::now;
        WheelTimer timer = new WheelTimer(Executors.defaultThreadFactory(), Executors.newSingleThreadExecutor(), clock, 10, TimeUnit.MILLISECONDS, 512);
        final WheelTimer.Task task = new WheelTimer.Task() {
            @Override
            public long getId() {
                return 0;
            }

            @Override
            public void run(WheelTimer.TaskInfo taskInfo) {
                log.info("@@@");
            }
        };
        timer.addTask(task, 100, TimeUnit.MILLISECONDS);
        Thread.sleep(200);
        timer.addTask(task, 100, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 100; i++) {
            Thread.sleep(50);
            timer.addTask(task, 100, TimeUnit.MILLISECONDS);
        }
        Thread.sleep(1000 * 12);
        timer.stop();
        Thread.sleep(1000 * 2);
        log.info("pendingTimeouts--> {}", timer.pendingTasks());
        log.info("#end");
    }

    @Test
    public void t03() throws Exception {
        AtomicLong count = new AtomicLong(0);
        WheelTimer.Clock clock = SystemClock::now;
        WheelTimer timer = new WheelTimer(Executors.defaultThreadFactory(), Executors.newSingleThreadExecutor(), clock, 10, TimeUnit.MILLISECONDS, 512);
        final Supplier<WheelTimer.Task> task = () -> new WheelTimer.Task() {
            private final long id = count.incrementAndGet();

            @Override
            public long getId() {
                return id;
            }

            @Override
            public void run(WheelTimer.TaskInfo taskInfo) {
                log.info("@@@ -> {}", id);
            }
        };
        timer.start();
        Date now = new Date();
        WheelTimer.TaskInfo taskInfo = timer.addTask(task.get(), now);
        log.info("--> {}", taskInfo.getState());
        for (int i = 1; i <= 1000; i++) {
            timer.addTask(task.get(), DateUtils.addMilliseconds(now, i * 100));
        }
        log.info("pendingTimeouts--> {}", timer.pendingTasks());
        Thread.sleep(1000 * 120);
        timer.stop();
        Thread.sleep(1000 * 2);
        log.info("pendingTimeouts--> {}", timer.pendingTasks());
        log.info("#end");
    }
}
