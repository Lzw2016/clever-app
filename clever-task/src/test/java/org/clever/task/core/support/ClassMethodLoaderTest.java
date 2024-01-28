package org.clever.task.core.support;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.DateUtils;
import org.clever.core.tuples.TupleTwo;
import org.clever.task.core.job.JobContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/27 22:06 <br/>
 */
@Slf4j
public class ClassMethodLoaderTest {
    @Test
    public void t01() {
        TupleTwo<Class<?>, Method> tuple = ClassMethodLoader.getMethod("org.clever.task.core.support.TaskTest", "job01");
        if (tuple == null) {
            log.info("###");
            return;
        }
        log.info("-> {}", tuple.getValue2());
        tuple = ClassMethodLoader.getMethod("org.clever.task.core.support.TaskTest", "job02");
        if (tuple == null) {
            log.info("###");
            return;
        }
        log.info("-> {}", tuple.getValue2());
    }
}

@Slf4j
class TaskTest {
    public static void job01() {
        log.info("定时任务: {}", DateUtils.getCurrentDate("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public static void job02(JobContext context) {
        log.info("定时任务: dbNow={}", context.getDbNow());
    }
}
