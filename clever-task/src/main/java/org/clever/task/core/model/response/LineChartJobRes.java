package org.clever.task.core.model.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/14 10:27 <br/>
 */
@Data
public class LineChartJobRes {
    /**
     * 任务运行次数
     */
    private Integer runCount;
    /**
     * 任务运行失败数
     */
    private Integer runFailCount;
    /**
     * 任务运行成功比例
     */
    private BigDecimal successPercent;
}
