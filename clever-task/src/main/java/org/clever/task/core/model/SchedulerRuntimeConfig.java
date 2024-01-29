package org.clever.task.core.model;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/29 10:46 <br/>
 */
@Data
public class SchedulerRuntimeConfig {
    /**
     * 调度线程池大小
     */
    private Integer schedulerExecutorPoolSize;
    /**
     * 定时任务执行线程池大小
     */
    private Integer jobExecutorPoolSize;
    /**
     * 定时任务执行线程池队列大小
     */
    private Integer jobExecutorQueueSize;
    /**
     * 负载权重
     */
    private Double loadWeight;
}
