//package org.clever.core;
//
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.clever.util.Assert;
//
//import java.util.Comparator;
//import java.util.LinkedList;
//import java.util.List;
//
///**
// * 应用程序启动时的任务
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/18 22:54 <br/>
// */
//@Slf4j
//public class AppStartupTask {
//    private static final List<StartupTask> STARTUP_TASK_LIST = new LinkedList<>();
//    private static volatile boolean IS_STARTED = false;
//
//    /**
//     * 注册开机任务
//     *
//     * @param runnable 开机任务
//     * @param order    执行顺序，值越小，优先级越高
//     * @param name     任务名称
//     */
//    public synchronized static void addStartupTask(Runnable runnable, double order, String name) {
//        STARTUP_TASK_LIST.add(new StartupTask(runnable, order, name));
//    }
//
//    /**
//     * 注册开机任务
//     *
//     * @param runnable 开机任务
//     * @param order    执行顺序，值越小，优先级越高
//     */
//    public synchronized static void addStartupTask(Runnable runnable, double order) {
//        STARTUP_TASK_LIST.add(new StartupTask(runnable, order, null));
//    }
//
//    /**
//     * 注册开机任务
//     *
//     * @param runnable 开机任务
//     */
//    public synchronized static void addStartupTask(Runnable runnable) {
//        STARTUP_TASK_LIST.add(new StartupTask(runnable, 0, null));
//    }
//
//    /**
//     * 执行开机任务
//     */
//    public synchronized static void start() {
//        Assert.isFalse(IS_STARTED, "开机任务已执行,不可重复执行");
//        IS_STARTED = true;
//        STARTUP_TASK_LIST.sort(Comparator.comparingDouble(o -> o.order));
//        int idx = 1;
//        for (StartupTask task : STARTUP_TASK_LIST) {
//            log.info(
//                    "# 执行开机任务 {}{}",
//                    String.format("%-2s", idx++),
//                    StringUtils.isNoneBlank(task.name) ? String.format(" | %s", task.name) : ""
//            );
//            try {
//                task.runnable.run();
//            } catch (Exception e) {
//                log.error("# 执行开机任务失败 {} | {}", idx++, e.getMessage(), e);
//            }
//        }
//    }
//
//    @Data
//    private static class StartupTask {
//        private final Runnable runnable;
//        private final double order;
//        private final String name;
//    }
//}
