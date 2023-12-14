package org.clever.task.core.model.request;

import lombok.Data;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/14 10:15 <br/>
 */
@Data
public class RunJobsReq {
    /**
     * 开始时间
     */
    private Date start;
    /**
     * 结束时间
     */
    private Date end;
    /**
     * 数据量限制
     */
    private Integer limit;
}
