package org.clever.task.core.model.request;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/14 10:15 <br/>
 */
@Data
public class RunJobsReq {
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 数据量限制
     */
    private Integer limit;
}
