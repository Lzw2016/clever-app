package org.clever.core.thread;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/29 10:59 <br/>
 */
@Data
public class ThreadPoolState {
    /**
     * 当前活跃执行任务的线程数
     */
    private int activeCount;
    /**
     * 工作队列中等待执行的任务数
     */
    private int queueSize;
    /**
     * 当前线程池中的线程数（包括核心线程和非核心线程）
     */
    private int poolSize;
    /**
     * 是否已调用 shutdown() 方法，不再接受新任务，但会继续处理队列中的任务
     */
    private boolean shutdown;
    /**
     * 线程池是否已经完全终止，即所有提交的任务都已经执行完毕或被取消
     */
    private boolean terminated;
    /**
     * 已完成的任务总数
     */
    private long completedTaskCount;
    /**
     * 返回计划执行的任务的大致总数。由于任务和线程的状态在计算过程中可能会动态变化，因此返回的值只是一个近似值。
     */
    private long taskCount;
    /**
     * 核心线程数
     */
    private int corePoolSize;
    /**
     * 最大线程数
     */
    private int maximumPoolSize;
    /**
     * 获取拒绝策略等其他详细配置信息
     */
    private String abortPolicy;

    @Override
    public String toString() {
        return "ThreadPoolState{" +
            "activeCount=" + activeCount +
            ", queueSize=" + queueSize +
            ", poolSize=" + poolSize +
            ", shutdown=" + shutdown +
            ", terminated=" + terminated +
            ", completedTaskCount=" + completedTaskCount +
            ", taskCount=" + taskCount +
            ", corePoolSize=" + corePoolSize +
            ", maximumPoolSize=" + maximumPoolSize +
            ", abortPolicy='" + abortPolicy + '\'' +
            '}';
    }
}
