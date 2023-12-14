package org.clever.task.core.model.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/14 10:22 <br/>
 */
@Data
public class LineChartTriggerRes {
    /**
     * 时间
     */
    private String time;
    /**
     * 触发次数
     */
    private Integer triggerCount;
    /**
     * 错过触发次数
     */
    private Integer misfireCount;
    /**
     * 触发成功比例
     */
    private BigDecimal successPercent;
}
