package org.clever.data.redis.domain.geo;

import org.clever.data.geo.Distance;
import org.clever.data.geo.Metric;
import org.clever.data.geo.Shape;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;


/**
 * 表示由宽度和高度定义的地理空间边界框
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 14:35 <br/>
 */
public class BoundingBox implements Shape {
    private final Distance width;
    private final Distance height;

    /**
     * 根据给定的宽度和高度创建一个新的 {@link BoundingBox}。两个距离必须使用相同的 {@link Metric}
     *
     * @param width  不得为 {@literal null}
     * @param height 不得为 {@literal null}
     */
    public BoundingBox(Distance width, Distance height) {
        Assert.notNull(width, "Width must not be null!");
        Assert.notNull(height, "Height must not be null!");
        Assert.isTrue(width.getMetric().equals(height.getMetric()), "Metric for width and height must be the same!");
        this.width = width;
        this.height = height;
    }

    /**
     * 根据给定的宽度、高度和 {@link Metric} 创建一个新的 {@link BoundingBox}
     *
     * @param width  不得为 {@literal null}
     * @param height 不得为 {@literal null}
     * @param metric 不得为 {@literal null}
     */
    public BoundingBox(double width, double height, Metric metric) {
        this(new Distance(width, metric), new Distance(height, metric));
    }

    /**
     * 返回此边界框的宽度
     *
     * @return 永远不会是{@literal null}
     */
    public Distance getWidth() {
        return width;
    }

    /**
     * 返回此边界框的高度
     *
     * @return 永远不会是{@literal null}
     */
    public Distance getHeight() {
        return height;
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(width);
        result = 31 * result + ObjectUtils.nullSafeHashCode(height);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BoundingBox)) {
            return false;
        }
        BoundingBox that = (BoundingBox) o;
        if (!ObjectUtils.nullSafeEquals(width, that.width)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(height, that.height);
    }

    @Override
    public String toString() {
        return String.format("Bounding box: [width=%s, height=%s]", width, height);
    }
}
