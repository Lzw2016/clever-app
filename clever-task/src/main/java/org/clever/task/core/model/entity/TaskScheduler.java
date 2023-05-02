package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度器(task_scheduler)
 */
@Data
public class TaskScheduler implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间(同一个namespace的不同调度器属于同一个集群) */
    private String namespace;
    /** 调度器实例名称 */
    private String instanceName;
    /** 最后心跳时间(毫秒时间戳) */
    private Long lastHeartbeatTime;
    /** 心跳频率(单位：毫秒) */
    private Long heartbeatInterval;
    /** 调度器配置，线程池大小、负载权重、最大并发任务数... */
    private String config;
    /** 描述 */
    private String description;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;

    @Data
    public static final class Config implements Serializable {
        /** 调度线程池大小 */
        private Integer schedulerExecutorPoolSize;
        /** 定时任务执行线程池大小 */
        private Integer jobExecutorPoolSize;
        /** 负载权重 */
        private Double loadWeight;
        /** 最大并发任务数(大于等于jobExecutorPoolSize值) */
        private Integer maxConcurrent;
    }
}
