package org.clever.data.geo;

import org.clever.data.domain.Range;
import org.clever.data.domain.Range.Bound;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.io.Serializable;

/**
 * 值对象以表示给定度量中的距离
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:13 <br/>
 */
public final class Distance implements Serializable, Comparable<Distance> {
    private static final long serialVersionUID = 2460886201934027744L;
    /**
     * 当前 {@link Metric} 中的距离值
     */
    private final double value;
    /**
     * {@link Distance} 的 {@link Metric}
     */
    private final Metric metric;

    /**
     * 使用中性指标创建新的 {@link Distance}。这意味着提供的值需要采用规范化形式
     */
    public Distance(double value) {
        this(value, Metrics.NEUTRAL);
    }

    /**
     * 使用给定的 {@link Metric} 创建一个新的 {@link Distance}。
     *
     * @param value  不得为 {@literal null}
     * @param metric 不得为 {@literal null}
     */
    public Distance(double value, Metric metric) {
        Assert.notNull(metric, "Metric must not be null");
        this.value = value;
        this.metric = metric;
    }

    /**
     * 在给定的 {@link Distance} 之间创建一个 {@link Range}
     *
     * @param min 可以是 {@literal null}
     * @param max 可以是 {@literal null}
     * @return 永远不会是 {@literal null}
     */
    public static Range<Distance> between(Distance min, Distance max) {
        return Range.from(Bound.inclusive(min)).to(Bound.inclusive(max));
    }

    /**
     * 通过根据给定值创建最小和最大 {@link Distance} 来创建新的 {@link Range}
     *
     * @param minValue  不得为 {@literal null}
     * @param minMetric 可以是 {@literal null}
     * @param maxValue  不得为 {@literal null}
     * @param maxMetric 可以是 {@literal null}
     */
    public static Range<Distance> between(double minValue, Metric minMetric, double maxValue, Metric maxMetric) {
        return between(new Distance(minValue, minMetric), new Distance(maxValue, maxMetric));
    }

    /**
     * 返回有关基础 {@link Metric} 的规范化值
     */
    public double getNormalizedValue() {
        return value / metric.getMultiplier();
    }

    /**
     * 返回距离所在单位的 {@link String} 表示
     *
     * @return the unit
     * @see Metric#getAbbreviation()
     */
    public String getUnit() {
        return metric.getAbbreviation();
    }

    /**
     * 将给定距离添加到当前距离。生成的 {@link Distance} 将与当前的度量标准相同
     *
     * @param other 不得为 {@literal null}
     */
    public Distance add(Distance other) {
        Assert.notNull(other, "Distance to add must not be null");
        double newNormalizedValue = getNormalizedValue() + other.getNormalizedValue();
        return new Distance(newNormalizedValue * metric.getMultiplier(), metric);
    }

    /**
     * 将给定的 {@link Distance} 添加到当前距离并强制结果在给定的 {@link Metric} 中
     *
     * @param other  不得为 {@literal null}
     * @param metric 不得为 {@literal null}
     */
    public Distance add(Distance other, Metric metric) {
        Assert.notNull(other, "Distance to must not be null");
        Assert.notNull(metric, "Result metric must not be null");
        double newLeft = getNormalizedValue() * metric.getMultiplier();
        double newRight = other.getNormalizedValue() * metric.getMultiplier();
        return new Distance(newLeft + newRight, metric);
    }

    /**
     * 在给定的 {@link Metric} 中返回一个新的 {@link Distance}。这意味着返回的实例将具有与原始实例相同的规范化值
     *
     * @param metric 不得为 {@literal null}
     */
    public Distance in(Metric metric) {
        Assert.notNull(metric, "Metric must not be null");
        return this.metric.equals(metric) ? this : new Distance(getNormalizedValue() * metric.getMultiplier(), metric);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Distance that) {
        if (that == null) {
            return 1;
        }
        double difference = this.getNormalizedValue() - that.getNormalizedValue();
        return difference == 0 ? 0 : difference > 0 ? 1 : -1;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(value);
        if (metric != Metrics.NEUTRAL) {
            builder.append(" ").append(metric.toString());
        }
        return builder.toString();
    }

    public double getValue() {
        return this.value;
    }

    public Metric getMetric() {
        return this.metric;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Distance)) {
            return false;
        }
        Distance distance = (Distance) o;
        if (value != distance.value) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(metric, distance.metric);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(value);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + ObjectUtils.nullSafeHashCode(metric);
        return result;
    }
}
