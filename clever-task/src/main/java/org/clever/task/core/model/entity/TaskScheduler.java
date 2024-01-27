package org.clever.task.core.model.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.mapper.JacksonMapper;

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
    /** 最后心跳时间 */
    private Date lastHeartbeatTime;
    /** 心跳频率(单位：毫秒) */
    private Long heartbeatInterval;
    /** 调度器配置，线程池大小、负载权重、最大并发任务数... */
    private String config;
    /** 调度器运行时信息 */
    private String runtimeInfo;
    /** 描述 */
    private String description;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;

    /**
     * 获取配置信息
     */
    public Config getConfigInfo() {
        if (StringUtils.isBlank(config)) {
            return null;
        }
        return JacksonMapper.getInstance().fromJson(config, Config.class);
    }

    @Data
    public static final class Config implements Serializable {
        /** 调度线程池大小 */
        private Integer schedulerExecutorPoolSize;
        /** 定时任务执行线程池大小 */
        private Integer jobExecutorPoolSize;
        /** 定时任务执行线程池队列大小 */
        private Integer jobExecutorQueueSize;
        /** 负载权重 */
        private Double loadWeight;
    }
}
