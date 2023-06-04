package org.clever.task.core.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.model.request.QueryByPage;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/06/04 20:42 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SchedulerLogReq extends QueryByPage {
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 调度器实例名称
     */
    private String instanceName;
    /**
     * 事件名称
     */
    private String eventName;
    /**
     * 创建时间 - 开始
     */
    private Date createAtStart;
    /**
     * 创建时间 - 结束
     */
    private Date createAtEnd;
}
