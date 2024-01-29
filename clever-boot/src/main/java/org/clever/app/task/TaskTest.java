package org.clever.app.task;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.DateUtils;
import org.clever.task.core.job.JobContext;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/19 10:16 <br/>
 */
@Slf4j
public class TaskTest {
    public static void job01() {
        log.info("定时任务: {}", DateUtils.getCurrentDate("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public static void job02(JobContext context) {
        context.info("开始");
        context.info("定时任务: dbNow={}", context.getDbNow());
        for (int i = 0; i < 300; i++) {
            context.info("测试: {}", i);
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
        }
        context.info("完成");
    }
}
