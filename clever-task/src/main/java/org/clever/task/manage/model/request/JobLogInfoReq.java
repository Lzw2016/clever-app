package org.clever.task.manage.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.model.request.QueryByPage;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/22 20:49 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JobLogInfoReq extends QueryByPage {
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 调度器实例名称
     */
    private String instanceName;
    /**
     * 任务ID
     */
    private Long jobId;
    /**
     * 任务名称
     */
    private String jobName;
    /**
     * 任务类型
     */
    private Integer jobType;
    /**
     * 任务执行结果
     */
    private Integer jobStatus;
    /**
     * 是否是手动触发
     */
    private Integer isManual;
    // /**
    //  * 是否错过了触发
    //  */
    // private Integer misFired;
    /**
     * 触发时间 - 开始
     */
    private Date fireTimeStart;
    /**
     * 触发时间 - 结束
     */
    private Date fireTimeEnd;
}
