package org.clever.task.manage.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.model.request.QueryByPage;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/04 23:01 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskJobTriggerReq extends QueryByPage {
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 触发器名称
     */
    private String name;
    /**
     * 触发类型，1：cron触发，2：固定间隔触发
     */
    private Integer type;
    /**
     * 创建时间-开始
     */
    private Date createStart;
    /**
     * 创建时间-结束
     */
    private Date createEnd;
}
