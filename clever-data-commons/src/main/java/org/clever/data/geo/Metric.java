package org.clever.data.geo;

import java.io.Serializable;

/**
 * {@link Metric} 的接口，可应用于基本比例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 13:18 <br/>
 */
public interface Metric extends Serializable {
    /**
     * 返回乘数以根据基本比例计算指标值
     */
    double getMultiplier();

    /**
     * 返回 {@link Metric} 所在单位的科学缩写
     */
    String getAbbreviation();
}
