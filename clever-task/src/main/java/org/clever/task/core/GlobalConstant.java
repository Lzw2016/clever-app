package org.clever.task.core;

/**
 * 参数调优: <br />
 * 1.调度线程池大小 <br />
 * 2.定时任务执行线程池大小 <br />
 * 3.数据库连接池大小 <br />
 * 4.当前 {@link GlobalConstant} 的常量参数 <br />
 * 5.瓶颈在数据库上 <br />
 * 每秒能触发的任务数量，性能参考:
 * <pre>{@code
 * 25  - 很稳定
 * 50  - 稳定
 * 80  - 稳定
 * 100 - 比较稳定
 * 125 - 不稳定
 * 150 - 不稳定
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/13 10:40 <br/>
 */
public interface GlobalConstant {
    // 调度器节点注册的时间间隔(单位：毫秒)
    int REGISTER_SCHEDULER_INTERVAL = 60_000;
    // 数据完整性校验(一致性校验)的时间间隔(单位：毫秒)
    int DATA_CHECK_INTERVAL = 3600_000;
    // 维护当前集群可用的调度器列表的时间间隔(单位：毫秒)
    int RELOAD_SCHEDULER_INTERVAL = 5_000;
    // 每N秒读取接下来(N+M)秒内需要触发的触发器，N的值，建议：2_000 ~ 10_000(单位：毫秒)
    int NEXT_TRIGGER_N = 2_000;
    // 每N秒读取接下来(N+M)秒内需要触发的触发器，M的值，建议：2_000 ~ 5_000(单位：毫秒)
    int NEXT_TRIGGER_M = 2_000;
    // 在1秒内需要触发的触发器列表最大值
    int NEXT_TRIGGER_MAX_COUNT = 150;
    // 线程池线程保持时间(单位：毫秒)
    int THREAD_POOL_KEEP_ALIVE_SECONDS = 3_000;
    // 保存日志的时间间隔(单位：毫秒)，建议：100 ~ 1_000(单位：毫秒)
    int SAVE_LOG_INTERVAL = 300;
    // 集合初始容量
    int INITIAL_CAPACITY = 1024;
}
