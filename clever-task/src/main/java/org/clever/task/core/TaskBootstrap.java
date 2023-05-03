package org.clever.task.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.AppShutdownHook;
import org.clever.core.BannerUtils;
import org.clever.core.OrderIncrement;
import org.clever.core.env.Environment;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.job.HttpJobExecutor;
import org.clever.task.core.job.JavaJobExecutor;
import org.clever.task.core.job.JobExecutor;
import org.clever.task.core.listeners.*;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/02 09:46 <br/>
 */
@Slf4j
public class TaskBootstrap {
    public static TaskBootstrap create(SchedulerConfig schedulerConfig) {
        return new TaskBootstrap(schedulerConfig);
    }

    public static TaskBootstrap create(Environment environment) {
        SchedulerConfig schedulerConfig = Binder.get(environment).bind(SchedulerConfig.PREFIX, SchedulerConfig.class).orElseGet(SchedulerConfig::new);
        // 打印配置日志
        List<String> logs = new ArrayList<>();
        logs.add("timed-task: ");
        logs.add("  jdbcName                  : " + schedulerConfig.getJdbcName());
        logs.add("  namespace                 : " + schedulerConfig.getNamespace());
        logs.add("  instanceName              : " + schedulerConfig.getInstanceName());
        logs.add("  heartbeatInterval         : " + schedulerConfig.getHeartbeatInterval() + "ms");
        logs.add("  description               : " + schedulerConfig.getDescription());
        logs.add("  schedulerExecutorPoolSize : " + schedulerConfig.getSchedulerExecutorPoolSize());
        logs.add("  schedulerExecutorQueueSize: " + schedulerConfig.getSchedulerExecutorQueueSize());
        logs.add("  jobExecutorPoolSize       : " + schedulerConfig.getJobExecutorPoolSize());
        logs.add("  jobExecutorQueueSize      : " + schedulerConfig.getJobExecutorQueueSize());
        logs.add("  loadWeight                : " + schedulerConfig.getLoadWeight());
        BannerUtils.printConfig(log, "定时任务配置", logs.toArray(new String[0]));
        TaskBootstrap taskBootstrap = create(schedulerConfig);
        AppContextHolder.registerBean("taskBootstrap", taskBootstrap, true);
        AppContextHolder.registerBean("taskInstance", taskBootstrap.taskInstance, true);
        return taskBootstrap;
    }

    public static final List<JobExecutor> JOB_EXECUTORS = new ArrayList<>();
    public static final List<SchedulerListener> SCHEDULER_LISTENERS = new ArrayList<>();
    public static final List<JobTriggerListener> JOB_TRIGGER_LISTENERS = new ArrayList<>();
    public static final List<JobListener> JOB_LISTENERS = new ArrayList<>();

    static {
        JOB_EXECUTORS.add(new HttpJobExecutor());
        JOB_EXECUTORS.add(new JavaJobExecutor());
        SCHEDULER_LISTENERS.add(new SchedulerLogListener());
        JOB_TRIGGER_LISTENERS.add(new JobTriggerLogListener());
        JOB_LISTENERS.add(new JobLogListener());
    }

    @Getter
    private final SchedulerConfig schedulerConfig;
    @Getter
    private final TaskInstance taskInstance;
    private volatile boolean isStarted = false;

    public TaskBootstrap(SchedulerConfig schedulerConfig) {
        Assert.notNull(schedulerConfig, "参数 schedulerConfig 不能为空");
        TaskDataSource.JDBC_DATA_SOURCE_NAME = schedulerConfig.getJdbcName();
        this.schedulerConfig = schedulerConfig;
        this.taskInstance = new TaskInstance(
                TaskDataSource.getQueryDSL(),
                schedulerConfig,
                JOB_EXECUTORS,
                SCHEDULER_LISTENERS,
                JOB_TRIGGER_LISTENERS,
                JOB_LISTENERS
        );
    }

    public synchronized void start() {
        if (!schedulerConfig.isEnable()) {
            return;
        }
        if (isStarted) {
            return;
        }
        isStarted = true;
        taskInstance.start();
        AppShutdownHook.addShutdownHook(taskInstance::stop, OrderIncrement.NORMAL, "停止定时任务");
    }
}
