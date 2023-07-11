package org.clever.task.core.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.model.request.QueryByPage;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/07/11 17:46 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskJobReq extends QueryByPage {
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 任务名称
     */
    private String name;
    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
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
