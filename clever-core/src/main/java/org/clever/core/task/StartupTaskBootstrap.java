package org.clever.core.task;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.core.*;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/19 10:09 <br/>
 */
@Slf4j
public class StartupTaskBootstrap {
    public static StartupTaskBootstrap create(String rootPath, StartupTaskConfig config) {
        return new StartupTaskBootstrap(rootPath, config);
    }

    public static StartupTaskBootstrap create(String rootPath, Environment environment) {
        StartupTaskConfig config = Binder.get(environment).bind(StartupTaskConfig.PREFIX, StartupTaskConfig.class).orElseGet(StartupTaskConfig::new);
        AppContextHolder.registerBean("startupTaskConfig", config, true);
        List<StartupTaskConfig.TimedTaskConfig> timedTask = config.getTimedTask();
        List<StartupTaskConfig.CmdTaskConfig> cmdTask = config.getCmdTask();
        List<String> logs = new ArrayList<>();
        logs.add("poolSize    : " + config.getPoolSize());
        if (timedTask != null && !timedTask.isEmpty()) {
            logs.add("timedTask: ");
            for (StartupTaskConfig.TimedTaskConfig task : timedTask) {
                logs.add("  - name    : " + task.getName());
                logs.add("    enable  : " + task.isEnable());
                logs.add("    interval: " + StrFormatter.toPlainString(task.getInterval()));
                logs.add("    clazz   : " + task.getClazz());
                logs.add("    method  : " + task.getMethod());
            }
        }
        if (cmdTask != null && !cmdTask.isEmpty()) {
            logs.add("cmdTask: ");
            for (StartupTaskConfig.CmdTaskConfig task : cmdTask) {
                logs.add("  - name    : " + task.getName());
                logs.add("    enable  : " + task.isEnable());
                logs.add("    async   : " + task.isAsync());
                logs.add("    interval: " + StrFormatter.toPlainString(task.getInterval()));
                logs.add("    workDir : " + ResourcePathUtils.getAbsolutePath(rootPath, task.getWorkDir()));
                logs.add("    cmd     : " + CmdTask.getCmd(task.getCmd()));
            }
        }
        BannerUtils.printConfig(log, "startup-task配置", logs.toArray(new String[0]));
        return create(rootPath, config);
    }

    private final String rootPath;
    private final StartupTaskConfig config;
    private ClassLoader classLoader = this.getClass().getClassLoader();
    private final List<StartupTask> startupTasks = new LinkedList<>();
    private volatile boolean isStarted = false;
    private volatile ScheduledExecutorService scheduled;

    public StartupTaskBootstrap(String rootPath, StartupTaskConfig config) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(config, "参数 config 不能为null");
        this.rootPath = rootPath;
        this.config = config;
    }

    public void setClassLoader(ClassLoader classLoader) {
        Assert.notNull(classLoader, "参数 classLoader 不能为null");
        Assert.isFalse(isStarted, "任务已执行,设置 classLoader 已无效");
        this.classLoader = classLoader;
    }

    private ScheduledExecutorService getScheduled() {
        if (scheduled == null) {
            scheduled = new ScheduledThreadPoolExecutor(
                Math.max(1, config.getPoolSize()),
                new BasicThreadFactory.Builder()
                    .namingPattern("timed-task-%s")
                    .daemon(true)
                    .build()
            );
            AppShutdownHook.addShutdownHook(scheduled::shutdownNow, OrderIncrement.NORMAL, "停止开机任务调度器");
        }
        return scheduled;
    }

    private void init() {
        List<StartupTaskConfig.TimedTaskConfig> timedTask = config.getTimedTask();
        List<StartupTaskConfig.CmdTaskConfig> cmdTask = config.getCmdTask();
        double order = -10000;
        if (timedTask != null && !timedTask.isEmpty()) {
            for (StartupTaskConfig.TimedTaskConfig task : timedTask) {
                if (!task.isEnable()) {
                    continue;
                }
                try {
                    addTimedTask(task, order++);
                } catch (Exception e) {
                    log.error("定时任务: [{}],初始化失败", task.getName(), e);
                }
            }
        }
        if (cmdTask != null && !cmdTask.isEmpty()) {
            for (StartupTaskConfig.CmdTaskConfig task : cmdTask) {
                if (!task.isEnable()) {
                    continue;
                }
                try {
                    addCmdTask(task, order++);
                } catch (Exception e) {
                    log.error("命令行任务: [{}],初始化失败", task.getName(), e);
                }
            }
        }
    }

    /**
     * 执行任务
     */
    public synchronized void start() {
        Assert.isFalse(isStarted, "任务已执行,不可重复执行");
        isStarted = true;
        init();
        startupTasks.sort(Comparator.comparingDouble(o -> o.order));
        int idx = 1;
        for (StartupTask task : startupTasks) {
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

    /**
     * 注册任务
     *
     * @param runnable 任务
     * @param order    执行顺序，值越小，优先级越高
     * @param name     任务名称
     */
    public synchronized void addStartupTask(Runnable runnable, double order, String name) {
        startupTasks.add(new StartupTask(runnable, order, name));
    }

    /**
     * 注册任务
     *
     * @param runnable 任务
     * @param order    执行顺序，值越小，优先级越高
     */
    public synchronized void addStartupTask(Runnable runnable, double order) {
        startupTasks.add(new StartupTask(runnable, order, null));
    }

    /**
     * 注册任务
     *
     * @param runnable 任务
     */
    public synchronized void addStartupTask(Runnable runnable) {
        startupTasks.add(new StartupTask(runnable, 0, null));
    }

    /**
     * 注册定时任务
     *
     * @param config 定时任务配置
     * @param order  执行顺序，值越小，优先级越高
     */
    public synchronized void addTimedTask(StartupTaskConfig.TimedTaskConfig config, double order) {
        TimedTask timedTask = new TimedTask(
            config.getName(),
            config.getInterval(),
            config.getClazz(),
            config.getMethod(),
            classLoader
        );
        addStartupTask(() -> timedTask.start(getScheduled()), order, config.getName());
    }

    /**
     * 注册定时任务
     *
     * @param config 定时任务配置
     */
    public synchronized void addTimedTask(StartupTaskConfig.TimedTaskConfig config) {
        addTimedTask(config, 0);
    }

    /**
     * 注册命令行任务
     *
     * @param config 命令行任务配置
     * @param order  执行顺序，值越小，优先级越高
     */
    public synchronized void addCmdTask(StartupTaskConfig.CmdTaskConfig config, double order) {
        CmdTask cmdTask = new CmdTask(
            config.getName(),
            config.isAsync(),
            config.getInterval(),
            new File(ResourcePathUtils.getAbsolutePath(rootPath, config.getWorkDir())),
            config.getCmd()
        );
        addStartupTask(cmdTask::start, order, config.getName());
    }

    /**
     * 注册命令行任务
     *
     * @param config 命令行任务配置
     */
    public synchronized void addCmdTask(StartupTaskConfig.CmdTaskConfig config) {
        addCmdTask(config, 0);
    }

    @Data
    private static class StartupTask {
        private final Runnable runnable;
        private final double order;
        private final String name;
    }
}
