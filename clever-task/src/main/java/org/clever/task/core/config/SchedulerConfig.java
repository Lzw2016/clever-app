package org.clever.task.core.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/02 17:54 <br/>
 */
@Data
public class SchedulerConfig implements Serializable {
    public static final String PREFIX = "timed-task";

    /**
     * 是否启用“定时任务”
     */
    private boolean enable = false;
    /**
     * jdbc数据源
     */
    private String jdbcName = "default";
    /**
     * 命名空间(同一个namespace的不同调度器属于同一个集群)
     */
    private String namespace = "default";
    /**
     * 调度器实例名称
     */
    private String instanceName = "node01";
    /**
     * 心跳频率，建议：800 ~ 15000(单位：毫秒)
     */
    private long heartbeatInterval = 10_000;
    /**
     * 调度器节点描述
     */
    private String description;
    /**
     * 调度线程池大小
     */
    private int schedulerExecutorPoolSize = 16;
    /**
     * 调度线程池队列大小(大于等于schedulerExecutorPoolSize值)
     */
    private int schedulerExecutorQueueSize = 16;
    /**
     * 定时任务执行线程池大小
     */
    private int jobExecutorPoolSize = 64;
    /**
     * 定时任务执行线程池队列大小(大于等于jobExecutorPoolSize值)
     */
    private int jobExecutorQueueSize = 64;
    /**
     * 负载权重
     */
    private double loadWeight = 1.0;
}