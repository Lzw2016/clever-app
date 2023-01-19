package org.clever.app.task;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.SystemClock;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/19 10:16 <br/>
 */
@Slf4j
public class TaskTest {
    public static void job01() {
        log.info("定时任务: {}", SystemClock.now());
    }
}
