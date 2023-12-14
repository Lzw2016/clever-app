package org.clever.task.core.model.response;

import lombok.Data;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/13 22:25 <br/>
 */
@Data
public class StatisticsInfoRes {
    /**
     * 任务数量
     */
    private Integer jobCount;
    /**
     * 触发次数
     */
    private Integer triggerCount;
    /**
     * 命名空间数
     */
    private Integer namespaceCount;
    /**
     * 服务节点数(总数)
     */
    private Integer instanceCount;
    /**
     * 服务节点数(活动的)
     */
    private Integer activeInstanceCount;
    /**
     * 任务类型比例
     */
    private JobTypeRatio jobTypeRatio;
    /**
     * 命名空间任务数比例{@code Map<namespaceName, JobTypeRatio>}
     */
    private Map<String, JobTypeRatio> namespaceJobTypeRatioMap;

    /**
     * 任务类型比例(百分比)
     */
    @Data
    public static class JobTypeRatio {
        private Integer http;
        private Integer java;
        private Integer js;
        private Integer shell;
    }
}
