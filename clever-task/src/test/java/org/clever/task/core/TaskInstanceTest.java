package org.clever.task.core;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.job.HttpJobExecutor;
import org.clever.task.core.job.MockJobExecutor;
import org.clever.task.core.listeners.JobLogListener;
import org.clever.task.core.listeners.JobTriggerLogListener;
import org.clever.task.core.listeners.SchedulerLogListener;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/03 12:28 <br/>
 */
@Slf4j
public class TaskInstanceTest {
    public static SchedulerConfig newSchedulerConfig(String instanceName) {
        SchedulerConfig config = new SchedulerConfig();
        config.setSchedulerExecutorPoolSize(4);
        config.setJobExecutorPoolSize(8);
        config.setNamespace("lzw");
        config.setInstanceName(instanceName);
        config.setDescription("测试节点01");
        return config;
    }

    public static void startTaskInstance(String instanceName) throws InterruptedException {
        Jdbc jdbc = BaseTest.newMysql();
        QueryDSL queryDSL = QueryDSL.create(jdbc);
        TaskInstance taskInstance = new TaskInstance(
                queryDSL,
                newSchedulerConfig(instanceName),
                Arrays.asList(new MockJobExecutor(), new HttpJobExecutor()),
                Collections.singletonList(new SchedulerLogListener()),
                Collections.singletonList(new JobTriggerLogListener()),
                Collections.singletonList(new JobLogListener())
        );
        taskInstance.start();
        Thread.sleep(1000 * 60 * 2);
        taskInstance.pause();
        Thread.sleep(1000 * 30);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(8_000);
            } catch (InterruptedException ignored) {
            }
            queryDSL.getJdbc().close();
        }));
    }

    @Test
    public void t01() throws InterruptedException {
        startTaskInstance("n01");
    }
}
