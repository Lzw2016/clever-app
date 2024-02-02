package org.clever.task.core.model.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.mapper.JacksonMapper;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 定时任务(task_job)
 */
@Data
public class TaskJob implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 任务名称 */
    private String name;
    /** 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本 */
    private Integer type;
    /** 最大重入执行数量(对于单个节点当前任务未执行完成就触发了下一次执行导致任务重入执行)，小于等于0：表示禁止重入执行 */
    private Integer maxReentry;
    /** 是否允许多节点并发执行，使用分布式锁实现，不建议禁止，0：禁止，1：允许 */
    private Integer allowConcurrent;
    /** 执行失败时的最大重试次数 */
    private Integer maxRetryCount;
    /** 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单 */
    private Integer routeStrategy;
    /** 路由策略，1-指定节点优先，调度器名称集合 */
    private String firstScheduler;
    /** 路由策略，2-固定节点白名单，调度器名称集合 */
    private String whitelistScheduler;
    /** 路由策略，3-固定节点黑名单，调度器名称集合 */
    private String blacklistScheduler;
    /** 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH */
    private Integer loadBalance;
    /** 是否更新任务数据，0：不更新，1：更新 */
    private Integer isUpdateData;
    /** 任务数据(json格式) */
    private String jobData;
    /** 运行次数 */
    private Long runCount;
    /** 是否禁用：0-启用，1-禁用 */
    private Integer disable;
    /** 描述 */
    private String description;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;

    /**
     * 路由策略，1-指定节点优先，调度器名称集合
     */
    @SuppressWarnings("unchecked")
    public List<String> getFirstInstanceNames() {
        if (StringUtils.isBlank(firstScheduler)) {
            return Collections.emptyList();
        }
        return JacksonMapper.getInstance().fromJson(firstScheduler, List.class);
    }

    /**
     * 路由策略，2-固定节点白名单，调度器名称集合
     */
    @SuppressWarnings("unchecked")
    public List<String> getWhitelistInstanceNames() {
        if (StringUtils.isBlank(whitelistScheduler)) {
            return Collections.emptyList();
        }
        return JacksonMapper.getInstance().fromJson(whitelistScheduler, List.class);
    }

    /**
     * 路由策略，3-固定节点黑名单，调度器名称集合
     */
    @SuppressWarnings("unchecked")
    public List<String> getBlacklistInstanceNames() {
        if (StringUtils.isBlank(blacklistScheduler)) {
            return Collections.emptyList();
        }
        return JacksonMapper.getInstance().fromJson(blacklistScheduler, List.class);
    }


}
