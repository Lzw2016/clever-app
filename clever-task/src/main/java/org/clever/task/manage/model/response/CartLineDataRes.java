package org.clever.task.manage.model.response;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 16:23 <br/>
 */
@Data
public class CartLineDataRes {
    /**
     * 报表时间
     */
    private String reportTime;
    /**
     * job 运行总次数
     */
    private Long jobCount = 0L;
    /**
     * job 运行错误次数
     */
    private Long jobErrCount = 0L;
    /**
     * 触发总次数
     */
    private Long triggerCount = 0L;
    /**
     * 错过触发次数
     */
    private Long misfireCount = 0L;
}
