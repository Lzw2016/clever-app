package org.clever.core;

import org.clever.core.number.NumberScaleUtils;

/**
 * order 自增工具(一共支持6个层级的自增，每个层级最多自增99次)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/28 09:20 <br/>
 */
public class OrderIncrement {
    private static final double LEVEL1 = 100;
    private static final double LEVEL2 = 1;
    private static final double LEVEL3 = 0.01;
    private static final double LEVEL4 = 0.0001;
    private static final double LEVEL5 = 0.000001;
    private static final double LEVEL6 = 0.00000001;

    private int level1Count = 0;
    private int level2Count = 0;
    private int level3Count = 0;
    private int level4Count = 0;
    private int level5Count = 0;
    private int level6Count = 0;

    /**
     * 获取当前排序值
     */
    public synchronized double order() {
        double order = LEVEL1 * level1Count
                + LEVEL2 * level2Count
                + LEVEL3 * level3Count
                + LEVEL4 * level4Count
                + LEVEL5 * level5Count
                + LEVEL6 * level6Count;
        return NumberScaleUtils.round(order, 8);
    }

    /**
     * 自增第一层(第一层自增值最大，第六层最小)
     */
    public synchronized double incrL1() {
        level1Count++;
        if (level1Count > 99) {
            level1Count = 99;
            throw new RuntimeException("order 1层级自增溢出");
        }
        return order();
    }

    /**
     * 自增第二层(第一层自增值最大，第六层最小)
     */
    public synchronized double incrL2() {
        level2Count++;
        if (level2Count > 99) {
            level2Count = 99;
            throw new RuntimeException("order 2层级自增溢出");
        }
        return order();
    }

    /**
     * 自增第三层(第一层自增值最大，第六层最小)
     */
    public synchronized double incrL3() {
        level3Count++;
        if (level3Count > 99) {
            level3Count = 99;
            throw new RuntimeException("order 3层级自增溢出");
        }
        return order();
    }

    /**
     * 自增第四层(第一层自增值最大，第六层最小)
     */
    public synchronized double incrL4() {
        level4Count++;
        if (level4Count > 99) {
            level4Count = 99;
            throw new RuntimeException("order 4层级自增溢出");
        }
        return order();
    }

    /**
     * 自增第五层(第一层自增值最大，第六层最小)
     */
    public synchronized double incrL5() {
        level5Count++;
        if (level5Count > 99) {
            level5Count = 99;
            throw new RuntimeException("order 5层级自增溢出");
        }
        return order();
    }

    /**
     * 自增第六层(第一层自增值最大，第六层最小)
     */
    public synchronized double incrL6() {
        level6Count++;
        if (level6Count > 99) {
            level6Count = 99;
            throw new RuntimeException("order 6层级自增溢出");
        }
        return order();
    }
}
