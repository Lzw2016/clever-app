package org.clever.task.core;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.listeners.JobLogListener;
import org.clever.task.core.listeners.JobTriggerLogListener;
import org.clever.task.core.listeners.SchedulerLogListener;
import org.clever.task.core.model.CronTrigger;
import org.clever.task.core.model.HttpJobModel;
import org.clever.task.core.model.JobInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/03 12:28 <br/>
 */
@Slf4j
public class TaskInstanceTest {
    public static int SLEEP_1 = 1000 * 60 * 2;
    public static int SLEEP_2 = 1000 * 30;

    public static SchedulerConfig newSchedulerConfig(String instanceName) {
        SchedulerConfig config = new SchedulerConfig();
        config.setSchedulerExecutorPoolSize(256);
        config.setJobExecutorPoolSize(512);
        config.setJobExecutorQueueSize(5120);
        config.setNamespace("lzw");
        config.setInstanceName(instanceName);
        config.setDescription("测试节点01");
        return config;
    }

    public static void startTaskInstance(String instanceName, Consumer<TaskInstance> callback) throws InterruptedException {
        Jdbc jdbc = BaseTest.newMysql();
        QueryDSL queryDSL = QueryDSL.create(jdbc);
        TaskInstance taskInstance = new TaskInstance(
            "./",
            queryDSL,
            newSchedulerConfig(instanceName),
            // Arrays.asList(new MockJobExecutor(), new HttpJobExecutor()),
            // Collections.singletonList(new MockJobExecutor()),
            Collections.singletonList(new RedisMockJobExecutor()),
            Collections.singletonList(new SchedulerLogListener()),
            Collections.singletonList(new JobTriggerLogListener()),
            Collections.singletonList(new JobLogListener())
        );
        callback.accept(taskInstance);
        taskInstance.start();
        Thread.sleep(SLEEP_1);
        taskInstance.paused();
        Thread.sleep(SLEEP_2);
        taskInstance.stop();
        queryDSL.getJdbc().close();
    }

    @Test
    public void t01() throws InterruptedException {
        SLEEP_1 = 1000 * 5;
        SLEEP_2 = 1000 * 3;
        startTaskInstance("n01", taskInstance -> {
            HttpJobModel job = new HttpJobModel("访问百度", "GET", "https://www.baidu.com");
            job.setAllowConcurrent(0);
            CronTrigger trigger = new CronTrigger("访问百度_触发器", "0/1 * * * * ?");
            trigger.setAllowConcurrent(0);
            JobInfo info = taskInstance.addJob(job, trigger);
            log.info("info -> {}", info);
        });
    }

    @Test
    public void t02() throws InterruptedException {
        SLEEP_1 = 3600_000;
        SLEEP_2 = 1000 * 3;
        startTaskInstance("n01", taskInstance -> {
        });
    }

    @Test
    public void t03() throws InterruptedException {
        SLEEP_1 = 3600_000;
        SLEEP_2 = 1000 * 3;
        startTaskInstance("n02", taskInstance -> {
        });
    }

    @Test
    public void t04() throws InterruptedException {
        SLEEP_1 = 1000 * 60 * 60 * 10;
        SLEEP_2 = 1000 * 3;
        startTaskInstance("n03", taskInstance -> {
        });
    }
}
