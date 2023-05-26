package org.clever.task.core;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/13 10:40 <br/>
 */
public interface GlobalConstant {
    // 调度器节点注册的时间间隔(单位：毫秒)
    int REGISTER_SCHEDULER_INTERVAL = 60_000;
    // 数据完整性校验(一致性校验)的时间间隔(单位：毫秒)
    int DATA_CHECK_INTERVAL = 3600_000;
    // 初始化触发器下一次触发时间(校准触发器触发时间)的时间间隔(单位：毫秒)
    int CALC_NEXT_FIRE_TIME_INTERVAL = 300_000;
    // 维护当前集群可用的调度器列表的时间间隔(单位：毫秒)
    int RELOAD_SCHEDULER_INTERVAL = 5_000;
    // 每N秒读取接下来(N+M)秒内需要触发的触发器，N的值，建议：2_000 ~ 30_000(单位：毫秒)
    int NEXT_TRIGGER_N = 2_000;
    // 每N秒读取接下来(N+M)秒内需要触发的触发器，M的值，建议：2_000 ~ 8_000(单位：毫秒)
    int NEXT_TRIGGER_M = 2_000;
    // 接下来(N+M)秒内需要触发的触发器列表最大值
    int NEXT_TRIGGER_MAX_COUNT = 1000;
    // 线程池线程保持时间(单位：毫秒)
    long THREAD_POOL_KEEP_ALIVE_SECONDS = 3_000L;
    // 集合初始容量
    int INITIAL_CAPACITY = 1024;
}
