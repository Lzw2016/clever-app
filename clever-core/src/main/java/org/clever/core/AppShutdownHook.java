package org.clever.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * 为优雅停机提供支持，管理停机时需要执行的任务
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 16:27 <br/>
 */
@Slf4j
public class AppShutdownHook {
    private static final List<ShutdownTask> SHUTDOWN_TASK_LIST = new LinkedList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SHUTDOWN_TASK_LIST.sort(Comparator.comparingDouble(o -> o.order));
            int idx = 1;
            for (ShutdownTask task : SHUTDOWN_TASK_LIST) {
                log.info(
                        "# 执行停机任务 {}{}",
                        String.format("%-2s", idx++),
                        StringUtils.isNoneBlank(task.name) ? String.format(" | %s", task.name) : ""
                );
                try {
                    task.runnable.run();
                } catch (Exception e) {
                    log.error("# 执行停机任务失败 {} | {}", idx++, e.getMessage(), e);
                }
            }
        }));
    }

    /**
     * 注册停机任务
     *
     * @param runnable 停机任务
     * @param order    执行顺序，值越小，优先级越高
     * @param name     任务名称
     */
    public synchronized static void addShutdownHook(Runnable runnable, double order, String name) {
        SHUTDOWN_TASK_LIST.add(new ShutdownTask(runnable, order, name));
    }

    /**
     * 注册停机任务
     *
     * @param runnable 停机任务
     * @param order    执行顺序，值越小，优先级越高
     */
    public synchronized static void addShutdownHook(Runnable runnable, double order) {
        SHUTDOWN_TASK_LIST.add(new ShutdownTask(runnable, order, null));
    }

    /**
     * 注册停机任务
     *
     * @param runnable 停机任务
     */
    public synchronized static void addShutdownHook(Runnable runnable) {
        SHUTDOWN_TASK_LIST.add(new ShutdownTask(runnable, 0, null));
    }

    @Data
    private static class ShutdownTask {
        private final Runnable runnable;
        private final double order;
        private final String name;
    }
}
