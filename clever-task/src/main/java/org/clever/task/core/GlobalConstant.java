package org.clever.task.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/13 10:40 <br/>
 */
public interface GlobalConstant {
    // 守护线程名
    String DATA_CHECK_DAEMON_NAME = "定时任务数据校验";
    String REGISTER_SCHEDULER_DAEMON_NAME = "调度器节点注册";
    String CALC_NEXT_FIRE_TIME_DAEMON_NAME = "校准触发器触发时间";
    String HEARTBEAT_DAEMON_NAME = "心跳保持";
    String RELOAD_SCHEDULER_DAEMON_NAME = "加载调度器";
    String RELOAD_NEXT_TRIGGER_DAEMON_NAME = "加载将要触发的触发器";
    String TRIGGER_JOB_EXEC_DAEMON_NAME = "调度器轮询任务";
    // 工作线程名
    String SCHEDULER_EXECUTOR_NAME = "调度线程池";
    String JOB_EXECUTOR_NAME = "定时任务执行线程池";

    // 数据完整性校验(一致性校验)的时间间隔(单位：毫秒)
    int DATA_CHECK_INTERVAL = 3600_000;
    // 调度器节点注册的时间间隔(单位：毫秒)
    int REGISTER_SCHEDULER_INTERVAL = 60_000;
    // 初始化触发器下一次触发时间(校准触发器触发时间)的时间间隔(单位：毫秒)
    int CALC_NEXT_FIRE_TIME_INTERVAL = 300_000;
    // 维护当前集群可用的调度器列表的时间间隔(单位：毫秒)
    int RELOAD_SCHEDULER_INTERVAL = 5_000;
    // 维护接下来N秒内需要触发的触发器列表的时间间隔，建议：500 ~ 1000(单位：毫秒)
    int RELOAD_NEXT_TRIGGER_INTERVAL = 1_000;
    // 接下来N秒内需要触发的触发器列表(N = RELOAD_NEXT_TRIGGER_INTERVAL * NEXT_TRIGGER_N)
    double NEXT_TRIGGER_N = 1.5;
    // 调度器轮询任务的时间间隔(单位：毫秒)
    int TRIGGER_JOB_EXEC_INTERVAL = 300;
    // 调度器轮询任务的最大时间间隔(单位：毫秒)
    int TRIGGER_JOB_EXEC_MAX_INTERVAL = 900;
    // 接下来N秒内需要触发的触发器列表最大值
    int NEXT_TRIGGER_MAX_COUNT = 1000;

    // 线程池初始延时(单位：毫秒)
    int THREAD_POOL_INITIAL_DELAY = 0;
    // 线程池线程保持时间(单位：毫秒)
    long THREAD_POOL_KEEP_ALIVE_SECONDS = 3_000L;
    // 集合初始容量
    int INITIAL_CAPACITY = 1024;
    // 线程池名称
    Map<String, String> THREAD_POOL_NAME = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put(DATA_CHECK_DAEMON_NAME, "data_check-pool-%d");
                put(REGISTER_SCHEDULER_DAEMON_NAME, "register_scheduler-pool-%d");
                put(CALC_NEXT_FIRE_TIME_DAEMON_NAME, "calc_next_fire_time-pool-%d");
                put(HEARTBEAT_DAEMON_NAME, "heartbeat-pool-%d");
                put(RELOAD_SCHEDULER_DAEMON_NAME, "reload_scheduler-pool-%d");
                put(RELOAD_NEXT_TRIGGER_DAEMON_NAME, "reload_next_trigger-pool-%d");
                put(TRIGGER_JOB_EXEC_DAEMON_NAME, "trigger_job_exec-pool-%d");
                put(SCHEDULER_EXECUTOR_NAME, "scheduler_executor-pool-%d");
                put(JOB_EXECUTOR_NAME, "job_executor-pool-%d");
            }}
    );
}
