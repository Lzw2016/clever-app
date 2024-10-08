package org.clever.task.manage.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/28 22:33 <br/>
 */
@Data
public class ExecJobReq {
    /**
     * 任务ID
     */
    @NotNull
    private Long jobId;
    /**
     * 调度器实例名称
     */
    private String instanceName;
}
