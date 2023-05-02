package org.clever.task.core.model;

import lombok.Getter;
import lombok.Setter;
import org.clever.task.core.model.entity.TaskJob;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 11:38 <br/>
 */
@Getter
@Setter
public abstract class AbstractJob implements Serializable {
    /**
     * 任务名称
     */
    protected String name;
    /**
     * 最大重入执行数量(对于单个节点当前任务未执行完成就触发了下一次执行导致任务重入执行)，小于等于0：表示禁止重入执行
     */
    protected Integer maxReentry;
    /**
     * 是否允许多节点并发执行，使用悲观锁实现(不建议使用)，0：禁止，1：允许
     */
    protected Integer allowConcurrent;
    /**
     * 执行失败时的最大重试次数
     */
    protected Integer maxRetryCount;
    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    protected Integer routeStrategy;
    /**
     * 路由策略，1-指定节点优先，调度器名称集合
     */
    protected String firstScheduler;
    /**
     * 路由策略，2-固定节点白名单，调度器名称集合
     */
    protected String whitelistScheduler;
    /**
     * 路由策略，3-固定节点黑名单，调度器名称集合
     */
    protected String blacklistScheduler;
    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    protected Integer loadBalance;
    /**
     * 是否更新任务数据，0：不更新，1：更新
     */
    protected Integer isUpdateData;
    /**
     * 任务数据(json格式)
     */
    protected String jobData;
    /**
     * 是否禁用：0-启用，1-禁用
     */
    protected Integer disable;
    /**
     * 描述
     */
    protected String description;

    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    public abstract Integer getType();

    public TaskJob toJob() {
        TaskJob job = new TaskJob();
        job.setName(getName());
        job.setMaxReentry(getMaxReentry());
        job.setAllowConcurrent(getAllowConcurrent());
        job.setMaxRetryCount(getMaxRetryCount());
        job.setRouteStrategy(getRouteStrategy());
        job.setFirstScheduler(getFirstScheduler());
        job.setWhitelistScheduler(getWhitelistScheduler());
        job.setBlacklistScheduler(getBlacklistScheduler());
        job.setLoadBalance(getLoadBalance());
        job.setIsUpdateData(getIsUpdateData());
        job.setJobData(getJobData());
        job.setDisable(getDisable());
        job.setDescription(getDescription());
        job.setType(getType());
        return job;
    }
}
