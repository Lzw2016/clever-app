package org.clever.task.manage.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/09/14 15:32 <br/>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TaskInfoReq {
    /** 命名空间 */
    private String namespace;
    /** 任务ID */
    private Long jobId;

    private TaskJob job;
    private TaskHttpJob httpJob;
    private TaskJavaJob javaJob;
    private TaskJsJob jsJob;
    private TaskShellJob shellJob;
    private TaskJobTrigger jobTrigger;

    @Data
    public static class TaskJob {
        /** 主键id */
        private Long id;
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
        /** 是否禁用：0-启用，1-禁用 */
        private Integer disable;
        /** 描述 */
        private String description;
    }

    @Data
    public static class TaskHttpJob {
        /** 主键id */
        private Long id;
        /** http请求method，ALL GET HEAD POST PUT DELETE CONNECT OPTIONS TRACE PATCH */
        private String requestMethod;
        /** Http请求地址 */
        private String requestUrl;
        /** Http请求数据json格式，包含：params、headers、body */
        private String requestData;
    }

    @Data
    public static class TaskJavaJob {
        /** 主键id */
        private Long id;
        /** 是否是静态方法(函数)，0：非静态，1：静态 */
        private Integer isStatic;
        /** java class全路径 */
        private String className;
        /** java class method */
        private String classMethod;
    }

    @Data
    public static class TaskJsJob {
        /** 主键id */
        private Long id;
        /** 文件内容 */
        private String content;
        /** 读写权限：0-可读可写，1-只读 */
        private Integer readOnly;
    }

    @Data
    public static class TaskShellJob {
        /** 主键id */
        private Long id;
        /** shell脚本类型：bash|sh|ash|powershell|cmd|python|node|deno|php */
        private String shellType;
        /** 执行终端的字符集编码，如：“UTF-8” */
        private String shellCharset;
        /** 执行超时时间，单位：秒，默认：“10分钟” */
        private Integer shellTimeout;
        /** 文件内容 */
        private String content;
        /** 读写权限：0-可读可写，1-只读 */
        private Integer readOnly;
    }

    @Data
    public static class TaskJobTrigger {
        /** 主键id */
        private Long id;
        /** 触发开始时间 */
        private Date startTime;
        /** 触发结束时间 */
        private Date endTime;
        /** 错过触发策略，1：忽略，2：立即补偿触发一次 */
        private Integer misfireStrategy;
        /** 是否允许多节点并行触发，使用分布式锁实现，不建议允许，0：禁止，1：允许 */
        private Integer allowConcurrent;
        /** 触发类型，1：cron触发，2：固定间隔触发 */
        private Integer type;
        /** cron表达式 */
        private String cron;
        /** 固定间隔触发，间隔时间(单位：秒) */
        private Long fixedInterval;
        /** 是否禁用：0-启用，1-禁用 */
        private Integer disable;
        /** 描述 */
        private String description;
    }
}
