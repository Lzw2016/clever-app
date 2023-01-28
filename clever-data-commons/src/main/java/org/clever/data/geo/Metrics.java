package org.clever.data.geo;

/**
 * 常用的 {@link Metric}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:14 <br/>
 */
public enum Metrics implements Metric {
    KILOMETERS(6378.137, "km"),
    MILES(3963.191, "mi"),
    NEUTRAL(1, "");

    private final double multiplier;
    private final String abbreviation;

    /**
     * 使用给定的乘数创建一个新的 {@link Metrics}
     *
     * @param multiplier   赤道处的地球半径不能为 {@literal null}
     * @param abbreviation 用于此 {@link Metric} 的缩写不得为 {@literal null}
     */
    Metrics(double multiplier, String abbreviation) {
        this.multiplier = multiplier;
        this.abbreviation = abbreviation;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public String getAbbreviation() {
        return abbreviation;
    }
}
