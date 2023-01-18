package org.clever.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.util.Assert;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * 应用程序启动时的任务
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/18 22:54 <br/>
 */
@Slf4j
public class AppStartUpTask {
    private static final List<StartUpTask> STARTUP_TASK_LIST = new LinkedList<>();
    private static volatile boolean isStarted = false;

    /**
     * 注册开机任务
     *
     * @param runnable 开机任务
     * @param order    执行顺序，值越小，优先级越高
     * @param name     任务名称
     */
    public synchronized static void addStartUpTask(Runnable runnable, double order, String name) {
        STARTUP_TASK_LIST.add(new StartUpTask(runnable, order, name));
    }

    /**
     * 注册开机任务
     *
     * @param runnable 开机任务
     * @param order    执行顺序，值越小，优先级越高
     */
    public synchronized static void addStartUpTask(Runnable runnable, double order) {
        STARTUP_TASK_LIST.add(new StartUpTask(runnable, order, null));
    }

    /**
     * 注册开机任务
     *
     * @param runnable 开机任务
     */
    public synchronized static void addStartUpTask(Runnable runnable) {
        STARTUP_TASK_LIST.add(new StartUpTask(runnable, 0, null));
    }

    /**
     * 执行开机任务
     */
    public synchronized static void start() {
        Assert.isFalse(isStarted, "开机任务已执行,不可重复执行");
        isStarted = true;
        STARTUP_TASK_LIST.sort(Comparator.comparingDouble(o -> o.order));
        int idx = 1;
        for (StartUpTask task : STARTUP_TASK_LIST) {
            log.info(
                    "# 执行开机任务 {}{}",
                    String.format("%-2s", idx++),
                    StringUtils.isNoneBlank(task.name) ? String.format(" | %s", task.name) : ""
            );
            try {
                task.runnable.run();
            } catch (Exception e) {
                log.error("# 执行开机任务失败 {} | {}", idx++, e.getMessage(), e);
            }
        }
    }

    @Data
    private static class StartUpTask {
        private final Runnable runnable;
        private final double order;
        private final String name;
    }
}
