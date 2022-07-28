package org.clever.core.number;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数字精度工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/11/30 19:46 <br/>
 */
public class NumberScaleUtils {
    /**
     * 数字四舍五入转换
     */
    public static double round(double value, int scale) {
        return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 截断指定精度之后的数字
     */
    public static double down(double value, int scale) {
        return new BigDecimal(value).setScale(scale, RoundingMode.DOWN).doubleValue();
    }
}
