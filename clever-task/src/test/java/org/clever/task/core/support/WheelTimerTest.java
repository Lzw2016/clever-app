package org.clever.task.core.support;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/23 22:19 <br/>
 */
@Slf4j
public class WheelTimerTest {
    @Test
    public void t01() throws Exception {
        WheelTimer timer = new WheelTimer(Executors.defaultThreadFactory(), Executors.newSingleThreadExecutor(), 10, TimeUnit.MILLISECONDS, 512);
        final WheelTimer.Task task = new WheelTimer.Task() {
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
}
